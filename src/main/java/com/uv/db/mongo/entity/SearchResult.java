package com.uv.db.mongo.entity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author uvsun 2020/3/14 2:17 下午
 */
@Data
@Document(collection = "result")
@Builder
@Slf4j
public class SearchResult {
    @Id
    private int id;
    /**
     * 搜索条件一对一关联
     */
    private int searchFilterId;

    //筛选过滤后关注的角色相关数据
    /**
     * 关注角色最新更新时间
     */
    private Date updateTime;
    @ToString.Exclude
    private Map<String, SimpleGamer> simpleGamerMap;

    /**
     * 入库才调用 refreshActionGamerIds() 从 simpleGamerMap 更新,平时只更新 simpleGamerMap
     */
    private Set<String> actionGamerIds;

    public void refreshActionGamerIds() {
        if (this.getSimpleGamerMap() != null) {
            this.setActionGamerIds(this.getSimpleGamerMap().keySet());
        }
    }

    public void actionGamer(SearchResult.SimpleGamer simpleGamer, long timestamp) {
        if (this.getSimpleGamerMap() == null) {
            this.setSimpleGamerMap(new HashMap<>());
        }
        this.getSimpleGamerMap().put(simpleGamer.getSn(), simpleGamer);

        this.setUpdateTime(new Date(timestamp));
    }

    public void unActionGamer(Gamer gamer, long timestamp) {

        if (this.getSimpleGamerMap() != null) {
            if (this.getSimpleGamerMap().containsKey(gamer.getOrderSn())) {
                this.getSimpleGamerMap().remove(gamer.getOrderSn());
                this.setUpdateTime(new Date(timestamp));
            }
        }

    }

    public boolean isActionedGamer(Gamer gamer) {
        if (this.getSimpleGamerMap() == null || gamer == null) {
            return false;
        }
        return this.getSimpleGamerMap().containsKey(gamer.getOrderSn());
    }

    public SearchResult.SimpleGamer getActionSimpleGamer(Gamer gamer) {
        if (gamer == null) {
            return null;
        }
        Map<String, SimpleGamer> m = this.getSimpleGamerMap();
        if (m == null || !m.containsKey(gamer.getOrderSn())) {
            return null;
        }
        return m.get(gamer.getOrderSn());
    }

    public static SimpleGamer generateSimpleGamer(Gamer gamer, long timestamp) {
        log.trace("[RE]generateSimpleGamer:" + gamer.getPrintInfo());
        return SearchResult.SimpleGamer.builder()
                .lastPrice(0)
                .price(gamer.getPrice())
                .sn(gamer.getId())
                .createTime(new Date(timestamp)).updateTime(new Date(timestamp))
                .build();
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SimpleGamer {
        private String sn;
        private int price;
        private int lastPrice;
        /**
         * Option英雄匹配度
         */
        private int heroFitDegree;
        /**
         * Option 技能匹配度
         */
        private int skillFitDegree;
        /**
         * 核心英雄 升星数综合, 觉醒数总和, 兵种解锁数总和, 兵种进阶数总和
         */
        private int coreHeroAdvanceSum;
        private int coreHeroAwakeSum;
        private int coreHeroArmUnlockSum;
        private int coreHeroArmAdvanceSum;
        /**
         * 可选英雄 升星数综合, 觉醒数总和, 兵种解锁数总和, 兵种进阶数总和
         */
        private int optionHeroAdvanceSum;
        private int optionHeroAwakeSum;
        private int optionHeroArmUnlockSum;
        private int optionHeroArmAdvanceSum;

        private Date createTime;
        private Date updateTime;

    }


}
