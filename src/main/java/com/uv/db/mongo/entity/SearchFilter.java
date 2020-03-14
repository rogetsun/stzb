package com.uv.db.mongo.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Set;

/**
 * @author uvsun 2020/3/11 6:48 下午
 */
@Document(collection = "filter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchFilter {
    //基础配置
    /**
     * 唯一索引
     */
    @Id
    private int id;
    private String name;
    private int minPrice;
    private int maxPrice;
    /**
     * 必选 的 英雄卡 和 技能
     */
    @ToString.Exclude
    private Set<Integer> containsHero;
    @ToString.Exclude
    private Set<Integer> containsSkill;
    /**
     * 可选 的 英雄卡 和 技能;影响契合度
     */
    @ToString.Exclude
    private Set<Integer> optionHero;
    @ToString.Exclude
    private Set<Integer> optionSkill;
    /**
     * 可选 英雄卡 和 技能 的最低契合度,低于则pass
     */
    private int optionHeroMinFitDegree;
    private int optionSkillMinFitDegree;

    @ToString.Exclude
    private String dingUrl;
    @ToString.Exclude
    private String dingSecret;
    /**
     * 配置更新时间
     */
    private Date updateTime;


}
