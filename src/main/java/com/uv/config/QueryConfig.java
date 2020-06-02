package com.uv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * @author uvsun 2020/3/12 1:33 上午
 */
@Configuration
@ConfigurationProperties(prefix = "cbg.query-config")
@Data
public class QueryConfig {
    private String queryConfigFile;
//    private int id;
//    private Set<Integer> hero;
//    private Set<Integer> skill;
//
//    private Set<Integer> optionHero;
//    private Set<Integer> optionSkill;
//    private int optionHeroMinFitDegree;
//    private int optionSkillMinFitDegree;
//
//    private int minPrice;
//    private int maxPrice;
}
