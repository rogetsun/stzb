package com.uv.controller;

import com.alibaba.fastjson.JSONObject;
import com.uv.cbg.Finder;
import com.uv.cbg.Searcher;
import com.uv.db.mongo.entity.*;
import com.uv.db.mongo.repository.*;
import com.uv.db.mongo.service.MongoService;
import com.uv.exception.CbgException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

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
    @Resource
    private HeroRepository heroRepository;
    @Resource
    private SkillRepository skillRepository;
    @Resource
    private GamerRepository gamerRepository;
    @Resource
    private GamerHisRepository gamerHisRepository;

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/gamer-compute/{searchFilterId}/{orderSn}")
    @ResponseBody
    public JSONObject computeStatis(@PathVariable int searchFilterId, @PathVariable String orderSn) {
        log.debug("searchFilterId:" + searchFilterId + ", orderSn:" + orderSn);
        JSONObject j = new JSONObject();

        try {
            Gamer g = gamerRepository.findGamerByOrderSn(orderSn);
//            if (g == null) {
//                GamerHis gamerHis = gamerHisRepository.findByOrderSn(orderSn);
//                if (gamerHis != null) {
//                    g = gamerHis.toGamer();
//                }
//            }
            if (g == null) {
                JSONObject equip = searcher.queryGamerDetail(orderSn);
//            log.debug(JSON.toJSONString(equip, true));
                g = searcher.generateGamer(equip);
                log.debug(g.toString());
                searcher.generateAndSetGamerDetail(g, equip);
            }

            SearchFilter filter = searchFilterRepository.findById(searchFilterId).orElse(null);

            if (filter != null) {
                SearchResult.SimpleGamer simpleGamer = SearchResult.generateSimpleGamer(g, System.currentTimeMillis());
                boolean analysisResult = finder.execAnalysis(g, filter, simpleGamer);
                j.put("analysisResult", analysisResult);
                j.put("simpleGamer", simpleGamer);
                Notice notice = mongoService.generateStatusChangeNotice(filter, g, simpleGamer, System.currentTimeMillis());
                j.put("notice", notice);
                j.put("filter", filter);
            }

            j.put("gamer", g);

            if (g.getSkillIds() != null && g.getSkillIds().size() > 0) {
                List<Skill> skills = skillRepository.findAllBySkillIdIn(g.getSkillIds());
                j.put("skill", skills);
            }
        } catch (CbgException e) {
            log.error("queryGamerDetail error,", e);
        }
        return j;

    }

    @RequestMapping(path = "/hero/{heroId}", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public Hero queryHero(@PathVariable Integer heroId) {
        Hero h = heroRepository.findById(heroId).orElse(null);
        if (h != null) {
            log.trace(h.toString());
            return h;
        } else {
            return null;
        }
    }

    @RequestMapping(path = "/skill/{skillId}", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public Skill querySkill(@PathVariable Integer skillId) {

        Skill s = skillRepository.findById(skillId).orElse(null);
        if (null != s) {
            log.trace(s.toString());
            return s;
        } else {
            return null;
        }
    }

}
