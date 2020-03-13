package com.uv.notify;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @author uvsun 2019-08-02 19:17
 */
public class HttpUtil {

    private static final Log log = LogFactory.getLog(HttpUtil.class);

    public static String doGet(String url) {
        // 创建httpGet远程连接实例
        HttpGet httpGet = new HttpGet(url);
        // 设置请求头信息，鉴权
        // httpGet.setHeader("Authorization", "Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0");
        // 设置配置请求参数
        RequestConfig requestConfig = getRequestConfig();
        // 为httpGet实例设置配置
        httpGet.setConfig(requestConfig);
        // 执行get请求得到返回对象
        return execRequest(url, httpGet);
    }


    public static String doPostWithFormData(String url, Map<String, Object> paramMap) {

        // 创建httpPost远程连接实例
        HttpPost httpPost = new HttpPost(url);
        // 配置请求参数实例
        RequestConfig requestConfig = getRequestConfig();
        // 为httpPost实例设置配置
        httpPost.setConfig(requestConfig);
        // 设置请求头
        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
        // 封装post请求参数
        if (null != paramMap && paramMap.size() > 0) {
            List<NameValuePair> nvps = new ArrayList<>();
            // 通过map集成entrySet方法获取entity
            Set<Map.Entry<String, Object>> entrySet = paramMap.entrySet();
            // 循环遍历，获取迭代器
            Iterator<Map.Entry<String, Object>> iterator = entrySet.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> mapEntry = iterator.next();
                nvps.add(new BasicNameValuePair(mapEntry.getKey(), mapEntry.getValue().toString()));
            }

            // 为httpPost设置封装好的请求参数
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                log.error("post request param urlencode error, url:" + url, e);
            }
        }
        return execRequest(url, httpPost);
    }


    public static String doPostWithJSONParam(String url, JSONObject json) throws UnsupportedEncodingException {

        // 创建httpPost远程连接实例
        HttpPost httpPost = new HttpPost(url);
        // 配置请求参数实例
        RequestConfig requestConfig = getRequestConfig();
        // 为httpPost实例设置配置
        httpPost.setConfig(requestConfig);
        // 设置请求头
        httpPost.addHeader("Content-Type", "application/json");
        // 封装post请求参数
        if (json != null && !json.isEmpty()) {
            httpPost.setEntity(new StringEntity(json.toJSONString()));
        }

        return execRequest(url, httpPost);
    }

    public static String doPostWithFormData(String url, JSONObject json) {
        Map<String, Object> m = json;
        return doPostWithFormData(url, m);
    }

    private static String execRequest(String url, HttpUriRequest httpRequest) {
        String result = null;
        // 创建httpClient实例
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse httpResponse = null;
        try {
            // httpClient对象执行post请求,并返回响应参数对象
            httpResponse = httpClient.execute(httpRequest);
            // 从响应对象中获取响应内容
            HttpEntity entity = httpResponse.getEntity();
            result = EntityUtils.toString(entity);
        } catch (IOException e) {
            log.error("post request error, url:" + url, e);

        } finally {
            // 关闭资源
            if (null != httpResponse) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    private static RequestConfig getRequestConfig() {
        return RequestConfig.custom()
                // 设置连接主机服务超时时间
                .setConnectTimeout(35000)
                // 设置连接请求超时时间
                .setConnectionRequestTimeout(35000)
                // 设置读取数据连接超时时间
                .setSocketTimeout(60000)
                .build();
    }
}
