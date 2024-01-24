package com.dehigher.biutils.activecontract;

import com.dehigher.biutils.activecontract.dto.ContractDetailResult;
import com.dehigher.biutils.activecontract.dto.TopItem;
import com.dehigher.biutils.activecontract.dto.TopItemElement;
import com.dehigher.biutils.base.FixedSizeQueue;
import com.dehigher.biutils.base.HttpUtils;
import com.dehigher.biutils.base.JacksonUtils;
import com.dehigher.biutils.base.TimeUtils;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ContractAnalysisExecutor {

    private final Logger log = LoggerFactory.getLogger(ContractAnalysisExecutor.class);

    private final static long validRoundTopExpire =  24 * 60 * 60 * 1000;

    private static final int TASK_QUEUE_SIZE = 10;

    private static final HttpClient httpClient = HttpUtils.createHttpClient(40, 40, 10000, 10000, 5000);

    private Map<String, TopItem> currentTxTop  = null;

    private Map<String, TopItem> currentVoTop = null;

    private Map<String, TopItem> currentDataMap = new HashMap<>();

    private Map<String, TopItem> lastDataMap = new HashMap<>();



    private final Map<String, List<Long>> roundDetailMap = new HashMap<>();

    private FixedSizeQueue<Set<String>> taskQueue = new FixedSizeQueue<>(TASK_QUEUE_SIZE);

    private Thread thread;

    // 定义查询的线程池
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5, 30, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2000),
                new ThreadFactoryBuilder().setNameFormat("handle-getContract-pool-%d").build(),
                new ThreadPoolExecutor.AbortPolicy());
    static {
        executor.prestartCoreThread();
    }

    private static final ExecutorCompletionService poolService = new ExecutorCompletionService<>(executor);

    public ContractAnalysisExecutor(){
        thread = new Thread(()->run());
        thread.start();
    }


    public void addJob(Set<String> contracts){
        taskQueue.enqueue(contracts);
    }

    public void run(){
        try{
            while (true) {
                Set<String> addresses = taskQueue.dequeue();
                if(addresses == null || addresses.size() == 0) {
                    sleep(500);
                    continue;
                }
                long start = System.currentTimeMillis();
                log.info("分析数据开始时间：{}, 待处理数据条数：{}", TimeUtils.format(start), addresses.size());
                handleJob(addresses);
                handleTopPush(addresses); // 榜单推送
                handleYiDongPush(); // 推送异动
                handleCallPush(); // 推动call
                long finish = System.currentTimeMillis();
                log.info("分析数据结束时间：" + TimeUtils.format(finish));
                log.info("持续时间：" + (finish-start)/1000 + "秒");
            }
        }catch (Exception e){
            sleep(1000);
            log.error("数据分析，出现异常，", e);
        }

    }

    /**
     * 功能一：榜单推送
     */
    private void handleTopPush(Set<String> addresses) {
        List<TopItem> collect = currentTxTop.values().stream().sorted(Comparator.comparing(TopItem::getTxnsm5m).reversed()).collect(Collectors.toList());
        // TODO 推送
        System.out.println("===========================功能一：榜单推送=====================================");
        System.out.println("address size:" + addresses.size());
        System.out.println(JacksonUtils.toJson(collect));
    }




    /**
     * 功能二： 推送易动合约
     */
    public void handleYiDongPush() {
        Set<String> keys = currentDataMap.keySet();
        // 设置轮次首轮
        for(String address:keys){
            if(lastDataMap != null && lastDataMap.containsKey(address)){
                TopItem item = currentDataMap.get(address);
                TopItem lastItem = lastDataMap.get(address);
                // 价格增长15%
                boolean priceUp = lastItem.getPriceUsd() != null && !lastItem.getPriceUsd().equals(new BigDecimal("0")) && item.getPriceUsd().divide(lastItem.getPriceUsd(),2, RoundingMode.HALF_UP).compareTo(new BigDecimal("1.15")) > 0;
                // 金额增长4倍
                boolean amountUp = lastItem.getVolume5m()!= null && !lastItem.getVolume5m().equals(new BigDecimal("0")) && item.getVolume5m().divide(lastItem.getVolume5m(), 2, RoundingMode.HALF_UP).compareTo(new BigDecimal("5")) > 0;
                // 15小时外
                boolean outTime = System.currentTimeMillis() - lastItem.getPairCreateAt() >= 60 * 60 * 15 * 1000;

                if(priceUp && amountUp && outTime){
                    // TODO 推送异动合约
                    System.out.println("==============================push 推送异动合约==================================");
                    System.out.println(String.format("address:%s", address));
                }
            }
        }
    }

    /**
     * 功能三：【交易次数榜】= 3次上榜，并且每次【交易量榜】< 4
     */
    public void handleCallPush(){
        Set<String> keys = currentTxTop.keySet();
        // 设置轮次首轮
        for(String address : keys){
            List<Long> roundDetail = roundDetailMap.get(address);
            if( roundDetail!= null && roundDetail.size()== 3){
                // TODO 推送
                System.out.println("==========【交易次数榜】= 3次上榜，并且每次【交易量榜】< 4=============");
                System.out.println(address);
            }
        }
    }

    private void handleJob(Set<String> addresses) {
        PriorityBlockingQueue<TopItemElement> txTop = new PriorityBlockingQueue<>();
        PriorityBlockingQueue<TopItemElement> voTop = new PriorityBlockingQueue<>();

        lastDataMap = currentDataMap;
        currentDataMap = new HashMap<>();

        for(String address : addresses){
           poolService.submit(() -> handleContract(currentDataMap, txTop, voTop, address));
        }

        try {
            for (int i = 0; i < addresses.size(); i++) {
                Future<Integer> completedFuture = poolService.take();
                try {
                    completedFuture.get(6, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    completedFuture.cancel(true);
                    log.warn("获取详情超时, err_message:" + e.getMessage());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("多线程从老鹰查询合约详情出错，err_message:" + e.getMessage());
        }

        if(txTop.size() == 0){
             System.out.println("=====");
        }

        // 保存结果: type 1-tx 、2-vo
        saveResult(txTop, 1);
        saveResult(voTop, 2);

        // // 设置轮次信息
        TopItemElement[] items = txTop.toArray(new TopItemElement[0]);
        for(TopItemElement item : items){
            setRoundDetailMap(currentVoTop, item.getItem().getAddress());
        }

    }


    private Void handleContract(Map<String, TopItem> currentDataMap, PriorityBlockingQueue<TopItemElement> txTop, PriorityBlockingQueue<TopItemElement> voTop, String address) {
        try{
            // 调老鹰
            TopItem item = getContractDetail(address);
            if(item == null){
                return null;
            }
            int capacity = TASK_QUEUE_SIZE;
            // 设置交易次数排行榜
            setTop(txTop, item, capacity);
            // 设置交易金额排行榜
            setTop(voTop, item, capacity);
            // 设置全量数据
            currentDataMap.put(address, item);
        }catch (Exception e){
            log.error("获取合约详情出错,address:[{}]，message:[{}]", address, e.getMessage());
        }
        return null;
    }

    private void setRoundDetailMap(Map<String, TopItem> currentVoTop, String address) {
        long current = System.currentTimeMillis();

        TopItem item = currentVoTop.get(address);
        if(item != null) {
            List<BigDecimal> vItems = currentVoTop
                    .values().stream()
                    .map(TopItem::getVolume5m)
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());

            int index = vItems.indexOf(vItems);
            if(index > 0 && index < 4){
                List<Long> timestamps = roundDetailMap.get(address);
                if(timestamps == null){
                    timestamps = new ArrayList<>();
                }else{
                    Iterator<Long> iterator = timestamps.iterator();
                    while (iterator.hasNext()) {
                        Long next = iterator.next();
                        if(current > next.longValue() + validRoundTopExpire){
                           iterator.remove();
                        }
                    }
                }
                timestamps.add(current);
            }
        }
    }

    // 设置排行榜
    private void setTop(PriorityBlockingQueue<TopItemElement> queue, TopItem item, int capacity) {
        if(queue.size() >= capacity){
            TopItemElement peek = queue.peek();
            if(peek == null){
                return;
            }
            TopItem min = peek.getItem();
            if(min != null && item.getTxnsm5m() <= min.getTxnsm5m()){
                return;
            }else{
                queue.poll();
            }
        }
        queue.offer(new TopItemElement(item, item.getTxnsm5m()) );
    }


    private TopItem getContractDetail(String address) {
        String url = "https://api.dexscreener.com/latest/dex/tokens/" + address;
        ContractDetailResult ret = null;
        RuntimeException ex = null;
        int tryCount = 1;
        while (tryCount >0){
            try{
                String res = HttpUtils.sendGetRequest(httpClient, url, null);
                ret = JacksonUtils.toObj(res, ContractDetailResult.class);
                if(ret == null || ret.getPairs() == null || ret.getPairs().size() == 0){
                    return null;
                }
                break;
            }catch (RuntimeException e){
                tryCount --;
                if(tryCount == 0){
                    ex = e;
                }
            }
        }

        if(ex != null){
            throw ex;
        }
        ContractDetailResult.Pair pair = ret.getPairs().get(0);
        return transContractDetailRet(address, pair);
    }


    private TopItem transContractDetailRet(String address, ContractDetailResult.Pair pair){
        TopItem item = new TopItem();
        item.setAddress(address);
        item.setPriceUsd(pair.getPriceUsd());
        item.setVolume5m(pair.getVolume().getM5());
        ContractDetailResult.Txns txns = pair.getTxns();
        item.setTxnsm5m(txns.getM5().getBuys() + txns.getM5().getSells());
        item.setPairCreateAt(pair.getPairCreatedAt());
        item.setSymbol(pair.getBaseToken().getSymbol());
        return item;
    }

    private void saveResult(PriorityBlockingQueue<TopItemElement> top, int type) {
        TopItemElement[] array = top.toArray(new TopItemElement[0]);
        Map<String, TopItem> map = new HashMap<>();
        for(TopItemElement item:array){
            if(item != null){
                String address = item.getItem().getAddress();
                map.put(address, item.getItem());
            }
        }
        if(type ==  1){
            currentTxTop = map;
        } else if(type == 2){
            currentVoTop = map;
        } else {
            throw new RuntimeException("save type not support");
        }
    }

    public static <K, V> int getKeyIndex(TreeMap<K, V> treeMap, K key) {
        // 使用 headMap 获取小于给定键的部分
        SortedMap<K, V> headMap = treeMap.headMap(key);
        // 计算部分的大小即索引
        return headMap.size();
    }


    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }

}
