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
    private String init;
    private String initQuery;
    private String saveQuery;
    private String gameAutoConfig;
    private String getHero;
    private String getSkill;
    private String parseHero;
    private String parseSkill;
}
