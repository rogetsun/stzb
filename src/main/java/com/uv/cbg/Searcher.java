package com.uv.cbg;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.uv.config.CbgReturnKey;
import com.uv.db.mongo.entity.Gamer;
import com.uv.db.mongo.entity.SearchFilter;
import com.uv.exception.CbgException;
import com.uv.notify.HttpUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author uvsun 2020/3/12 5:47 下午
 * 角色详细信息查询获取
 */
@Component
@Slf4j
@Data
public class Searcher {

    @Value("${cbg.queryUrl}")
    private String cbgSearchUrl;
    @Value("${cbg.queryDetailUrl}")
    private String cbgQueryDetailUrl;


    /**
     * key:
     * sn: "game_ordersn"
     * price: "price"
     * highLight: "highlights"
     * serverId: "serverid"
     * otherInfo: "other_info"
     * oi:
     * basicAttr: "basic_attrs"
     * dianJi: "available_dian_ji_count"
     * dianCang: "available_dian_cang_count"
     */
    @Value("${cbg.gamer.key.sn}")
    private String key2sn;
    @Value("${cbg.gamer.key.price}")
    private String key2price;
    @Value("${cbg.gamer.key.highLight}")
    private String key2highLight;
    @Value("${cbg.gamer.key.serverId}")
    private String key2serverId;
    @Value("${cbg.gamer.key.otherInfo}")
    private String key2otherInfo;
    @Value("${cbg.gamer.key.oi.basicAttr}")
    private String key2basicAttr;
    @Value("${cbg.gamer.key.oi.dianJi}")
    private String key2dianJi;
    @Value("${cbg.gamer.key.oi.dianCang}")
    private String key2dianCang;

    @Resource
    private CbgReturnKey cbgReturnKey;

