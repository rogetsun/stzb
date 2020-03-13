package com.uv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author uvsun 2020/3/12 1:33 上午
 */
@Configuration
@ConfigurationProperties(prefix = "cbg.query-config")
@Data
public class QueryConfig {

    private List<Integer> hero;
    private List<Integer> skill;
    private int minPrice;
    private int maxPrice;
}
