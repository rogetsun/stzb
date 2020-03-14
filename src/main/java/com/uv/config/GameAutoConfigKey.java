package com.uv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author uvsun 2020/3/14 2:23 上午
 * 藏宝阁 game-auto-config.js 返回的 JSON 内容对应 Key
 */
@Configuration
@ConfigurationProperties(prefix = "cbg.game-auto-config-key")
@Data
public class GameAutoConfigKey {
    private String skills;
    private String hero;
    private String dianJi;
    private String dianCang;
    private String specialHero;

    @Configuration
    @ConfigurationProperties(prefix = "cbg.game-auto-config-key.hero-key")
    @Data
    public static class HeroKeyConf {
        private String heroId;
        private String country;
        private String name;
        private String season;
        private String heroType;
        private String quality;
        private String pinyin;
        private String iconHeroId;
    }

    @Configuration
    @ConfigurationProperties(prefix = "cbg.game-auto-config-key.skill-key")
    @Data
    public static class SkillKeyConf {
        private String name;
        private String skillType;
        private String skillId;
    }
}
