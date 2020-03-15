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
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.Resource;
import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * @author uvsun
 */
@SpringBootApplication(scanBasePackages = {"com.uv.notify", "com.uv.cbg", "com.uv.db.mongo", "com.uv.config"})
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
    @Resource
    private ThreadPoolTaskScheduler taskScheduler;
    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    @SneakyThrows
    public static void main(String[] args) {
        SpringApplication.run(StabApplication.class, args);
    }


    @Override
    public void run(ApplicationArguments args) throws IOException {
        log.debug("stzb finder starting");
        args.getOptionNames().forEach(n -> {
            log.debug(n + ":" + args.getOptionValues(n) + ":" + (args.getOptionValues(n).getClass()));
            if (runConfig.getCmdLineGameAutoConfig().equalsIgnoreCase(n)) {
                try {
                    this.initGameConfig(args.getOptionValues(n).get(0));
                } catch (IOException e) {
                    log.error("initGameConfig error, gameConfigFile:" + args.getOptionValues(n).get(0), e);
                }
            } else if (runConfig.getCmdLineInit().equalsIgnoreCase(n)) {
                this.init();
            } else if (runConfig.getCmdLineInitQuery().equalsIgnoreCase(n)) {
                this.finder.initQuery();
            } else if (runConfig.getCmdLineSaveQuery().equalsIgnoreCase(n)) {
                this.finder.saveQueryFromConfig();
            } else if (runConfig.getCmdLineGetHero().equalsIgnoreCase(n)) {
                mongoService.getHeroAndPrint();
            } else if (runConfig.getCmdLineGetSkill().equalsIgnoreCase(n)) {
                mongoService.getSkillAndPrint();
            } else if (runConfig.getCmdLineParseHero().equalsIgnoreCase(n)) {
                try {
                    MongoService.parseAndPrint(this.readFile(args.getOptionValues(n).get(0)));
                } catch (IOException e) {
                    log.error("parseHero error, file:" + args.getOptionValues(n).get(0), e);
                }
            } else if (runConfig.getCmdLineParseSkill().equalsIgnoreCase(n)) {
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
        log.trace(taskScheduler.toString() + taskScheduler.getThreadNamePrefix() + ":" + taskScheduler.getActiveCount() + ":" + taskScheduler.getPoolSize());
        log.trace(taskExecutor.toString() + taskExecutor.getThreadNamePrefix() + ":" + taskExecutor.getActiveCount() + ":" + taskExecutor.getCorePoolSize());

        try {
            finder.test1();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            finder.test2();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        log.info("[APP]init Begin");
        this.finder.init();
        notifier.deleteAll();
        log.info("[APP]init End");
    }

    @Scheduled(fixedDelayString = "#{scheduleConf.findDelay}", initialDelay = 50000)
    public void findJob() {
        finder.find();
        try {
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
        String string = sb.toString();
        return string;
    }
}
