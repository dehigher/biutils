package com.dehigher.biutils.base;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import java.nio.charset.StandardCharsets;
import org.apache.http.util.EntityUtils;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * HttpClient工具类
 */
public class HttpUtils {


    public static final String POST_METHOD = "POST";

    public static final String GET_METHOD = "GET";

    public static HttpClient createHttpClientDefault(){
        return createHttpClient(4, 4, 10000, 10000, 10000);
    }

    /**
     * 实例化HttpClient
     *
     * @param maxTotal                 连接池里的最大连接数
     * @param maxPerRoute              某一个/每服务每次能并行接收的请求数量，这里route指的是域名
     * @param socketTimeout            指客户端和服务器建立连接后，客户端从服务器读取数据的timeout，超出后会抛出SocketTimeOutException
     * @param connectTimeout           指客户端和服务器建立连接的timeout
     * @param connectionRequestTimeout 指从连接池获取连接的timeout
     * @return
     */
    public static HttpClient createHttpClient(int maxTotal,
                                              int maxPerRoute,
                                              int socketTimeout,
                                              int connectTimeout,
                                              int connectionRequestTimeout) {
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(socketTimeout)
                .setConnectTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(maxTotal);
        cm.setDefaultMaxPerRoute(maxPerRoute);
        return HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(defaultRequestConfig)
                .build();
    }


    public static String sendGetRequest(HttpClient httpClient,
                                      String url,
                                      Map<String, String> headers) {
        HttpGet httpGet = new HttpGet(url);
        // 添加请求头
        addHeaders(httpGet, headers);
        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpGet)) {
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 发送post请求
     *
     * @param httpClient
     * @return
     */
    public static String sendParamPost(HttpClient httpClient,
                                  String url,
                                  Map<String, String> headers,
                                  Map<String, String> params,
                                  Charset encoding) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");

        // 添加请求头
        addHeaders(httpPost, headers);
        // 添加请求参数
        setPostEntity(httpPost, params, encoding);
        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpPost)) {
            return EntityUtils.toString(response.getEntity(), encoding);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String sendJsonPost(HttpClient httpClient,
                                      String url,
                                      Map<String, String> headers,
                                      String body,
                                      Charset encoding) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-Type", "application/json");
        // 添加请求头
        addHeaders(httpPost, headers);
        // 添加请求参数
        setPostJsonEntity(httpPost, body, encoding);
        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpPost)) {
            return EntityUtils.toString(response.getEntity(), encoding);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void addHeaders(HttpRequest httpRequest, String... kvs){
        if(kvs.length == 0 || kvs.length % 2 !=0) {
            throw new IllegalArgumentException("set http header, kvs.length is illegal, size = " + kvs.length);
        }
        for(int i = 0; i < kvs.length; i += 2){
            String key = kvs[i];
            String value = kvs[i + 1];
            httpRequest.addHeader(key, value);
        }
    }

    public static void setPostJsonEntity(HttpPost httpPost, String body) {
        // 添加请求参数
        if (StringUtils.isNotBlank(body)) {
            StringEntity stringEntity = new StringEntity(body, StandardCharsets.UTF_8);
            httpPost.setEntity(stringEntity);
        }
    }


    public static void addHeaders(HttpRequest httpRequest, Map<String, String> headers) {
        if (headers != null && headers.size() > 0) {
            Iterator<Map.Entry<String, String>> itr = headers.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, String> entry = itr.next();
                httpRequest.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void setPostEntity(HttpPost httpPost, Map<String, String> params, Charset encoding) {
        // 添加请求参数
        if (params != null && params.size() > 0) {
            List<NameValuePair> formParams = new ArrayList<>();
            Iterator<Map.Entry<String, String>> itr = params.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, String> entry = itr.next();
                formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            UrlEncodedFormEntity postEntity = new UrlEncodedFormEntity(formParams, encoding);
            httpPost.setEntity(postEntity);
        }
    }

    private static void setPostJsonEntity(HttpPost httpPost, String body, Charset encoding) {
        if (StringUtils.isNotEmpty(body)) {
            StringEntity stringEntity = new StringEntity(body, encoding);
            httpPost.setEntity(stringEntity);
        }
    }


}
