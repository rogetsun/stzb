package com.uv.stab;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.uv.cbg.Cleaner;
import com.uv.cbg.Finder;
import com.uv.cbg.Notifier;
import com.uv.config.CbgReturnKey;
import com.uv.config.DingConf;
import com.uv.config.GameAutoConfigKey;
import com.uv.config.RunConfig;
import com.uv.db.mongo.service.MongoService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.io.*;

/**
 * @author uvsun
 */
@SpringBootApplication(scanBasePackages = {"com.uv.notify", "com.uv.cbg", "com.uv.db.mongo", "com.uv.config", "com.uv.controller"})
@Slf4j
@EnableMongoRepositories(basePackages = {"com.uv.db.mongo.repository"})
@EnableScheduling
@EnableAsync
public class StabApplication implements ApplicationRunner {

    @Resource
    private MongoService mongoService;
    @Resource
    private GameAutoConfigKey gameAutoConfigKey;
    @Resource
    private RunConfig runConfig;
    @Resource
    private DingConf dingConf;
    @Resource
    private CbgReturnKey cbgReturnKey;
    @Resource
    private Finder finder;
    @Resource
    private Notifier notifier;
    @Resource
    private Cleaner cleaner;

    @SneakyThrows
    public static void main(String[] args) {
        SpringApplication.run(StabApplication.class, args);
    }


    @SneakyThrows
    @Override
    public void run(ApplicationArguments args) {
        log.debug("stzb finder starting");
        args.getOptionNames().forEach(n -> {
            log.debug(n + ":" + args.getOptionValues(n) + ":" + (args.getOptionValues(n).getClass()));
            if (runConfig.getCmdLineInit().equalsIgnoreCase(n)) {
                // 删除所有数据,从配置文件重新配置SearchFilter(QueryConfig)
                this.init();
            } else if (runConfig.getCmdLineInitQuery().equalsIgnoreCase(n)) {
                // 删除所有SearchFilter,重新从配置文件根据QueryConfig初始化SearchFilter
                this.finder.initQuery();
            } else if (runConfig.getCmdLineSaveQuery().equalsIgnoreCase(n)) {
                // 不删除任何东西,直接从指定的JSONArray配置文件(queryConfig.json)根据配置好的searchFilterList保存SearchFilter
                try {
                    this.saveSearchFilterFromConfig(args.getOptionValues(n).get(0));
                } catch (IOException e) {
                    log.error("initGameConfig error, gameConfigFile:" + args.getOptionValues(n).get(0), e);
                }
            } else if (runConfig.getCmdLineInitNoQuery().equalsIgnoreCase(n)) {
                // 删除全部东西 除了 SearchFilter
                this.initAllExceptSearchFilter();

            } else if (runConfig.getCmdLineGameAutoConfig().equalsIgnoreCase(n)) {
                // 从 命令行指定的 game-config.json 文件 初始化英雄 技能 码表数据
                try {
                    this.initGameConfig(args.getOptionValues(n).get(0));
                } catch (IOException e) {
                    log.error("initGameConfig error, gameConfigFile:" + args.getOptionValues(n).get(0), e);
                }
            } else if (runConfig.getCmdLineGetHero().equalsIgnoreCase(n)) {
                // 从英雄码表 打印所有英雄, 格式 和hero.txt一样
                mongoService.getHeroAndPrint();
            } else if (runConfig.getCmdLineGetSkill().equalsIgnoreCase(n)) {
                // 从技能码表打印所有技能
                mongoService.getSkillAndPrint();
            } else if (runConfig.getCmdLineParseHero().equalsIgnoreCase(n)) {
                // 从经过删除的 cmdLine指定的 hero.txt 转化出 HeroId 数组
                try {
                    MongoService.parseAndPrint(this.readFile(args.getOptionValues(n).get(0)));
                } catch (IOException e) {
                    log.error("parseHero error, file:" + args.getOptionValues(n).get(0), e);
                }
            } else if (runConfig.getCmdLineParseSkill().equalsIgnoreCase(n)) {
                // 从经过删除的 cmdLine指定的 skill.txt 转化出 skillId 数组
                try {
                    MongoService.parseAndPrint(this.readFile(args.getOptionValues(n).get(0)));
                } catch (IOException e) {
                    log.error("parseSkill error, file:" + args.getOptionValues(n).get(0), e);
                }
            }
        });
//        log.debug(queryConfig.toString());
        log.debug(dingConf.toString());
        log.debug(cbgReturnKey.toString());
//        this.init();
//        this.finder.initQuery();
//        this.finder.find();
//        this.parseFile2Json("src/main/resources/tmp.json");
//        this.finder.saveQueryFromConfig();
//        mongoService.getSkillAndPrint();
//        mongoService.getHeroAndPrint();
//        MongoService.parseAndPrint(this.readFile("src/main/resources/hero.txt"));
//        this.initAllExceptSearchFilter();
//        this.saveSearchFilterFromConfig("src/main/resources/query-config.json");
    }

    private void saveSearchFilterFromConfig(String queryConfigJsonArrayFile) throws IOException {
        log.info("[APP]saveQuery BEGIN");
        if (queryConfigJsonArrayFile == null) {
            queryConfigJsonArrayFile = "query-config.json";
        }

        String string = this.readFile(queryConfigJsonArrayFile);
        if (null != string) {
            mongoService.saveSearchFilterFromConfig(string);
        } else {
            log.error("saveQueryFromConfig:file not exists;f=" + queryConfigJsonArrayFile);
        }
        log.info("[APP]saveQuery END");
    }


    private void initAllExceptSearchFilter() {
        log.info("[APP]initNotQuery Begin");
        this.finder.delAllGamer();
        this.notifier.deleteAll();
        this.finder.deleteAllSearchResult();
        log.info("[APP]initNotQuery End");
    }

    private void init() {
        log.info("[APP]init Begin");
        this.finder.init();
        notifier.deleteAll();
        log.info("[APP]init End");
    }

    @Scheduled(fixedDelayString = "#{scheduleConf.findDelay}", initialDelay = 5000)
    public void findJob() {
        try {
            this.finder.find();
            this.cleaner.clear();
        } catch (Throwable e) {
            log.error("清理角色失败,", e);
        }
    }

    @Scheduled(fixedDelayString = "#{scheduleConf.noticeDelay}")
    public void noticeJob() {
        try {
            notifier.notice();
        } catch (Throwable e) {
            log.error("执行notice失败, ", e);
        }
    }

    // 工具方法

    private void initGameConfig(String gameConfigFile) throws IOException {
        log.info("[APP]initGameConfig Begin");
        JSONObject j = this.parseFile2Json(gameConfigFile);
        if (null != j) {
            JSONArray skillArr = j.getJSONArray(gameAutoConfigKey.getSkills());
            mongoService.parseSkillAndSave(skillArr);
            JSONArray heroArr = j.getJSONArray(gameAutoConfigKey.getHero());
            mongoService.parseHeroAndSave(heroArr);
        }
        log.info("[APP]initGameConfig End");
    }

    private JSONObject parseFile2Json(String file) throws IOException {
        String string = readFile(file);
        if (string == null) {
            return null;
        }
        JSONObject j = JSON.parseObject(string);
        log.trace(JSON.toJSONString(j, true));
        return j;
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
}
