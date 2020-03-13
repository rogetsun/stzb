package com.uv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author uvsun 2020/3/13 11:58 下午
 */
@Configuration
@ConfigurationProperties(prefix = "cbg.schedule")
@Data
public class ScheduleConf {
    private int findDelay;
    private int clearDelay;
    private int noticeDelay;

}