    /**
     * * String a = "https://stzb.cbg.163.com/cgi/mweb/pl/role?" +
     * * "platform_type=1" +
     * * "&price_min=400000" +
     * * "&price_max=499900" +
     * * "&card__hero_id=100027,100016,100035,100013,100480" +
     * * "&page=1";
     *
     * @param searchFilter 搜索过滤的query条件
     * @param page         页数
     * @return cbgQueryUrl
     */
    private String generateQueryUrl(SearchFilter searchFilter, int page) {
        if (cbgSearchUrl == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(cbgSearchUrl);
        if (searchFilter.getMinPrice() > 0) {
            sb.append("&").append("price_min=").append(searchFilter.getMinPrice());
        }
        if (searchFilter.getMaxPrice() > 0) {
            sb.append("&").append("price_max=").append(searchFilter.getMaxPrice());
        }
        if (searchFilter.getContainsHero() != null && searchFilter.getContainsHero().size() > 0) {
            String tmpHeros = Arrays.toString(searchFilter.getContainsHero().toArray());
            sb.append("&").append("card__hero_id=").append(tmpHeros.substring(1, tmpHeros.length() - 1).replaceAll(" ", ""));
        }
        sb.append("&page=").append(page);
        log.trace("[CBG]cbg search query url:" + sb.toString());

        return sb.toString();

    }


    /**
     * 根据queryFilter初步查询帐号
     *
     * @param searchFilter
     * @return
     */
    public List<Gamer> execNormalSearch(SearchFilter searchFilter) throws Throwable {

        List<Gamer> gamers = new ArrayList<>();

        int page = 1;
        try {
            while (true) {
                String url = this.generateQueryUrl(searchFilter, page++);

                String ret = HttpUtil.doGet(url);

                JSONObject r = JSON.parseObject(ret);

                int status = r.getIntValue(cbgReturnKey.getStatus());
                if (status != 1) {
                    String msg = r.getString(cbgReturnKey.getMsg());
                    log.error("查询藏宝课失败,status:" + status + ", msg:" + msg);
                    break;
                }

                JSONArray results = r.getJSONArray(cbgReturnKey.getResult());
                r.remove(cbgReturnKey.getResult());
                log.trace(r.toJSONString());

                int num = r.getInteger(cbgReturnKey.getNum());
                log.debug("found [" + results.size() + "] gamers, total:[" + num + "], page: " + r.getJSONObject(cbgReturnKey.getPaging()).toJSONString());

                gamers.addAll(this.generateGamers(results, results.size()));

                if (r.getJSONObject(cbgReturnKey.getPaging()).getBoolean(cbgReturnKey.getIsLastPage()) || gamers.size() >= num) {
                    break;
                }
            }
        } catch (Throwable e) {
            log.error("初步条件查询cbg失败,找到[" + gamers.size() + "]个角色", e);
            throw e;
        }

        return gamers;
    }


    private List<Gamer> generateGamers(JSONArray jsonArray, int num) {
        List<Gamer> gamerList = new ArrayList<>(num);
        jsonArray.forEach(tmpJ -> {
            JSONObject j = JSON.parseObject(tmpJ.toString());
            Gamer g = generateGamer(j);
            gamerList.add(g);
        });

        return gamerList;
    }

    /**
     * 根据基本角色信息生成Gamer
     *
     * @param j
     * @return
     */
    public Gamer generateGamer(JSONObject j) {
        return Gamer.builder()
                .id(j.getString(key2sn))
                .orderSn(j.getString(key2sn))
                .icon(j.getString(cbgReturnKey.getIcon()))
                .highText(Arrays.toString(j.getJSONArray(key2highLight).toArray()))
                .price(j.getIntValue(key2price))
                .dianCangCount(j.getJSONObject(key2otherInfo).getIntValue(key2dianCang))
                .dianJiCount(j.getJSONObject(key2otherInfo).getIntValue(key2dianJi))
                .serverId(j.getIntValue(key2serverId))
                .title(Arrays.toString(j.getJSONObject(key2otherInfo).getJSONArray(key2basicAttr).toArray()))
                .fiveStarCount(Integer.parseInt(j.getJSONObject(key2otherInfo).getJSONArray(key2basicAttr).get(0).toString().replaceAll("[^0-9*]", "")))
                .skillCount(Integer.parseInt(j.getJSONObject(key2otherInfo).getJSONArray(key2basicAttr).get(1).toString().replaceAll("[^0-9*]", "")))
                .hasDetail(false)
                .build();
    }

    /**
     * 根据基本角色信息生成Gamer
     *
     * @param j
     * @return
     */
    public void generateGamer(JSONObject j, Gamer gamer) {
        gamer.setId(j.getString(key2sn));
        gamer.setOrderSn(j.getString(key2sn));
        gamer.setIcon(j.getString(cbgReturnKey.getIcon()));
        gamer.setHighText(Arrays.toString(j.getJSONArray(key2highLight).toArray()));
        gamer.setPrice(j.getIntValue(key2price));
        gamer.setDianCangCount(j.getJSONObject(key2otherInfo).getIntValue(key2dianCang));
        gamer.setDianJiCount(j.getJSONObject(key2otherInfo).getIntValue(key2dianJi));
        gamer.setServerId(j.getIntValue(key2serverId));
        gamer.setTitle(Arrays.toString(j.getJSONObject(key2otherInfo).getJSONArray(key2basicAttr).toArray()));
        gamer.setFiveStarCount(Integer.parseInt(j.getJSONObject(key2otherInfo).getJSONArray(key2basicAttr).get(0).toString().replaceAll("[^0-9*]", "")));
        gamer.setSkillCount(Integer.parseInt(j.getJSONObject(key2otherInfo).getJSONArray(key2basicAttr).get(1).toString().replaceAll("[^0-9*]", "")));
        gamer.setHasDetail(false);
    }

    /**
     * 再次向 cbg 发起请求, 获取角色详细信息
     *
     * @param gamer
     * @throws CbgException
     */
    public void queryAndSetGamerDetailInfo(Gamer gamer) throws CbgException {
        log.trace("[SR]queryAndSetGamerDetail:" + gamer.getId());

        JSONObject equip = this.queryGamerDetail(gamer);

        this.generateAndSetGamerDetail(gamer, equip);
        log.trace("[SR]queryAndSetGamerDetail over:" + gamer);

    }

    public JSONObject queryGamerDetail(String gamerOrderSn) throws CbgException {
        return this.queryGamerDetail(Gamer.builder().orderSn(gamerOrderSn).serverId(1).build());
    }

    private JSONObject queryGamerDetail(Gamer gamer) throws CbgException {
        JSONObject param = this.generateDetailRequestParam(gamer);
        String retStr = HttpUtil.doPostWithFormData(this.cbgQueryDetailUrl, param);

        JSONObject r = JSON.parseObject(retStr);

        int status = r.getIntValue(cbgReturnKey.getStatus());
        if (status != 1) {
            String msg = r.getString(cbgReturnKey.getMsg());
            log.error("查询藏宝阁失败,status:" + status + ", msg:" + msg);
            throw new CbgException(status, msg, this.cbgQueryDetailUrl, param);
        }

        //角色详情
        return r.getJSONObject(cbgReturnKey.getDetailGamerKey());
    }

    public void generateAndSetGamerDetail(Gamer gamer, JSONObject equip) {
        gamer.setName(equip.getString(cbgReturnKey.getGamerNameKey()));
        gamer.setPrice(equip.getIntValue(key2price));
        gamer.setFirstPrice(equip.getIntValue(cbgReturnKey.getFirstPriceKey()));
        gamer.setSellStatus(equip.getIntValue(cbgReturnKey.getSellStatusKey()));
        gamer.setSellStatusDesc(equip.getString(cbgReturnKey.getSellStatusNameKey()));

        //角色拥有的 英雄卡, 技能 数据对象
        JSONObject equipInfo = equip.getJSONObject(cbgReturnKey.getDetailGamerInfoKey());
        gamer.setSkillList(equipInfo.getJSONArray(cbgReturnKey.getDetailSkillKey()));

        Set<Integer> skillIds = new LinkedHashSet<>(gamer.getSkillList().size());
        gamer.getSkillList().forEach(o -> {
            int skillId = JSON.parseObject(o.toString()).getIntValue(cbgReturnKey.getDetailSkillIdKey());
            skillIds.add(skillId);
        });
        gamer.setSkillIds(skillIds);


        List<Gamer.GamerHero> l = equipInfo.getJSONArray(cbgReturnKey.getDetailCardKey()).toJavaList(Gamer.GamerHero.class);
        gamer.setGamerHeroes(l);

        Map<Integer, Integer> heroIdIdxMap = new HashMap<>(gamer.getGamerHeroes().size());
        for (int i = 0; i < l.size(); i++) {

            heroIdIdxMap.put(l.get(i).getHeroId(), i);

        }
        gamer.setHeroIdIdxMap(heroIdIdxMap);

        gamer.setDianJiList(equipInfo.getJSONArray(cbgReturnKey.getDetailDianJiKey()));
        gamer.setDianCangList(equipInfo.getJSONArray(cbgReturnKey.getDetailDianCangKey()));
        gamer.setCardFeatureList(equipInfo.getJSONArray(cbgReturnKey.getDetailCardFeatureKey()));
        gamer.setTenure(equipInfo.getJSONObject(cbgReturnKey.getDetailTenureKey()));

        gamer.setHasDetail(true);
    }

    private JSONObject generateDetailRequestParam(Gamer gamer) {
        JSONObject param = new JSONObject(3);
        param.put("serverid", String.valueOf(gamer.getServerId()));
        param.put("ordersn", gamer.getOrderSn());
        param.put("view_loc", "equip_list");
        return param;
    }

}
