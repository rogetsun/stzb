package com.uv.notify;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.taobao.api.ApiException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * @author uvsun 2019-08-02 14:31
 */
@Service("dingNotify")
@Scope("singleton")
@Slf4j
@Data
public class DingNotify implements Notify {

    @Value("#{dingConf['url']}")
    private String url;

    @Value("#{dingConf.sendWaitMilliseconds}")
    private int sendWaitMilliseconds;

    @Value("#{dingConf.secret}")
    private String secret;

    /**
     * 发送藏宝阁 帐号信息 link的钉钉通知
     *
     * @param title
     * @param content
     * @param msgUrl
     * @param picUrl
     * @return
     * @throws ApiException
     */
    @Override
    public OapiRobotSendResponse sendLinkMsg(String title, String content, String msgUrl, String picUrl) throws ApiException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {

        return this.sendLinkMsg(this.url, this.secret, title, content, msgUrl, picUrl);
    }

    @Override
    public OapiRobotSendResponse sendTextMsg(String content) throws ApiException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        return this.sendTextMsg(this.url, this.secret, content);
    }

    @Override
    public OapiRobotSendResponse sendMarkDownMsg(String title, String content) throws ApiException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        return this.sendMarkDownMsg(this.url, this.secret, title, content);
    }

    @Override
    public OapiRobotSendResponse sendLinkMsg(String url, String secret, String title, String content, String msgUrl, String picUrl) throws ApiException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        DingTalkClient client = new DefaultDingTalkClient(this.getSecretURL(url, secret));
        OapiRobotSendRequest request = new OapiRobotSendRequest();

        request.setMsgtype("link");
        OapiRobotSendRequest.Link link = new OapiRobotSendRequest.Link();
        link.setMessageUrl(msgUrl);
        link.setPicUrl(picUrl);
        link.setTitle(title);
        link.setText(content);
        request.setLink(link);

        OapiRobotSendResponse response = client.execute(request);

        log.debug("[DD]" + response.getErrcode() + response.getErrmsg());
        return response;
    }

    @Override
    public OapiRobotSendResponse sendTextMsg(String url, String secret, String content) throws ApiException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        DingTalkClient client = new DefaultDingTalkClient(this.getSecretURL(url, secret));
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype("text");
        OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
        text.setContent(content);
        request.setText(text);
        OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
        at.setAtMobiles(Arrays.asList("13835176799"));
        request.setAt(at);
        OapiRobotSendResponse response = client.execute(request);
        log.debug("[DD]" + response.getErrcode() + response.getErrmsg());
        return response;
    }

    @Override
    public OapiRobotSendResponse sendMarkDownMsg(String url, String secret, String title, String content) throws ApiException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        DingTalkClient client = new DefaultDingTalkClient(this.getSecretURL(url, secret));
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype("markdown");
        OapiRobotSendRequest.Markdown markdown = new OapiRobotSendRequest.Markdown();
        markdown.setTitle(title);
        markdown.setText(content);
        request.setMarkdown(markdown);
        OapiRobotSendResponse response = client.execute(request);
        log.debug("[DD]" + response.getErrcode() + response.getErrmsg());
        return response;
    }

    /**
     * 获取钉钉加签后的加密URL
     *
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     */
    private String getSecretURL() throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {

        return this.getSecretURL(this.url, this.secret);
    }

    private String getSecretURL(String url, String secret) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        long timestamp = System.currentTimeMillis();
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String secretURL = URLEncoder.encode(new String(Base64.encodeBase64(signData)), "UTF-8");
        return url + "&timestamp=" + timestamp + "&sign=" + secretURL;
    }

}
