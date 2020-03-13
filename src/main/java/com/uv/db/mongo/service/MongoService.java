package com.uv.db.mongo.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.uv.config.GameKeyConf;
import com.uv.db.mongo.entity.Gamer;
import com.uv.db.mongo.entity.Hero;
import com.uv.db.mongo.entity.SearchFilter;
import com.uv.db.mongo.entity.Skill;
import com.uv.db.mongo.repository.GamerRepository;
import com.uv.db.mongo.repository.HeroRepository;
import com.uv.db.mongo.repository.SearchFilterRepository;
import com.uv.db.mongo.repository.SkillRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author uvsun 2020/3/11 9:24 下午
 */
@Service
@Data
@Slf4j
public class MongoService {
    @Resource
    private SearchFilterRepository searchFilterRepository;

    @Resource
    private GamerRepository gamerRepository;
    @Resource
    private HeroRepository heroRepository;
    @Resource
    private SkillRepository skillRepository;
    @Resource
    private GameKeyConf.HeroKeyConf heroKeyConf;
    @Resource
    private GameKeyConf.SkillKeyConf skillKeyConf;


    // SearchFilter 部分

    public List<SearchFilter> getAllSearchFilter() {
        return searchFilterRepository.findAll();
    }

    public SearchFilter getSearchFilterById(String id) {
        return searchFilterRepository.findById(id).orElse(null);
    }

    public SearchFilter saveSearchFilter(SearchFilter filter) {
        return searchFilterRepository.save(filter);
    }

    public void delAllSearchFilter() {
        searchFilterRepository.deleteAll();
    }

    public List<SearchFilter> getMaxPriceGreatThan(int price) {
        return searchFilterRepository.findByMaxPriceGreaterThanEqual(price);
    }

    // Gamer 部分

    public Gamer saveGamer(Gamer gamer) {
        return gamerRepository.save(gamer);
    }

    public Gamer findGamerBySn(String sn) {
        Optional<Gamer> o = gamerRepository.findById(sn);
        return o.orElse(null);
    }

    public void delAllGamer() {
        gamerRepository.deleteAll();
    }

    public Gamer findGamerById(String sn) {
        return this.findGamerBySn(sn);
    }

    //game-config 相关操作

    /**
     * 根据藏宝阁 game-auto-config.js 返回的内容中 hero 相关 JSONArray 解析并入库
     *
     * @param heros
     * @return
     */
    public List<Hero> parseHeroAndSave(JSONArray heros) {
        log.info("[GC]begin to parseHeroAndSave");
        List<Hero> l = new ArrayList<>();
        heros.forEach(heroObject -> {
            JSONObject h = JSON.parseObject(heroObject.toString());
            Hero hero = Hero.builder()
                    .id(h.getIntValue(heroKeyConf.getHeroId()))
                    .heroId(h.getIntValue(heroKeyConf.getHeroId()))
                    .heroType(h.getIntValue(heroKeyConf.getHeroType()))
                    .iconHeroId(h.getIntValue(heroKeyConf.getHeroId()))
                    .name(h.getString(heroKeyConf.getName()))
                    .country(h.getString(heroKeyConf.getCountry()))
                    .pinyin(h.getString(heroKeyConf.getPinyin()))
                    .quality(h.getIntValue(heroKeyConf.getQuality()))
                    .season(h.getString(heroKeyConf.getSeason()))
                    .build();

            l.add(hero);
            log.trace("[GC]" + hero.toString());
        });
        heroRepository.saveAll(l);
        log.info("[GC]end to parseHeroAndSave, save " + l.size() + " hero");
        return l;
    }

    /**
     * 根据藏宝阁 game-auto-config.js 返回的内容中 skill 相关 JSONArray 解析并入库
     *
     * @param skillArray
     * @return
     */
    public List<Skill> parseSkillAndSave(JSONArray skillArray) {
        log.info("[GC]begin to parseSkillAndSave");
        List<Skill> l = new ArrayList<>();
        skillArray.forEach(skillObject -> {
            JSONObject skillJSON = JSON.parseObject(skillObject.toString());
            Skill skill = Skill.builder()
                    .id(skillJSON.getIntValue(skillKeyConf.getSkillId()))
                    .skillId(skillJSON.getIntValue(skillKeyConf.getSkillId()))
                    .skillType(skillJSON.getIntValue(skillKeyConf.getSkillType()))
                    .name(skillJSON.getString(skillKeyConf.getName()))
                    .build();
            l.add(skill);
            log.trace("[GC]" + skill.toString());
        });
        skillRepository.saveAll(l);
        log.info("[GC]end to parseSkillAndSave, save " + l.size() + " skill");
        return l;
    }

}
