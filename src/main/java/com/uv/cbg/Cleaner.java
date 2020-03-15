package com.uv.cbg;

import com.uv.db.mongo.entity.Gamer;
import com.uv.db.mongo.entity.Notice;
import com.uv.db.mongo.entity.SearchFilter;
import com.uv.db.mongo.entity.SearchResult;
import com.uv.db.mongo.repository.GamerRepository;
import com.uv.db.mongo.repository.NoticeRepository;
import com.uv.db.mongo.repository.SearchFilterRepository;
import com.uv.db.mongo.repository.SearchResultRepository;
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
            log.debug("[CR]will clear " + l.size() + " gamers");
            for (Gamer gamer : l) {
                try {

                    log.trace("[CR]deal:status:" + gamer.getSellStatus() + ", " + gamer.getPrintInfo());
                    int oldSellStatus = gamer.getSellStatus();
                    searcher.queryAndSetGamerDetailInfo(gamer);

                    // todo 测试代码
//                    {
//                    gamer.setSellStatus(6);
//                    gamer.setSellStatusDesc("已售出");
//                    }

                    if (gamer.getSellStatus() != oldSellStatus) {
                        log.debug("[CR]CLEAR:" + gamer.getSellStatus() + ", " + gamer.getPrintInfo());

                        List<SearchResult> results = searchResultRepository.findAllByActionGamerIdsContains(gamer.getId());

                        for (SearchResult result : results) {
                            log.debug("[CR]NOTICE:" + result.toString());
                            noticeRepository.save(this.generateNotice(result, gamer));
                            if (gamer.getSellStatus() == 6 || gamer.getSellStatus() == 0) {
                                result.unActionGamer(gamer, this.dealTimestamp);
                                mongoService.saveSearchResult(result);
                            }
                        }
                        if (gamer.getSellStatus() == 6 || gamer.getSellStatus() == 0) {
                            gamerRepository.delete(gamer);
                        }
                        log.info("[CR]DELETE" + gamer.toString());

                    } else {
                        gamer.setDealTime(new Date(this.dealTimestamp));
                        gamerRepository.save(gamer);
                    }
                } catch (Exception e) {
                    log.debug("获取准备清理的角色最新详细信息失败," + gamer, e);
                }

            }

        }
        return l;
    }

    private Notice generateNotice(SearchResult result, Gamer gamer) {
        SearchFilter filter = searchFilterRepository.findById(result.getSearchFilterId()).orElse(null);
        if (filter != null) {
            return Notice.builder()
                    .id(filter.getId() + "-" + gamer.getId())
                    .dingUrl(filter.getDingUrl())
                    .dingSecret(filter.getDingSecret())
                    .hasNotify(false)
                    .createTime(new Date(this.dealTimestamp))
                    .title("[" + gamer.getSellStatus() + "][" + gamer.getSellStatusDesc() + "][" + gamer.getName() + "]" + (gamer.getPrice() / 100))
                    .content(finder.generateNoticeContent(gamer, result.getActionSimpleGamer(gamer)))
                    .url(finder.generateWebUrl(gamer))
                    .icon(finder.generateIconUrl(gamer))
                    .build();
        }
        return null;

    }
}
