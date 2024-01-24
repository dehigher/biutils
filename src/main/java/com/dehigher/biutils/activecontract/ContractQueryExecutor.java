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

    private static final FixedSizeQueue<ActiveContractItem.Item> container = new FixedSizeQueue<>(MAX_QUEUE_SIZE);

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

    private long startTime = 0;

    private long tickTime = 10 * 1000;

    private long pointBlockNumber = 0;

    private final long period;

    private final Thread thread = new Thread(()-> run());

    private final ContractAnalysisExecutor analysisExecutor = new ContractAnalysisExecutor();

    public ContractQueryExecutor(long period) {
        this.period = period;
    }

    /**
     * 启动线程
     */
    public void start(){
        thread.start();
    }

    private String run(){
        pointBlockNumber = getCurrentBlockNumber() - 3;
        // 到了执行事件, 更新轮次数据, 5分钟执行一次
        while(true){
            run(pointBlockNumber);
        }
    }

    private void run(long startBlockNumber) {
        long blockTimestamp = 0;
        while (true){
            List<ActiveContractItem.Item> contractsOfBlock = getContractsOfBlock(startBlockNumber);
            blockTimestamp = TimeUtils.time2stamp("YYYY-MM-ddThh:mm:ssZ", contractsOfBlock.get(0).getBlock_signed_at());
            if(startTime == 0 && pointBlockNumber == startBlockNumber){
                startTime = blockTimestamp;
                continue;
            }
            if((blockTimestamp - startTime) > period) {
               break;
            }else {
                pointBlockNumber ++;
                container.enqueue(contractsOfBlock);
            }
        }

        // 提交任务
        List<ActiveContractItem.Item> all = container.getAllElements();
        Set<String> ret = filterContract(all);
        analysisExecutor.addJob(ret);
        FileUtil.writeToFile(JacksonUtils.toJson(ret), "contracts/last_5m_" + startTime + "_" + TimeUtils.format(blockTimestamp));

        // 更新下一个轮多时间
        pointBlockNumber ++; // 多一个块
        startTime += tickTime; // 更新下一轮开始时间
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

    /**
     * 获取一个块的数据
     */
    private List<ActiveContractItem.Item> getContractsOfBlock(long pointBlockNumber) {

        int pageNo = 0;
        int pageSize = 500;
        int maxPage = MAX_QUEUE_SIZE / pageSize;
        boolean nf = true;


        ArrayList<ActiveContractItem.Item> ret = new ArrayList<>();
        while (nf){
            List<ActiveContractItem.Item> contracts = getContracts(pointBlockNumber, pointBlockNumber + 1, pageNo, pageSize);
            if(contracts.size() > 0 && pageNo <= maxPage) {
                pageNo ++;
                ret.addAll(contracts);
            }else {
                nf = false;
            }
        }
        return ret;
    }


    /**
     * 处理换CovalentKey
     */
    private List<ActiveContractItem.Item> getContracts(long startBlock, long endBlock, int pageNo, int pageSize){
        List<ActiveContractItem.Item> contracts = null;
        while (true){
            String key = getCovalentKey();
            try{
                contracts = getContracts(key, startBlock, endBlock, pageNo, pageSize);
            }catch (Exception e){
                sleep(1500);
                if(currentCovalentKeyIndex >= covalentKeys.length){
                    currentCovalentKeyIndex = 0;
                    logger.info("获取合约出错，已经重试一轮还是失败，换key也无效，需要人工介入，暂时继续重试");
                } else{
                    currentCovalentKeyIndex ++;
                }
            }
            if(contracts != null){
                break;
            }
        }
        return contracts;
    }


    private long getCurrentBlockNumber() {
        long ret = 0;
        while (ret == 0){
            try {
                // Web3j web3j = Web3j.build(new HttpService("https://mainnet.infura.io/v3/472152e2fab6485e89429bc399b92fdc"));
                Web3j web3j = Web3j.build(new HttpService("https://rpc.notadegen.com/eth"));
                BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
                logger.info("Block number: " + blockNumber);
                ret = blockNumber.longValue();
            }catch (Exception e){
                // 重试
                logger.warn("获取块高失败: message:" + e.getMessage());
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
