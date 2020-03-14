package com.uv.db.mongo.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.uv.config.GameAutoConfigKey;
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
    private GameAutoConfigKey.HeroKeyConf heroKeyConf;
    @Resource
    private GameAutoConfigKey.SkillKeyConf skillKeyConf;


    // SearchFilter 部分

    public List<SearchFilter> getAllSearchFilter() {
        return searchFilterRepository.findAll();
    }

    public SearchFilter saveSearchFilter(SearchFilter filter) {
        return searchFilterRepository.save(filter);
    }

    public void delAllSearchFilter() {
        searchFilterRepository.deleteAll();
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

    public void getSkillAndPrint() {
        System.out.println("parseSkill begin");
        List<Skill> l = skillRepository.findAll();
        for (Skill skill : l) {
            System.out.println(skill.getSkillId() + ":" + skill.getSkillType() + ":" + skill.getName());
        }
        System.out.println("parseSkill over");
    }

    public void getHeroAndPrint() {
        System.out.println("parseHero begin");
        List<Hero> l = heroRepository.findAllByQuality(5);
        for (Hero hero : l) {
            String c;
            switch (hero.getCountry()) {
                case "1":
                    c = "汉";
                    break;
                case "2":
                    c = "魏";
                    break;
                case "3":
                    c = "蜀";
                    break;
                case "4":
                    c = "吴";
                    break;
                case "5":
                    c = "群";
                    break;
                default:
                    c = "";
            }
            System.out.println(hero.getHeroId() + ":" + hero.getHeroType() + ":" + c + hero.getName());
        }
        System.out.println("parseHero over");
    }

    public static void main(String[] args) {
        String s = "100631:3:蜀严颜\n" +
                "100013:3:群马超\n" +
                "100615:3:蜀马岱\n" +
                "100479:1:群吕布\n" +
                "100072:2:蜀关银屏\n" +
                "100019:3:蜀马云禄\n" +
                "100020:2:蜀黄月英\n" +
                "100618:1:魏贾诩\n" +
                "100021:2:蜀赵云\n" +
                "100023:3:魏曹操\n" +
                "100496:1:蜀诸葛亮\n" +
                "100451:3:蜀关羽\n" +
                "100475:2:魏郝昭\n" +
                "100574:2:吴陆抗\n" +
                "100582:3:蜀法正\n" +
                "100480:2:汉董卓\n" +
                "100534:3:蜀徐庶\n" +
                "100494:3:群祝融夫人\n" +
                "100630:2:汉皇甫嵩\n" +
                "100029:1:魏张春华\n" +
                "100028:1:魏王异\n" +
                "100478:2:吴陆逊\n" +
                "100101:1:汉灵帝\n" +
                "100476:3:魏郭嘉\n" +
                "100024:3:魏荀彧\n" +
                "100586:3:群庞德\n" +
                "100337:2:群貂蝉\n" +
                "  \n" +
                "100589:2:吴周泰\n" +
                "102001:2:蜀赵云\n";
        parseAndPrint(s);
    }

    public static void parseAndPrint(String s) {
        System.out.println(s);
        System.out.println(s.split("[\\s\\t\\n]+").length);
        s = s
                .replaceAll(":\\d:", ":")
                .replaceAll(":?[*]*[\\s\\n\\t]+", ",");
        System.out.println(s);
        System.out.println(s.split(",").length);
        s = s.replaceAll("[^\\d,]*", "");
        System.out.println(s);
        System.out.println(s.split(",").length);
    }
}
