package com.uv.db.mongo.entity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author uvsun 2020/3/11 9:15 下午
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document
@Builder
public class Gamer {
    @Id
    private String id;
    private String orderSn;
    private String icon;
    private int dianCangCount;
    private int dianJiCount;
    private int fiveStarCount;
    private int skillCount;
    private int serverId;
    private int price;
    private int changeCount;
    private String title;
    private String highText;
    private Date createTime;
    private Date updateTime;
    private Date dealTime;
    private boolean hasDetail;

    //以下信息为请求cbg查询了详细信息后获得

    private int firstPrice;
    private String name;
    /**
     * 销售状态
     * * 0:买家取回,2:上架中,包含未过公示期,6:买家取走
     * 猜测
     * * 1:下架,3:下单....
     */
    private int sellStatus;
    private String sellStatusDesc;

    @ToString.Exclude
    private List<GamerHero> gamerHeroes;
    @ToString.Exclude
    private Map<Integer, Integer> heroIdIdxMap;
    @ToString.Exclude
    private JSONArray skillList;
    @ToString.Exclude
    private Set<Integer> skillIds;
    @ToString.Exclude
    private JSONArray dianCangList;
    @ToString.Exclude
    private JSONArray dianJiList;
    @ToString.Exclude
    private JSONArray cardFeatureList;

    /**
     * 各种玉符(元宝) 虎符 月卡结束时间 等
     */
    private JSONObject tenure;

    public String getPrintInfo() {
        return "Gamer[" + this.orderSn + " : " + this.name + " : " + this.price + "]";
    }

    public static void main(String[] args) {
        String a = "[\n" +
                "    {\n" +
                "      \"hit_range\": 2,\n" +
                "      \"dynamic_icon\": 0,\n" +
                "      \"hero_features\": 0,\n" +
                "      \"name\": \"张角\",\n" +
                "      \"hero_type_advance\": 0,\n" +
                "      \"awake_state\": 0,\n" +
                "      \"country\": 5,\n" +
                "      \"is_season_card\": 0,\n" +
                "      \"card_border\": \"\",\n" +
                "      \"hero_type_availible\": [],\n" +
                "      \"hero_type\": 3,\n" +
                "      \"cost\": 2.5,\n" +
                "      \"season\": \"N\",\n" +
                "      \"icon_hero_id\": 100008,\n" +
                "      \"cfg_hero_type_availible\": [\n" +
                "        31,\n" +
                "        23\n" +
                "      ],\n" +
                "      \"quality\": 5,\n" +
                "      \"advance_num\": 5,\n" +
                "      \"hero_id\": 100008\n" +
                "    },\n" +
                "    {\n" +
                "      \"hit_range\": 3,\n" +
                "      \"dynamic_icon\": 0,\n" +
                "      \"hero_features\": 0,\n" +
                "      \"name\": \"马超\",\n" +
                "      \"hero_type_advance\": 1,\n" +
                "      \"awake_state\": 1,\n" +
                "      \"country\": 5,\n" +
                "      \"is_season_card\": 0,\n" +
                "      \"card_border\": \"\",\n" +
                "      \"hero_type_availible\": [\n" +
                "        23\n" +
                "      ],\n" +
                "      \"hero_type\": 3,\n" +
                "      \"cost\": 3,\n" +
                "      \"season\": \"N\",\n" +
                "      \"icon_hero_id\": 100013,\n" +
                "      \"cfg_hero_type_availible\": [\n" +
                "        23,\n" +
                "        43\n" +
                "      ],\n" +
                "      \"quality\": 5,\n" +
                "      \"advance_num\": 0,\n" +
                "      \"hero_id\": 100013\n" +
                "    },\n" +
                "    {\n" +
                "      \"hit_range\": 4,\n" +
                "      \"dynamic_icon\": 1,\n" +
                "      \"hero_features\": 0,\n" +
                "      \"name\": \"吕蒙\",\n" +
                "      \"hero_type_advance\": 1,\n" +
                "      \"awake_state\": 1,\n" +
                "      \"country\": 4,\n" +
                "      \"is_season_card\": 0,\n" +
                "      \"card_border\": \"\",\n" +
                "      \"hero_type_availible\": [\n" +
                "        31\n" +
                "      ],\n" +
                "      \"hero_type\": 1,\n" +
                "      \"cost\": 3.5,\n" +
                "      \"season\": \"N\",\n" +
                "      \"icon_hero_id\": 100035,\n" +
                "      \"cfg_hero_type_availible\": [\n" +
                "        11,\n" +
                "        31\n" +
                "      ],\n" +
                "      \"quality\": 5,\n" +
                "      \"advance_num\": 3,\n" +
                "      \"hero_id\": 100035\n" +
                "    }]";
        JSONArray j = JSON.parseArray(a);
        System.out.println(a);
        List<GamerHero> gs = j.toJavaList(GamerHero.class);
        gs.forEach(System.out::println);
    }

    @Data
    public static class GamerHero {
        /**
         * {
         * "hit_range": 2,
         * "dynamic_icon": 0,
         * "hero_features": 0,
         * "name": "张角",
         * "hero_type_advance": 0,
         * "awake_state": 0,
         * "country": 5,
         * "is_season_card": 0,
         * "card_border": "",
         * "hero_type_availible": [],
         * "hero_type": 3,
         * "cost": 2.5,
         * "season": "N",
         * "icon_hero_id": 100008,
         * "cfg_hero_type_availible": [
         * 31,
         * 23
         * ],
         * "quality": 5,
         * "advance_num": 5,
         * "hero_id": 100008
         * },
         */
        private int hitRange;
        private int dynamicIcon;
        private int heroFeatures;
        private String name;
        /**
         * 兵种进阶标识
         */
        private int heroTypeAdvance;
        /**
         * 兵种觉醒状态
         */
        private int awakeState;
        private int country;
        private int isSeasonCard;
        private String cardBorder;
        /**
         * 解锁兵种集合
         */
        private Set<Integer> heroTypeAvailible;
        private int heroType;
        private BigDecimal cost;
        private String season;
        private int heroId;
        /**
         * 全部可解锁兵种集合
         */
        private Set<Integer> cfgHeroTypeAvailible;
        /**
         * 几星英雄
         */
        private int quality;
        /**
         * 进阶几星,几红
         */
        private int advanceNum;
        private int iconHeroId;


    }
}
