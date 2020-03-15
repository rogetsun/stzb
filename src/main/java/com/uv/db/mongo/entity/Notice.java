package com.uv.db.mongo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * @author uvsun 2020/3/11 9:17 下午
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document
@Builder
public class Notice {

    @Id
    private String id;

    private String title;
    private String content;
    private String url;
    private String icon;
    private boolean hasNotify;
    private String dingUrl;
    private String dingSecret;
    private Date createTime;
    private Date noticeTime;



}
