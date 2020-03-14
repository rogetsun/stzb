package com.uv.cbg;

import com.uv.bean.SearchFilterAndResult;
import com.uv.config.CbgReturnKey;
import com.uv.config.DingConf;
import com.uv.config.QueryConfig;
import com.uv.db.mongo.entity.Gamer;
import com.uv.db.mongo.entity.Notice;
import com.uv.db.mongo.entity.SearchFilter;
import com.uv.db.mongo.entity.SearchResult;
import com.uv.db.mongo.repository.GamerRepository;
import com.uv.db.mongo.repository.NoticeRepository;
import com.uv.db.mongo.repository.SearchResultRepository;
import com.uv.db.mongo.service.MongoService;
import com.uv.exception.CbgException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Resource(name = "mongoService")
    private MongoService service;
    @Resource
    private NoticeRepository noticeRepository;
    @Resource
    private SearchResultRepository searchResultRepository;
    @Resource
    private GamerRepository gamerRepository;

    @Resource
    private Searcher searcher;

    private Class<? extends Throwable> throwableClass;


    public void init() {
        this.initQuery();
        this.delAllGamer();
    }

    /**
     * 按配置文件初始化搜索条件到mongo
     */
    public void initQuery() {
        service.delAllSearchFilter();
        searchResultRepository.deleteAll();
        this.saveQueryFromConfig();
    }

    /**
     * 核心查找方法
     */
    public void find() {

        this.setExecTimestamp(System.currentTimeMillis());
        log.debug("\n");
        log.debug("[CBG]begin to find:" + new Date((this.execTimestamp)));

        List<SearchFilter> filters = this.getAllSearchFilter();

        log.debug("[FD]found [" + filters.size() + "] SearchFilter|QueryConfig");

        filters.forEach(searchFilter -> log.debug(searchFilter.toString()));

        for (SearchFilter filter : filters) {

            try {

                //根据SearchFilter基础筛选信息初步查询角色
                List<Gamer> gamers = searcher.execNormalSearch(filter);
                log.info("[FD]FOUND [" + gamers.size() + "] BY " + filter.toString());

                //补充角色信息,分析角色和SearchFilter详细过滤,并生成通知到通知表
                SearchResult result = this.dealGamers(filter, gamers);
                log.info("[FD]ACTION [" + result.getSimpleGamerMap().size() + "]");

            } catch (Throwable e) {
                log.error("根据filter[" + filter.getId() + "]搜索分析gamer失败," + filter, e);
                if (this.throwableClass == null || !this.throwableClass.equals(e.getClass())) {
                    noticeRepository.save(
                            Notice.builder()
                                    .id("ERROR" + new Date(this.execTimestamp))
                                    .title("根据filter[" + filter.getId() + "]搜索分析gamer失败")
                                    .content(e.getLocalizedMessage())
                                    .build()
                    );
                    this.throwableClass = e.getClass();
                }

            }
        }

        log.debug("[CBG]end to find.running [" + ((System.currentTimeMillis() - this.getExecTimestamp()) / 1000) + "s]");

    }


    /**
     * 根据查询到的帐号开始处理分析
     *
     * @param filter
     * @param gamers
     */
    private SearchResult dealGamers(SearchFilter filter, List<Gamer> gamers) throws UnsupportedEncodingException, CbgException {

        //搜索条件和结果封装打包
        SearchFilterAndResult filterAndResult = this.generateSearchFilterAndResult(filter);
        SearchResult result = filterAndResult.getSearchResult();
        int okCount = 0;
        for (Gamer gamer : gamers) {

            log.trace("\n");
            log.trace("[FD]dealGamers:" + gamer.getPrintInfo());

            //更新基础价格,补充角色信息
            Gamer tmpGamer = this.doGamer(gamer);
            //分析角色
            log.trace("[FD]will analyzeGamer:" + tmpGamer.getPrintInfo());

            SearchResult.SimpleGamer simpleGamer = this.analyzeGamer(filterAndResult, tmpGamer);
            log.trace("[FD]analyzeGamer END:isAction:" + result.isActionedGamer(tmpGamer) + ", simpleGamer:" + (simpleGamer == null ? "dropped" : simpleGamer.toString()));

            if (simpleGamer != null && result.isActionedGamer(tmpGamer)) {
                okCount++;
                if (simpleGamer.getUpdateTime().getTime() == this.execTimestamp) {
                    sendNotice(filter, tmpGamer, simpleGamer);
                }
            }
        }

        log.trace("[FD]searchResult:" + result);

        if (result.getUpdateTime().getTime() == this.getExecTimestamp()) {
            log.trace("[FD]SAVE searchResult");
            result.refreshActionGamerIds();
            searchResultRepository.save(result);
        }

        log.info("[FD]searchResult: 匹配 [" + okCount + "]");

        return result;
    }

    private void sendNotice(SearchFilter filter, Gamer gamer, SearchResult.SimpleGamer simpleGamer) {
        String titleKey = null;
        if (simpleGamer.getLastPrice() == 0) {
            titleKey = "[新]";
        } else if (simpleGamer.getLastPrice() < simpleGamer.getPrice()) {
            titleKey = "[涨]";
        } else if (simpleGamer.getLastPrice() > simpleGamer.getPrice()) {
            titleKey = "[降]";
        }
        if (titleKey != null) {
            Notice notice = Notice.builder()
                    .id(filter.getId() + "-" + gamer.getId())
                    .dingUrl(filter.getDingUrl())
                    .dingSecret(filter.getDingSecret())
                    .hasNotify(false)
                    .createTime(new Date(this.execTimestamp))
                    .title(titleKey + "[" + gamer.getName() + "]" + (simpleGamer.getPrice() / 100))
                    .content(("[新]".equals(titleKey) ? "" : ("LP:" + simpleGamer.getLastPrice() / 100 + ", ")) + this.generateNoticeContent(gamer))
                    .url(this.generateWebUrl(gamer))
                    .icon(this.generateIconUrl(gamer))
                    .build();
            log.trace("[NOTICE]new:" + notice.toString());
            noticeRepository.save(notice);
        }
    }

    /**
     * 处理单个角色: 更新价格, 获取详细信息; 并保存入库
     *
     * @param gamer
     * @return
     * @throws UnsupportedEncodingException
     * @throws CbgException
     */
    private Gamer doGamer(Gamer gamer) throws UnsupportedEncodingException, CbgException {
        log.trace("[FD]doGamer BEGIN:" + gamer.getPrintInfo());
        Gamer tmpGamer = gamerRepository.findById(gamer.getId()).orElse(null);

        if (tmpGamer != null) {
            if (tmpGamer.getPrice() != gamer.getPrice()) {
                log.info((tmpGamer.getPrice() > gamer.getPrice() ? "[下降]" : "[上涨]") + gamer.getPrintInfo());

                tmpGamer.setPrice(gamer.getPrice());
                tmpGamer.setUpdateTime(new Date(this.execTimestamp));
                tmpGamer.setChangeCount(tmpGamer.getChangeCount() + 1);

            }
        } else {
            gamer.setUpdateTime(new Date(this.execTimestamp));
            gamer.setCreateTime(new Date(this.execTimestamp));
            gamer.setChangeCount(0);
            tmpGamer = gamer;
        }

        if (!tmpGamer.isHasDetail() || tmpGamer.getSkillList() == null || tmpGamer.getSkillList().size() == 0) {
            searcher.queryAndSetGamerDetailInfo(tmpGamer);
            tmpGamer.setUpdateTime(new Date(this.execTimestamp));
        }

        if (tmpGamer.getDealTime() == null || tmpGamer.getDealTime().getTime() < this.execTimestamp) {
            tmpGamer.setDealTime(new Date(this.execTimestamp));
            service.saveGamer(tmpGamer);
        }
        log.trace("[FD]doGamer END:" + tmpGamer.getPrintInfo());
        return tmpGamer;
    }

    /**
     * 将搜索条件和结果打包,如果没有生成过结果则新生成
     *
     * @param filter
     * @return
     */
    private SearchFilterAndResult generateSearchFilterAndResult(SearchFilter filter) {
        log.trace("[FD]generateSearchFilterAndResult:BEGIN");

        SearchResult result = null;

        List<SearchResult> resultList = searchResultRepository.findAllBySearchFilterId(filter.getId());

        if (resultList != null && resultList.size() > 0) {
            result = resultList.get(0);
            log.trace("[FD]generateSearchFilterAndResult:searchResultRepository found " + result);
        }
        if (null == result) {
            result = SearchResult.builder()
                    .searchFilterId(filter.getId()).id(filter.getId())
                    .updateTime(new Date(this.execTimestamp))
                    .build();
            log.trace("[FD]generateSearchFilterAndResult:build new " + result);

        }
        log.trace("[FD]generateSearchFilterAndResult:END");
        return SearchFilterAndResult.builder()
                .searchFilter(filter)
                .searchResult(result)
                .build();

    }


    /**
     * 分析 gamer 是否符合 详细 过滤条件,符合或者gamer 价格变动 返回发送的通知
     *
     * @param filterAndResult
     * @param gamer
     * @return
     */
    private SearchResult.SimpleGamer analyzeGamer(SearchFilterAndResult filterAndResult, Gamer gamer) {
        Notice notice = null;

        SearchFilter filter = filterAndResult.getSearchFilter();
        SearchResult result = filterAndResult.getSearchResult();

        SearchResult.SimpleGamer simpleGamer = result.getActionSimpleGamer(gamer);

        if (simpleGamer == null) {
            simpleGamer = SearchResult.generateSimpleGamer(gamer, this.execTimestamp);
        }

        if (simpleGamer.getCreateTime().getTime() == this.execTimestamp
                || simpleGamer.getUpdateTime().getTime() < filter.getUpdateTime().getTime()) {
            //todo 执行分析
            log.trace("[FD]exec analyze:" + gamer.getPrintInfo());
            /**
             * 判断必选技能是否满足
             */
            if (filter.getContainsSkill() != null && filter.getContainsSkill().size() > 0) {
                if (!gamer.getSkillIds().containsAll(filter.getContainsSkill())) {
                    log.trace("[FD]skill not enough, continue next!" + gamer.getPrintInfo());
                    result.unActionGamer(gamer, this.execTimestamp);
                    return null;
                }
            }
            /**
             * 计算契合度
             */
            if (filter.getOptionHero() != null) {
                Set<Integer> optionHero = new HashSet<>(filter.getOptionHero());
                optionHero.retainAll(gamer.getHeroIds());
                simpleGamer.setHeroFitDegree(optionHero.size() * 100 / filter.getOptionHero().size());
            } else {
                simpleGamer.setHeroFitDegree(100);
            }
            if (simpleGamer.getHeroFitDegree() < filter.getOptionHeroMinFitDegree()) {
                log.trace("[FD]Option Hero not enough, continue next!" + gamer.getPrintInfo());
                result.unActionGamer(gamer, this.execTimestamp);
                return null;
            }

            if (filter.getOptionSkill() != null) {
                Set<Integer> optionSkill = new HashSet<>(filter.getOptionSkill());
                optionSkill.retainAll(gamer.getSkillIds());
                simpleGamer.setSkillFitDegree(optionSkill.size() * 100 / filter.getOptionSkill().size());
            } else {
                simpleGamer.setSkillFitDegree(100);
            }

            if (simpleGamer.getSkillFitDegree() < filter.getOptionSkillMinFitDegree()) {
                log.trace("[FD]Option Skill not enough, continue next!" + gamer.getPrintInfo());
                result.unActionGamer(gamer, this.execTimestamp);
                return null;
            }
            simpleGamer.setUpdateTime(new Date(this.execTimestamp));
            result.actionGamer(simpleGamer, this.execTimestamp);
            log.trace("[FD]analyze OK:" + simpleGamer);
        } else {
            log.trace("[FD]compare price:" + gamer.getPrintInfo());

            if (simpleGamer.getPrice() != gamer.getPrice()) {
                log.trace("[FD]changed price:" + gamer.getPrintInfo());

                simpleGamer.setLastPrice(simpleGamer.getPrice());
                simpleGamer.setPrice(gamer.getPrice());
                simpleGamer.setUpdateTime(new Date(this.execTimestamp));

                result.setUpdateTime(new Date(this.execTimestamp));

            }

        }

        return simpleGamer;
    }

    public String generateNoticeContent(Gamer gamer) {
        StringBuilder sb = new StringBuilder();
        if (gamer.getFirstPrice() != 0) {
            sb.append("FP:").append(gamer.getFirstPrice() / 100).append(",");
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

    public void saveQueryFromConfig() {
        SearchFilter sf = SearchFilter.builder()
                .id(queryConfig.getId() == 0 ? 1 : queryConfig.getId())
                .name("default")
                .minPrice(queryConfig.getMinPrice())
                .maxPrice(queryConfig.getMaxPrice())
                .containsHero(queryConfig.getHero())
                .containsSkill(queryConfig.getSkill())
                .optionHero(queryConfig.getOptionHero())
                .optionSkill(queryConfig.getOptionSkill())
                .optionHeroMinFitDegree(queryConfig.getOptionHeroMinFitDegree())
                .optionSkillMinFitDegree(queryConfig.getOptionSkillMinFitDegree())
                .dingSecret(dingConf.getSecret())
                .dingUrl(dingConf.getUrl())
                .updateTime(new Date())
                .build();
        log.debug("save queryConfig:" + sf.toString());
        service.saveSearchFilter(sf);
    }

    public void delAllGamer() {
        service.delAllGamer();
    }

}
