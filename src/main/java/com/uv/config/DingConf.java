package com.uv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author uvsun 2020/3/12 2:37 上午
 */
@Configuration
@ConfigurationProperties(prefix = "notify.ding")
@Data
public class DingConf {
    private String url;
    private int sendWaitMilliseconds;
    private String secret;
    private String gamerUrl;
}
