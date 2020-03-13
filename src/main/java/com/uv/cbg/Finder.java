package com.uv.cbg;

import com.uv.config.CbgReturnKey;
import com.uv.config.DingConf;
import com.uv.config.QueryConfig;
import com.uv.db.mongo.entity.Gamer;
import com.uv.db.mongo.entity.Notice;
import com.uv.db.mongo.entity.SearchFilter;
import com.uv.db.mongo.repository.NoticeRepository;
import com.uv.db.mongo.service.MongoService;
import com.uv.db.redis.RedisUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author uvsun 2020/3/8 3:25 下午
 * 根据SearchFilter初步查询筛选角色, 并调用后续 Searcher 获取详细信息
 */
@Component
@Slf4j
@Data
public class Finder {

    private long execTimestamp;

    @Resource
    private QueryConfig queryConfig;

    @Resource
    private DingConf dingConf;

    @Value("${cbg.webDetailUrl}")
    private String cbgWebUrl;
    @Value("${cbg.gamerResUrl}")
    private String gamerResUrl;
    @Resource
    private CbgReturnKey cbgReturnKey;

    @Resource
    private RedisUtil redisUtil;

    @Resource(name = "mongoService")
    private MongoService service;
    @Resource
    private NoticeRepository noticeRepository;

    @Resource
    private Searcher searcher;


    public void init() {
        this.initQuery();
        this.delAllGamer();
    }

    /**
     * 按配置文件初始化搜索条件到mongo
     */
    public void initQuery() {
        service.delAllSearchFilter();
        SearchFilter sf = SearchFilter.builder()
                .containsHero(queryConfig.getHero())
                .containsSkill(queryConfig.getSkill())
                .id(1)
                .minPrice(queryConfig.getMinPrice())
                .maxPrice(queryConfig.getMaxPrice())
                .dingSecret(dingConf.getSecret())
                .dingUrl(dingConf.getUrl())
                .name("default")
                .updateTime(new Date())
                .build();
        log.debug("init queryConfig:" + sf.toString());
        service.saveSearchFilter(sf);
    }

    public void delAllGamer() {
        service.delAllGamer();
    }

    public void find() {

        log.debug("[CBG]begin to find.");

        List<SearchFilter> filters = this.getAllSearchFilter();

        long startTimestamp = System.currentTimeMillis();
        log.debug("[FD]found [" + filters.size() + "] SearchFilter|QueryConfig");

        filters.forEach(searchFilter -> log.debug(searchFilter.toString()));

        for (SearchFilter filter : filters) {

            List<Gamer> gamers = searcher.execNormalSearch(filter);
            log.info("[FD]FOUND [" + gamers.size() + "] BY " + filter.toString());

            this.dealGamers(filter, gamers, startTimestamp);

            if (filter.getUpdateTime().getTime() == startTimestamp) {
                service.saveSearchFilter(filter);
            }
        }
        this.setExecTimestamp(startTimestamp);

        log.debug("[CBG]end to find.running [" + ((System.currentTimeMillis() - startTimestamp) / 1000) + "s]");

    }

