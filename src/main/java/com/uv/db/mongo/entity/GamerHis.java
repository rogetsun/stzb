package com.uv.db.mongo.entity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

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
public class GamerHis {
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
    private List<Gamer.GamerHero> gamerHeroes;
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

    public Gamer toGamer() {
        return Gamer.builder()
                .orderSn(this.getOrderSn())
                .icon(this.getIcon())
                .dianCangCount(this.getDianCangCount())
                .dianJiCount(this.getDianJiCount())
                .fiveStarCount(this.getFiveStarCount())
                .skillCount(this.getSkillCount())
                .serverId(this.getServerId())
                .price(this.getPrice())
                .changeCount(this.getChangeCount())
                .title(this.getTitle())
                .highText(this.getHighText())
                .createTime(this.getCreateTime())
                .updateTime(this.getUpdateTime())
                .dealTime(this.getDealTime())
                .hasDetail(this.isHasDetail())
                .firstPrice(this.getFirstPrice())
                .name(this.getName())
                .sellStatus(this.getSellStatus())
                .sellStatusDesc(this.getSellStatusDesc())
                .gamerHeroes(this.getGamerHeroes())
                .heroIdIdxMap(this.getHeroIdIdxMap())
                .skillList(this.getSkillList())
                .skillIds(this.getSkillIds())
                .dianCangList(this.getDianCangList())
                .dianJiList(this.getDianJiList())
                .cardFeatureList(this.getCardFeatureList())
                .tenure(this.getTenure())
                .build();
    }

    public static GamerHis buildFromGamer(Gamer gamer) {
        return GamerHis.builder()
                .orderSn(gamer.getOrderSn())
                .icon(gamer.getIcon())
                .dianCangCount(gamer.getDianCangCount())
                .dianJiCount(gamer.getDianJiCount())
                .fiveStarCount(gamer.getFiveStarCount())
                .skillCount(gamer.getSkillCount())
                .serverId(gamer.getServerId())
                .price(gamer.getPrice())
                .changeCount(gamer.getChangeCount())
                .title(gamer.getTitle())
                .highText(gamer.getHighText())
                .createTime(gamer.getCreateTime())
                .updateTime(gamer.getUpdateTime())
                .dealTime(gamer.getDealTime())
                .hasDetail(gamer.isHasDetail())
                .firstPrice(gamer.getFirstPrice())
                .name(gamer.getName())
                .sellStatus(gamer.getSellStatus())
                .sellStatusDesc(gamer.getSellStatusDesc())
                .gamerHeroes(gamer.getGamerHeroes())
                .heroIdIdxMap(gamer.getHeroIdIdxMap())
                .skillList(gamer.getSkillList())
                .skillIds(gamer.getSkillIds())
                .dianCangList(gamer.getDianCangList())
                .dianJiList(gamer.getDianJiList())
                .cardFeatureList(gamer.getCardFeatureList())
                .tenure(gamer.getTenure())
                .build();
    }

}
