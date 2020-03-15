package com.uv.cbg;

import com.uv.bean.SearchFilterAndResult;
import com.uv.config.DingConf;
import com.uv.config.QueryConfig;
import com.uv.db.mongo.entity.Gamer;
import com.uv.db.mongo.entity.SearchFilter;
import com.uv.db.mongo.entity.SearchResult;
import com.uv.db.mongo.repository.GamerRepository;
import com.uv.db.mongo.repository.SearchResultRepository;
import com.uv.db.mongo.service.MongoService;
import com.uv.exception.CbgException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
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
    @Resource(name = "mongoService")
    private MongoService service;

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
        log.info("\n");
        log.info("[CBG]begin to find:" + new Date((this.execTimestamp)));

        List<SearchFilter> filters = this.getAllSearchFilter();

        log.info("[FD]found [" + filters.size() + "] SearchFilter|QueryConfig");

        filters.forEach(searchFilter -> log.debug(searchFilter.toString()));

        List<ListenableFuture<SearchFilterAndResult>> futures = new ArrayList<>(filters.size());

        for (SearchFilter filter : filters) {
            ListenableFuture<SearchFilterAndResult> future = dealSearchFilter(filter);
            futures.add(future);
        }
        for (ListenableFuture<SearchFilterAndResult> future : futures) {
            try {
                future.get();
            } catch (Throwable e) {
                log.error("获取filter执行结果失败," + future, e);
                try {
                    service.sendExceptionNotice("获取filter执行结果失败", e, this.execTimestamp);
                } catch (Throwable ex) {
                    log.error("失败又失败,", ex);
                }
            }
        }
        log.info("[CBG]end to find.running [" + ((System.currentTimeMillis() - this.getExecTimestamp()) / 1000) + "s]");

    }

    @Async("taskExecutor")
    public ListenableFuture<SearchFilterAndResult> dealSearchFilter(SearchFilter filter) {
        SearchFilterAndResult searchFilterAndResult = null;
        try {
            log.info("[FD]DEAL " + filter.toString());
            //根据SearchFilter基础筛选信息初步查询角色
            List<Gamer> gamers = searcher.execNormalSearch(filter);
            log.info("[FD]FOUND [" + gamers.size() + "]");

            //补充角色信息,分析角色和SearchFilter详细过滤,并生成通知到通知表
            searchFilterAndResult = this.dealGamers(filter, gamers);
            log.info("[FD]ACTION [" + searchFilterAndResult.getSearchResult().getSimpleGamerMap().size() + "]");

        } catch (Throwable e) {
            log.error("根据filter[" + filter.getId() + "]搜索分析gamer失败," + filter, e);
            if (this.throwableClass == null || !this.throwableClass.equals(e.getClass())) {
                service.sendExceptionNotice("根据filter[" + filter.getId() + "]搜索分析gamer失败", e, this.execTimestamp);
                this.throwableClass = e.getClass();
            }

        }

        return new AsyncResult<>(searchFilterAndResult);
    }

    /**
     * 根据查询到的帐号开始处理分析
     *
     * @param filter
     * @param gamers
     */
    private SearchFilterAndResult dealGamers(SearchFilter filter, List<Gamer> gamers) throws UnsupportedEncodingException, CbgException {

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
                    service.sendPriceChangedNotice(filter, tmpGamer, simpleGamer, this.execTimestamp);
                }
            }
        }

        log.debug("[FD]searchResult:" + result);

        if (result.getUpdateTime().getTime() == this.getExecTimestamp()) {
            log.trace("[FD]SAVE searchResult");
            service.saveSearchResult(result);
        }

        log.info("[FD]searchResult: 匹配 [" + okCount + "]");

        return filterAndResult;
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
