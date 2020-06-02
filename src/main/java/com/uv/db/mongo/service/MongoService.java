package com.uv.db.mongo.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.uv.config.CbgReturnKey;
import com.uv.config.DingConf;
import com.uv.config.GameAutoConfigKey;
import com.uv.db.mongo.entity.*;
import com.uv.db.mongo.repository.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    private SearchResultRepository searchResultRepository;
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
    @Value("${cbg.webDetailUrl}")
    private String cbgWebUrl;
    @Value("${cbg.gamerResUrl}")
    private String gamerResUrl;
    @Value("#{dingConf.gamerUrl}")
    private String gamerUrl;
    @Resource
    private NoticeRepository noticeRepository;
    @Resource
    private CbgReturnKey cbgReturnKey;
    @Resource
    private DingConf dingConf;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");


    // Notice 部分

    /**
     * 发送处理异常通知
     *
     * @param title
     * @param content
     * @param execTimestamp
     * @return
     */
    public Notice sendExceptionNotice(String title, String content, long execTimestamp) {
        Notice notice = Notice.builder()
                .id("ERROR" + sdf.format(new Date(execTimestamp)))
                .title(title)
                .content(content)
                .createTime(new Date(execTimestamp))
                .hasNotify(false)
                .dingUrl(dingConf.getUrl())
                .dingSecret(dingConf.getSecret())
                .build();
        noticeRepository.save(notice);
        log.info("[NOTICE]new:" + notice.toString());
        return notice;
    }

    /**
     * @param filter
     * @param gamer
     * @param simpleGamer
     * @param execTimestamp
     */
    public void sendStatusChangedNotice(SearchFilter filter, Gamer gamer, SearchResult.SimpleGamer simpleGamer, long execTimestamp) {
        if (filter != null) {
            Notice notice = generateStatusChangeNotice(filter, gamer, simpleGamer, execTimestamp);
            noticeRepository.save(notice);
            log.debug("[NOTICE]new:" + notice.toString());
        }

    }

    public Notice generateStatusChangeNotice(SearchFilter filter, Gamer gamer, SearchResult.SimpleGamer simpleGamer, long execTimestamp) {
        return Notice.builder()
                .id(filter.getId() + "-" + gamer.getId())
                .dingUrl(filter.getDingUrl())
                .dingSecret(filter.getDingSecret())
                .hasNotify(false)
                .createTime(new Date(execTimestamp))
                .title("[" + gamer.getSellStatus() + "][" + gamer.getSellStatusDesc() + "]" + this.generateNoticeTitle(gamer, simpleGamer))
                .content(this.generateNoticeContent(filter, gamer, simpleGamer))
                .url(this.generateWebUrl(gamer))
                .icon(this.generateIconUrl(gamer))
                .gamerUrl(gamerUrl + "?" + "filter=" + filter.getId() + "&sn=" + gamer.getOrderSn())
                .build();
    }

    /**
     * 入参有simpleGamer的,一般为价格变动通知
     *
     * @param filter
     * @param gamer
     * @param simpleGamer
     * @param timestamp
     */
    public void sendPriceChangedNotice(SearchFilter filter, Gamer gamer, SearchResult.SimpleGamer simpleGamer, long timestamp) {
        Notice notice = generatePriceNotice(filter, gamer, simpleGamer, timestamp);
        if (notice != null) {
            noticeRepository.save(notice);
            log.debug("[NOTICE]new:" + notice.toString());
        }
    }

    public Notice generatePriceNotice(SearchFilter filter, Gamer gamer, SearchResult.SimpleGamer simpleGamer, long timestamp) {
        Notice notice = null;
        String titleKey = null;
        if (simpleGamer.getLastPrice() == 0) {
            titleKey = "[新]";
        } else if (simpleGamer.getLastPrice() < simpleGamer.getPrice()) {
            titleKey = "[涨]";
        } else if (simpleGamer.getLastPrice() > simpleGamer.getPrice()) {
            titleKey = "[降]";
        }
        if (titleKey != null) {
            notice = Notice.builder()
                    .id(filter.getId() + "-" + gamer.getId())
                    .dingUrl(filter.getDingUrl())
                    .dingSecret(filter.getDingSecret())
                    .hasNotify(false)
                    .createTime(new Date(timestamp))
                    .title(titleKey + this.generateNoticeTitle(gamer, simpleGamer))
                    .content(this.generateNoticeContent(filter, gamer, simpleGamer))
                    .url(this.generateWebUrl(gamer))
                    .icon(this.generateIconUrl(gamer))
                    .gamerUrl(gamerUrl + "?" + "filter=" + filter.getId() + "&sn=" + gamer.getOrderSn())
                    .build();

        }
        return notice;
    }

    public String generateIconUrl(Gamer g) {
        return this.gamerResUrl + g.getIcon();
    }

    public String generateWebUrl(Gamer gamer) {
        return this.cbgWebUrl + "/" + gamer.getServerId() + "/" + gamer.getOrderSn() + "?view_loc=equip_list";
    }

    private String generateNoticeTitle(Gamer gamer, SearchResult.SimpleGamer simpleGamer) {
        return (simpleGamer.getPrice() / 100) +
                "[" +
                (null == simpleGamer ? "0" : simpleGamer.getHeroFitDegree()) +
                ":" +
                (null == simpleGamer ? "0" : simpleGamer.getSkillFitDegree()) +
                "]" +
                gamer.getName();
    }

    private String generateNoticeContent(SearchFilter filter, Gamer gamer, SearchResult.SimpleGamer simpleGamer) {
        StringBuilder sb = new StringBuilder();

        if (simpleGamer.getLastPrice() != simpleGamer.getPrice() && simpleGamer.getLastPrice() != 0) {
            sb.append("LP:").append(simpleGamer.getLastPrice() / 100).append(",");
        }
        if (gamer.getFirstPrice() != 0) {
            sb.append("FP:").append(gamer.getFirstPrice() / 100).append(",");
        }

        sb
                //核心英雄统计数据
                .append("CH:[")
                .append(filter.getContainsHero().size())
                .append("/").append(simpleGamer.getCoreHeroAdvanceSum())
                .append("/").append(simpleGamer.getCoreHeroAwakeSum())
                .append("/").append(simpleGamer.getCoreHeroArmUnlockSum())
                .append("/").append(simpleGamer.getCoreHeroArmAdvanceSum())
                .append("]")
                //可选英雄统计数据
                .append("OH:[")
                .append(filter.getOptionHero().size())
                .append("/").append(simpleGamer.getOptionHeroAdvanceSum())
                .append("/").append(simpleGamer.getOptionHeroAwakeSum())
                .append("/").append(simpleGamer.getOptionHeroArmUnlockSum())
                .append("/").append(simpleGamer.getOptionHeroArmAdvanceSum())
                .append("]")
                //
                .append("5:[").append(gamer.getFiveStarCount()).append("]")
                .append(",SK:[").append(gamer.getSkillCount()).append("]")
                .append(",Y:").append(gamer.getTenure().getIntValue(cbgReturnKey.getDetailTenureYuanBaoKey()))
                .append(",DJ:").append(gamer.getDianJiCount())
                .append(",DC:").append(gamer.getDianCangCount());

        return sb.toString();
    }

    // SearchFilter 部分
    public void refreshSearchFilterUpdateTime() {
        log.debug("[APP]refreshSearchFilterUpdateTime");
        searchFilterRepository.findAll().forEach(searchFilter -> {
            searchFilter.setUpdateTime(new Date());
            log.debug("[APP]" + searchFilter.toString());
            searchFilterRepository.save(searchFilter);
        });
        log.debug("[APP]refreshSearchFilterUpdateTime END");

    }

    public void saveSearchFilterFromConfig(String queryConfigJsonArrayFile) throws IOException {
        log.info("[APP]saveQuery BEGIN");
        if (queryConfigJsonArrayFile == null) {
            queryConfigJsonArrayFile = "query-config.json";
        }

        String string = this.readFile(queryConfigJsonArrayFile);
        if (null != string) {
            this.saveSearchFilter(string);
        } else {
            log.error("saveQueryFromConfig:file not exists;f=" + queryConfigJsonArrayFile);
        }
        log.info("[APP]saveQuery END");
    }

    private String readFile(String file) throws IOException {
        File f = new File(file);
        log.info(file + ".exists:" + f.exists());
        if (!f.exists()) {
            return null;
        }
        log.info(f.getCanonicalPath());
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        String s;
        StringBuilder sb = new StringBuilder();
        while ((s = reader.readLine()) != null) {
            log.info("[" + s + "]");
            sb.append(s).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public List<SearchFilter> getAllSearchFilter() {
        return searchFilterRepository.findAll();
    }

    public SearchFilter saveSearchFilter(SearchFilter filter) {
        return searchFilterRepository.save(filter);
    }

    public void delAllSearchFilter() {
        searchFilterRepository.deleteAll();
    }

    // SearchResult 部分

    public SearchResult saveSearchResult(SearchResult result) {
        result.refreshActionGamerIds();
        return this.searchResultRepository.save(result);
    }

    private void saveSearchFilter(String jsonArrayString) {

        JSONArray jsonArray = JSON.parseArray(jsonArrayString);
        List<SearchFilter> l = jsonArray.toJavaList(SearchFilter.class);
        if (l != null) {
            for (SearchFilter filter : l) {
                filter.setUpdateTime(new Date());
                log.info("saveSearchFilterFromConfig:" + filter);

                searchFilterRepository.save(filter);
            }
        }

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

    public static void main(String[] args) {
        String s = "200194:2:避其锋芒:****\n" +
                "200201:2:无心恋战:****\n" +
                "200834:3:人中吕布:*****\n" +
                "200900:1:垒实迎击:*****\n" +
                "200767:1:谋定后动:****\n" +
                "200220:2:反计之策:***\n" +
                "200673:3:掎角之势:***\n" +
                "200721:3:折戟强攻:****\n" +
                "200684:3:道行险阻:****\n" +
                "200801:3:利兵谋胜:****\n" +
                "200829:3:兼弱攻昧:****\n" +
                "200800:2:众谋不懈:****\n" +
                "200847:3:河内世泽:***\n" +
                "200784:2:桃园结义:****\n" +
                "200817:3:极火佐攻:****\n" +
                "200658:4:钝兵挫锐:***\n" +
                "200659:4:怯心夺志:***\n" +
                "200674:2:一夫当关:***\n" +
                "200676:3:雄兵破敌:***\n" +
                "200844:3:鼎足江东:***\n" +
                "200715:3:十面埋伏:***\n" +
                "200717:2:鹤翼:**\n" +
                "200818:2:反间:****\n" +
                "200734:2:擅兵不寡:***\n" +
                "200731:3:全军突击:***\n" +
                "200826:3:绝水遏敌:****\n" +
                "200119:3:焰焚箕轸:**\n" +
                "200122:3:水淹七军:**\n" +
                "200130:3:楚歌四起:**\n" +
                "200863:1:击势:****\n" +
                "200813:2:形兵之极:***\n" +
                "200208:4:温酒斩将:**\n" +
                "200217:3:安抚军心:**\n" +
                "200839:4:疾击其后:****\n" +
                "200862:2:单骑救主:**\n" +
                "200224:2:穷追猛打:*\n" +
                "200226:2:长兵方阵:**\n" +
                "200786:4:掠敌之利:****\n" +
                "200184:1:百战精兵:***\n" +
                "200714:3:万箭齐发\n" +
                "200239:3:移花接木:*\n" +
                "200644:1:步步为营:*\n" +
                "200926:3:威震逍遥:**\n" +
                "200646:2:疾风迅雷:*\n" +
                "200883:2:鸟云山兵:***\n" +
                "200888:3:当阳桥:***\n" +
                "200814:3:汜水关:***\n" +
                "200815:3:奇正之势:**\n" +
                "200788:1:枭雄:***\n" +
                "200789:3:凤仪亭:***\n" +
                "200812:2:兵贵神速:*\n" +
                "200688:3:合流:**\n" +
                "200781:2:衡轭:**\n" +
                "200782:2:疏数:*\n" +
                "200783:2:鱼鳞:*\n" +
                "200691:2:白刃:*\n" +
                "200822:1:合众:**\n" +
                "200823:3:武锋:**\n" +
                "200632:1:回马:**";
        parseAndPrint(s);
    }


}
