package com.uv.controller;

import com.alibaba.fastjson.JSONObject;
import com.uv.cbg.Finder;
import com.uv.cbg.Searcher;
import com.uv.db.mongo.entity.Gamer;
import com.uv.db.mongo.entity.Notice;
import com.uv.db.mongo.entity.SearchFilter;
import com.uv.db.mongo.entity.SearchResult;
import com.uv.db.mongo.repository.SearchFilterRepository;
import com.uv.db.mongo.service.MongoService;
import com.uv.exception.CbgException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * @author uvsun 2020/3/24 12:43 上午
 */
@Controller
@Slf4j
public class CbgController {

    @Resource
    private Searcher searcher;
    @Resource
    private Finder finder;
    @Resource
    private SearchFilterRepository searchFilterRepository;
    @Resource
    private MongoService mongoService;

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/gamer-compute/{searchFilterId}/{orderSn}")
    @ResponseBody
    public JSONObject computeStatis(@PathVariable int searchFilterId, @PathVariable String orderSn) {
        log.debug("searchFilterId:" + searchFilterId + ", orderSn:" + orderSn);
        JSONObject j = new JSONObject();

        try {
            JSONObject equip = searcher.queryGamerDetail(orderSn);
//            log.debug(JSON.toJSONString(equip, true));
            Gamer g = searcher.generateGamer(equip);
            log.debug(g.toString());
            searcher.generateAndSetGamerDetail(g, equip);
            SearchFilter filter = searchFilterRepository.findById(searchFilterId).orElse(null);
            if (filter != null) {
                SearchResult.SimpleGamer simpleGamer = SearchResult.generateSimpleGamer(g, System.currentTimeMillis());
                finder.execAnalysis(g, filter, simpleGamer);
                j.put("simpleGamer", simpleGamer);
                Notice notice = mongoService.generatePriceNotice(filter, g, simpleGamer, System.currentTimeMillis());
                j.put("notice", notice);
            }
//            j.put("gamer", g);
        } catch (CbgException e) {
            log.error("queryGamerDetail error,", e);
        }
        return j;

    }

}
