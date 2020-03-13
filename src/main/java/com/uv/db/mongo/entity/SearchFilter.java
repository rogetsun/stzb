package com.uv.db.mongo.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author uvsun 2020/3/11 6:48 下午
 */
@Document(collection = "filter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchFilter {
    @Id
    private int id;
    private String name;
    private int minPrice;
    private int maxPrice;
    private List<Integer> containsHero;
    private List<Integer> containsSkill;
    private String dingUrl;
    private String dingSecret;
    private Date updateTime;
    @ToString.Exclude
    private Map<String, SimpleGamer> simpleGamerMap;

    private List<String> actionGamerIds;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SimpleGamer{
        private String sn;
        private int price;
        private int lastPrice;
        private Date createTime;
        private Date updateTime;

    }

}
