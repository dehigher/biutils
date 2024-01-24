package com.dehigher.biutils;

import com.dehigher.biutils.base.FixedSizeQueue;
import com.dehigher.biutils.base.HttpUtils;
import org.apache.http.client.HttpClient;
import org.junit.Test;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;


public class BaseTest {

    private HttpClient httpClient = HttpUtils.createHttpClientDefault();

    private static List<String> covalentKeys;

    static {
        covalentKeys = new ArrayList<>();
        covalentKeys.add("cqt_rQWCVKYGRhXdmGMKytKrrR3bDrFj");
        covalentKeys.add("cqt_rQBWy8yG3b7J9P4v7hcJcHCTwMK4");
    }

    //  当前有效的key
    private String currentValidCovalentKey = covalentKeys.get(0);

    /**
     * curl -X GET https://api.covalenthq.com/v1/eth-mainnet/block_v2/{startDate}/{endDate}/? \
     *     -H 'Content-Type: application/json' \
     *     -u YOUR_API_KEY: \
     */
    @Test
    public void testGet(){
        String key = "cqt_rQCv66KmQD3wccfvyckP8YgCjyj7";
        String url = "https://api.covalenthq.com/v1/eth-mainnet/block_v2/10/?";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + key);
        String ret = HttpUtils.sendGetRequest(httpClient, url, headers);
        System.out.println("=====ret=====");
        System.out.println(ret);
        System.out.println("============");
    }

    @Test
    public void testQueryContract(){
        String key = "cqt_rQCv66KmQD3wccfvyckP8YgCjyj7";
        String url ="https://api.covalenthq.com/v1/1/events/topics/0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef/?starting-block=%s&ending-block=%s";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + key);
        long endBlockNumber = getCurrentBlockNumber() - 3;
        long startBlockNumber = endBlockNumber -2;
        String ret = HttpUtils.sendGetRequest(httpClient, String.format(url, startBlockNumber, endBlockNumber), headers);
        System.out.println("=====ret=====");
        System.out.println(ret);
        System.out.println("============");
    }

    @Test
    public void testGetBlockHeight(){
        try {
            Web3j web3j = Web3j.build(new HttpService("https://mainnet.infura.io/v3/472152e2fab6485e89429bc399b92fdc"));
            BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
            System.out.println("当前块高：" + blockNumber);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testGetToken(){
        String address = "0x5d9f940eabc07633158532e62e79746af0a12e64";
        String url = "https://api.dexscreener.com/latest/dex/tokens/" + address;
        String ret = HttpUtils.sendGetRequest(httpClient, url, null);
        System.out.println("===========");
        System.out.println(ret);
    }


    @Test
    public void testIntStream() {
        IntStream.rangeClosed(1, 20).parallel().forEach(i -> System.out.println(i));
    }


    @Test
    public  void testQueue() {
        int capacity = 200;
        FixedSizeQueue<Integer> queue = new FixedSizeQueue<>(capacity);

        // 添加元素
        for (int i = 1; i <= 100; i++) {
            queue.enqueue(i);
            System.out.println("Enqueued: " + i + ", Queue: " + queue);
        }

        List<Integer> all = queue.getAllElements();
        System.out.println("all size:" + all.size());

        System.out.println("=====");
        System.out.println("all size:" + all.size());
    }

    @Test
    public  void test() {
        int capacity = 200;
        FixedSizeQueue<Integer> queue = new FixedSizeQueue<>(capacity);
        // 添加元素
        for (int i = 1; i <= 10; i++) {
            queue.enqueue(i);
            System.out.println("Enqueued: " + i + ", Queue: " + queue);
        }

        while (!queue.isEmpty()){
            System.out.println("dequeue" + queue.dequeue());
        }
    }


    @Test
    public void testTimeFormat() {
        String timestampString = "2024-01-19T14:30:11Z";

        // 使用 DateTimeFormatter 解析时间字符串
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        LocalDateTime localDateTime = LocalDateTime.parse(timestampString, formatter);

        // 将 LocalDateTime 转换为时间戳
        Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
        long timestamp = instant.toEpochMilli();

        System.out.println("Original Timestamp String: " + timestampString);
        System.out.println("Converted Timestamp: " + timestamp);
    }


    private long getCurrentBlockNumber() {
        try {
            Web3j web3j = Web3j.build(new HttpService("https://mainnet.infura.io/v3/472152e2fab6485e89429bc399b92fdc"));
            BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
            return blockNumber.longValue();
        }catch (Exception e){
            throw new RuntimeException("获取块高出错", e);
        }
    }



}
