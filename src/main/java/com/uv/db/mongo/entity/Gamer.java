package com.uv.db.mongo.entity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    private JSONObject json;
    private Date createTime;
    private Date updateTime;

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
    private JSONArray cardList;
    @ToString.Exclude
    private JSONArray skillList;
    @ToString.Exclude
    private List<Integer> skillIdList;
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

}
