package com.dehigher.biutils.activecontract;

import com.dehigher.biutils.activecontract.dto.ActiveContractItem;
import com.dehigher.biutils.activecontract.dto.CovalentResult;
import com.dehigher.biutils.base.*;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;


public class ContractQueryExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ContractQueryExecutor.class);

    private static final int MAX_QUEUE_SIZE = 20000;



    private static final HttpClient httpClient = HttpUtils.createHttpClient(2, 2, 10000, 10000, 10000);

    private static final String[] covalentKeys = new String[] {
            "cqt_rQBWy8yG3b7J9P4v7hcJcHCTwMK4",
            "cqt_rQwMhh4bYVfPXJJypqX8QvRMyfGK",
            "cqt_rQCv66KmQD3wccfvyckP8YgCjyj7",
            "cqt_rQkHg7RQp8yPcmcthDdq6jpCvxyh",
            "cqt_rQv8PYgYF4QJh94ctckxKCW9M7BB",
            "cqt_rQpHxqkxG38bkCGhQ4hPRBQmhpPp",
            "cqt_rQ7fMMhb4QMK8cXJTrDVmwQpQdTK",
            "cqt_rQywf34fmWCTW3TmqpXvYtpfmcGk",
            "cqt_rQWCVKYGRhXdmGMKytKrrR3bDrFj"
    };

    private static final Set<String> BLACK_ADDRESS_LIST = new HashSet<>();
    static {
        BLACK_ADDRESS_LIST.add("0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2");
    }


    private int currentCovalentKeyIndex = 0;

    private long nextPeriodStartTime = 0;

    private long lastBlockNumber = 0;

    private long period;

    private Thread thread;

    private ContractAnalysisExecutor analysisExecutor;


    public ContractQueryExecutor(long period) {
        this.period = period;
    }



    /**
     * 启动线程
     */
    public void start(){
        thread = new Thread(()-> run());
        thread.start();
        analysisExecutor = new ContractAnalysisExecutor();
    }

    private String run(){
        // 到了执行事件, 更新轮次数据, 5分钟执行一次
        while(true){
            long current = System.currentTimeMillis();
            if(nextPeriodStartTime == 0 || current >= nextPeriodStartTime){
                List<ActiveContractItem.Item> contracts = getContractsPerRound();
                Set<String> ret = filterContract(contracts);
                FileUtil.writeToFile(JacksonUtils.toJson(ret), "contracts/last_5m_" + lastBlockNumber + "_" + TimeUtils.format(current));
                // 添加队列
                analysisExecutor.addJob(ret);
                updateNextStartTime(current);
            }else{
                sleep(500);
            }
        }
    }

    /**
     * 基于时间过滤，并且做深拷贝
     */
    private Set<String> filterContract(List<ActiveContractItem.Item> contracts) {
        return contracts.stream().parallel()
                .filter(item -> !BLACK_ADDRESS_LIST.contains(item.getSender_address()))
                .map(item -> item.getSender_address())
                .collect(Collectors.toSet());
    }

    private void updateNextStartTime(long taskStartTime) {
        long taskFinishedTime = System.currentTimeMillis();
        long nextTime = taskStartTime + period;
        if(taskFinishedTime >= nextTime){
            nextPeriodStartTime = taskFinishedTime + 1000; // 延迟一秒
        }else{
            nextPeriodStartTime = nextTime;
        }
    }

    /**
     * 获取一轮的数据
     */
    private List<ActiveContractItem.Item> getContractsPerRound() {
        // block period
        long endBlockNumber = getCurrentBlockNumber() - 3;
        long startBlockNumber;
        if(lastBlockNumber == 0){
            startBlockNumber = endBlockNumber - 2;
        }else {
            startBlockNumber = lastBlockNumber + 1;
        }

        if(endBlockNumber < startBlockNumber){
            logger.warn("获取block number, 发现end < start, 可能是时间间隔太小，没有出块， start:[{}], end:[{}]", startBlockNumber, endBlockNumber);
            return Collections.emptyList();
        }

        int pageNo = 0;
        int pageSize = 500;
        int maxPage = MAX_QUEUE_SIZE / pageSize;
        boolean nf = true;


        FixedSizeQueue<ActiveContractItem.Item> container =  new FixedSizeQueue<>(MAX_QUEUE_SIZE);

        while (nf){
            List<ActiveContractItem.Item> contracts = getContracts(startBlockNumber, endBlockNumber, pageNo, pageSize);
            if(contracts.size() > 0 && pageNo<=maxPage) {
                pageNo ++;
                container.enqueue(contracts);
            }else {
                nf = false;
            }
        }

        // 更新轮次
        lastBlockNumber = endBlockNumber;

        return container.getAllElements();
    }

    /**
     * 处理换CovalentKey
     */
    private List<ActiveContractItem.Item> getContracts(long startBlock, long endBlock, int pageNo, int pageSize){
        List<ActiveContractItem.Item> contracts = null;
        for(int i = 0; i < covalentKeys.length; i++){
            String key = getCovalentKey();
            try{
                contracts = getContracts(key, startBlock, endBlock, pageNo, pageSize);
            }catch (Exception e){
                sleep(1500);
                if(currentCovalentKeyIndex >= covalentKeys.length){
                    currentCovalentKeyIndex = 0;
                } else{
                    currentCovalentKeyIndex ++;
                }
            }
            if(contracts != null){
                break;
            }
        }

        if(contracts == null){
            // TODO push message
            throw new RuntimeException("获取合约出错，已经重试还是失败，换key也无效，需要人工介入");
        }
        return contracts;
    }


    private long getCurrentBlockNumber() {
        long ret = 0;
        while (ret == 0){
            try {
                Web3j web3j = Web3j.build(new HttpService("https://mainnet.infura.io/v3/472152e2fab6485e89429bc399b92fdc"));
                BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
                logger.info("Block number: " + blockNumber);
                ret = blockNumber.longValue();
            }catch (Exception e){
                // 重试
                logger.warn("获取块高失败");
            }
        }
        return ret;
    }



    private List<ActiveContractItem.Item> getContracts(String key, long startBlock, long endBlock, int pageNo, int pageSize) {
        String urlPattern ="https://api.covalenthq.com/v1/1/events/topics/0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef/?starting-block=%s&ending-block=%s&page-size=%s&page-number=%s";
        String url = String.format(urlPattern, startBlock, endBlock, pageSize, pageNo);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + key);

        int tryTimes = 3;
        CovalentResult<ActiveContractItem> ret = null;
        RuntimeException ex = null;
        while (tryTimes>0){
            try{
                String res = HttpUtils.sendGetRequest(httpClient, url, headers);
                ret = JacksonUtils.toObj(res, new TypeReference<CovalentResult<ActiveContractItem>>() {});
                if(ret.isError()){
                    ex = new RuntimeException(res);
                }
                break;
            }catch (Exception e){
                tryTimes --;
                if(tryTimes == 0){
                    ex = (RuntimeException) e;
                }
            }
        }

        if(ex != null){
            throw ex;
        }

        assert ret != null;
        List<ActiveContractItem.Item> items = ret.getData().getItems();
        if(items == null){
            return Collections.emptyList();
        }
        return items;
    }

    private String getCovalentKey(){
        if(currentCovalentKeyIndex >= covalentKeys.length){
            currentCovalentKeyIndex = 0;
        }
        return covalentKeys[currentCovalentKeyIndex];
    }


    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
            // ignore
        }
    }


}