    /**
     * 根据查询到的帐号开始处理分析
     *
     * @param filter
     * @param gamers
     * @param dealTime
     */
    private void dealGamers(SearchFilter filter, List<Gamer> gamers, long dealTime) {
        for (Gamer gamer : gamers) {

            try {
                log.trace(gamer.getId() + ":" + gamer.toString());

                Gamer tmpGamer = service.findGamerById(gamer.getId());

                if (tmpGamer != null) {
                    if (tmpGamer.getPrice() != gamer.getPrice()) {
                        log.info((tmpGamer.getPrice() > gamer.getPrice() ? "[下降]" : "[上涨]") + gamer.toString());

                        tmpGamer.setPrice(gamer.getPrice());
                        tmpGamer.setUpdateTime(new Date(dealTime));
                        tmpGamer.setChangeCount(tmpGamer.getChangeCount() + 1);

                    }
                } else {
                    gamer.setUpdateTime(new Date(dealTime));
                    gamer.setCreateTime(new Date(dealTime));
                    gamer.setChangeCount(0);
                    tmpGamer = gamer;
                }

                if (tmpGamer.getSkillList() == null || tmpGamer.getSkillList().size() == 0) {
                    searcher.queryAndSetGamerDetailInfo(tmpGamer);
                    log.trace("[FD]queryAndSetGamerDetailInfo:" + tmpGamer.getId());
                    tmpGamer.setUpdateTime(new Date(dealTime));
                }

                if (tmpGamer.getUpdateTime().getTime() == dealTime) {
                    service.saveGamer(tmpGamer);
                }

                if (filter.getSimpleGamerMap() == null) {
                    Map<String, SearchFilter.SimpleGamer> map = new HashMap<>();
                    filter.setSimpleGamerMap(map);
                }
                if (filter.getActionGamerIds() == null) {
                    filter.setActionGamerIds(new ArrayList<>());
                }

                Map<String, SearchFilter.SimpleGamer> m = filter.getSimpleGamerMap();

                Notice notice = null;
                if (m.containsKey(tmpGamer.getId())) {
                    log.trace("[FD]SearchFilter has Gamer:" + tmpGamer.getId());
                    SearchFilter.SimpleGamer sg = m.get(gamer.getId());
                    if (sg.getPrice() != tmpGamer.getPrice()) {

                        sg.setLastPrice(sg.getPrice());
                        sg.setPrice(tmpGamer.getPrice());
                        sg.setUpdateTime(new Date(dealTime));

                        filter.setUpdateTime(new Date(dealTime));

                        notice = Notice.builder()
                                .id(filter.getId() + "-" + tmpGamer.getId())
                                .dingUrl(filter.getDingUrl()).dingSecret(filter.getDingSecret()).hasNotify(false).createTime(new Date(dealTime))
                                .title("[" + (sg.getLastPrice() > sg.getPrice() ? "降" : "涨") + "][" + tmpGamer.getName() + "]" + (sg.getPrice() / 100))
                                .content("LP:￥" + sg.getLastPrice() / 100 + ", " + this.generateNoticeContent(tmpGamer))
                                .url(this.generateWebUrl(tmpGamer))
                                .icon(this.generateIconUrl(tmpGamer))
                                .build();
                    }
                } else {

                    if (filter.getContainsSkill() != null && filter.getContainsSkill().size() > 0) {
                        if (!tmpGamer.getSkillIdList().containsAll(filter.getContainsSkill())) {
                            log.debug("[FD]skill not enough, continue!");
                            continue;
                        }
                    }

                    m.put(
                            tmpGamer.getId(),
                            SearchFilter.SimpleGamer.builder().lastPrice(0).price(tmpGamer.getPrice()).sn(gamer.getId())
                                    .createTime(new Date(dealTime)).updateTime(new Date(dealTime))
                                    .build()
                    );

                    filter.getActionGamerIds().add(tmpGamer.getId());
                    filter.setUpdateTime(new Date(dealTime));

                    notice = Notice.builder()
                            .id(filter.getId() + "-" + tmpGamer.getId())
                            .dingUrl(filter.getDingUrl())
                            .dingSecret(filter.getDingSecret())
                            .hasNotify(false)
                            .createTime(new Date(dealTime))
                            .title("[新][" + tmpGamer.getName() + "]" + (tmpGamer.getPrice() / 100))
                            .content(this.generateNoticeContent(gamer))
                            .url(this.generateWebUrl(tmpGamer))
                            .icon(this.generateIconUrl(tmpGamer))
                            .build();

                }
                if (null != notice) {
                    log.trace("[NOTICE]new:" + notice.toString());
                    noticeRepository.save(notice);
                }
            } catch (Throwable e) {
                log.error("[FD]处理Gamer失败," + gamer.toString(), e);
            }
        }
    }

    public String generateNoticeContent(Gamer gamer) {
        StringBuilder sb = new StringBuilder();
        if (gamer.getFirstPrice() != 0) {
            sb.append("FP:￥").append(gamer.getFirstPrice() / 100).append(",");
        }

        sb.append(gamer.getHighText())
                .append("5:").append(gamer.getFiveStarCount())
                .append(",SK:").append(gamer.getSkillCount())
                .append(",Y:").append(gamer.getTenure().getIntValue(cbgReturnKey.getDetailTenureYuanBaoKey()))
                .append(",DJ:").append(gamer.getDianJiCount())
                .append(",DC:").append(gamer.getDianCangCount())
        ;

        return sb.toString();
    }

    public String generateIconUrl(Gamer g) {
        return this.gamerResUrl + g.getIcon();
    }

    public String generateWebUrl(Gamer gamer) {
        return this.cbgWebUrl + "/" + gamer.getServerId() + "/" + gamer.getOrderSn() + "?view_loc=equip_list";
    }

    private List<SearchFilter> getAllSearchFilter() {
        return service.getAllSearchFilter();
    }

}
