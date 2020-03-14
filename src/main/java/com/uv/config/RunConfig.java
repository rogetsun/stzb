package com.uv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author uvsun 2020/3/14 9:06 下午
 */
@Configuration
@ConfigurationProperties(prefix = "run")
@Data
public class RunConfig {
    private int scheduleThreadNum;
    private String threadGroupName;
    private String threadNamePrefix;
    private String cmdLineInit;
    private String cmdLineInitQuery;
    private String cmdLineSaveQuery;
    private String cmdLineGameAutoConfig;
    private String cmdLineGetHero;
    private String cmdLineGetSkill;
    private String cmdLineParseHero;
    private String cmdLineParseSkill;
}
