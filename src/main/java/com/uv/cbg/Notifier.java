package com.uv.cbg;

import com.dingtalk.api.response.OapiRobotSendResponse;
import com.taobao.api.ApiException;
import com.uv.db.mongo.entity.Notice;
import com.uv.db.mongo.repository.NoticeRepository;
import com.uv.notify.DingNotify;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author uvsun 2020/3/13 2:39 上午
 * 将通知表的通知信息发送
 */
@Component
@Data
@Slf4j
public class Notifier {

    @Resource
    private NoticeRepository noticeRepository;
    @Resource
    private DingNotify dingNotify;

    public Notice notice() throws NoSuchAlgorithmException, ApiException, InvalidKeyException, UnsupportedEncodingException {
        log.trace("[NOTICE] begin to notice");
        Notice notice = noticeRepository.findFirstByHasNotify(false);
        if (notice != null) {
            log.info("[NOTICE]" + notice.toString());
            OapiRobotSendResponse response = dingNotify.sendLinkMsg(notice.getDingUrl(), notice.getDingSecret(), notice.getTitle(), notice.getContent(), notice.getUrl(), notice.getIcon());
            if (response.getErrcode() != 0) {
                log.error("[NOTICE]send err," + response.getErrmsg());
            } else {
                noticeRepository.delete(notice);
            }
        }
        log.trace("[NOTICE] end to notice");
        return notice;
    }

    public void deleteAll() {
        noticeRepository.deleteAll();
    }
}
