package com.uv.notify;

import com.dingtalk.api.response.OapiRobotSendResponse;
import com.taobao.api.ApiException;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * @author uvsun 2019-08-02 14:32
 */
public interface Notify {

    /**
     * 发送带link的通知
     *
     * @param title
     * @param content
     * @param msgUrl
     * @param picUrl
     * @return
     * @throws ApiException
     */
    OapiRobotSendResponse sendLinkMsg(String title, String content, String msgUrl, String picUrl) throws ApiException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException;

    /**
     * 发送普通文本通知
     *
     * @param content
     * @return
     * @throws ApiException
     */
    OapiRobotSendResponse sendTextMsg(String content) throws ApiException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException;

    /**
     * 发送标记文本通知
     *
     * @param title
     * @param content
     * @return
     * @throws ApiException
     */
    OapiRobotSendResponse sendMarkDownMsg(String title, String content) throws ApiException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException;
}
