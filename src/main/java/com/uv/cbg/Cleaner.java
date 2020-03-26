package com.uv.cbg;

import com.uv.db.mongo.entity.Gamer;
import com.uv.db.mongo.entity.GamerHis;
import com.uv.db.mongo.entity.SearchFilter;
import com.uv.db.mongo.entity.SearchResult;
import com.uv.db.mongo.repository.*;
import com.uv.db.mongo.service.MongoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author uvsun 2020/3/13 2:24 下午
 */
@Component
@Slf4j
public class Cleaner {

    @Resource
    private GamerRepository gamerRepository;
    @Resource
    private GamerHisRepository gamerHisRepository;
    @Resource
    private Finder finder;
    @Resource
    private Searcher searcher;
    @Resource
    private SearchFilterRepository searchFilterRepository;
    @Resource
    private SearchResultRepository searchResultRepository;
    @Resource
    private MongoService mongoService;
    @Resource
    private NoticeRepository noticeRepository;
    private long dealTimestamp;
    private Class<? extends Throwable> throwableClass;

    public List<Gamer> clear() {
        this.dealTimestamp = System.currentTimeMillis();
        List<Gamer> l;
        // todo 测试代码
//        {
//            Gamer g = gamerRepository.findById("201909291202116-1-SQHI1KYDLFBSXU").orElse(null);
//            assert g != null;
//            finder.setExecTimestamp(g.getUpdateTime().getTime());
//            g.setUpdateTime(new Date(g.getUpdateTime().getTime() - 1000));
//            gamerRepository.save(g);
//        }

        l = gamerRepository.findAllByDealTimeBefore(new Date(finder.getExecTimestamp()));

        if (null != l && l.size() > 0) {
            log.debug("[CR]will pre clear " + l.size() + " gamers");
            for (Gamer gamer : l) {
                try {

                    log.trace("[CR]deal:oldStatus:" + gamer.getSellStatus() + ", " + gamer.getPrintInfo());
                    int oldSellStatus = gamer.getSellStatus();
                    searcher.queryAndSetGamerDetailInfo(gamer);

                    // todo 测试代码
//                    {
//                    gamer.setSellStatus(6);
//                    gamer.setSellStatusDesc("已售出");
//                    }

                    log.trace("[CR]CHANGE:oldStatus:" + oldSellStatus + ", newStatus:" + gamer.getSellStatus() + ", " + gamer.getPrintInfo());
                    if (gamer.getSellStatus() != oldSellStatus) {

                        List<SearchResult> results = searchResultRepository.findAllByActionGamerIdsContains(gamer.getId());

                        for (SearchResult result : results) {

                            log.debug("[CR]NOTICE: " + result.toString());
                            SearchFilter filter = searchFilterRepository.findById(result.getSearchFilterId()).orElse(null);
                            SearchResult.SimpleGamer simpleGamer = result.getActionSimpleGamer(gamer);
                            mongoService.sendStatusChangedNotice(filter, gamer, simpleGamer, this.dealTimestamp);

                            if (gamer.getSellStatus() == 6 || gamer.getSellStatus() == 0) {
                                log.debug("[CR]UnAction: " + gamer.getPrintInfo());
                                result.unActionGamer(gamer, this.dealTimestamp);
                                mongoService.saveSearchResult(result);
                            }
                        }
                        if (gamer.getSellStatus() == 6 || gamer.getSellStatus() == 0) {
                            gamer.setDealTime(new Date((this.dealTimestamp)));
                            log.info("[CR]MV to His: " + gamer.getPrintInfo());
                            GamerHis gamerHis = GamerHis.buildFromGamer(gamer);
                            gamerHisRepository.save(gamerHis);
                            gamerRepository.delete(gamer);
                        } else {
                            gamer.setDealTime(new Date(this.dealTimestamp));
                            log.info("[CR]UPDATE status: " + gamer.getSellStatus() + ", " + gamer.getPrintInfo());
                            gamerRepository.save(gamer);
                        }

                    } else {
                        gamer.setDealTime(new Date(this.dealTimestamp));
                        log.info("[CR]UPDATE dealTime: " + gamer.getDealTime() + ", " + gamer.getPrintInfo());
                        gamerRepository.save(gamer);
                    }
                } catch (Throwable e) {
                    log.error("清理的角色最新详细信息失败," + gamer, e);
                    mongoService.sendExceptionNotice("[CR]" + gamer.getPrintInfo(), gamer.getPrintInfo().substring(0, 60), this.dealTimestamp);

                }

            }

        }
        return l;
    }


}
