package com.uv.stab;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.api.ApiException;
import com.uv.cbg.Cleaner;
import com.uv.cbg.Finder;
import com.uv.cbg.Notifier;
import com.uv.config.*;
import com.uv.db.mongo.service.MongoService;
import com.uv.notify.DingNotify;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@SpringBootApplication(scanBasePackages = {"com.uv.notify", "com.uv.cbg", "com.uv.db", "com.uv.config"})
@Slf4j
@EnableMongoRepositories(basePackages = {"com.uv.db.mongo.repository"})
@EnableScheduling
public class StabApplication implements ApplicationRunner {

    @Resource(name = "dingNotify")
    private DingNotify notify;

    @Resource
    private MongoService mongoService;
    @Resource
    private GameAutoConfigKey gameAutoConfigKey;
    @Resource
    private RunConfig runConfig;
    @Resource
    private QueryConfig queryConfig;
    @Resource
    private DingConf dingConf;
    @Resource
    private CbgReturnKey cbgReturnKey;
    @Resource
    private Finder finder;
    @Resource
    private Notifier notifier;
    @Resource
    private DingNotify dingNotify;
    @Resource
    private Cleaner cleaner;

    @SneakyThrows
    public static void main(String[] args) {
        SpringApplication.run(StabApplication.class, args);
    }


    private void init() {
        log.info("[APP]init Begin");
        this.finder.init();
        notifier.deleteAll();
        log.info("[APP]init End");
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        log.debug("stzb finder starting");
        args.getOptionNames().forEach(n -> {
            log.debug(n + ":" + args.getOptionValues(n) + ":" + (args.getOptionValues(n).getClass()));
            if (runConfig.getGameAutoConfig().equals(n)) {
                try {
                    this.initGameConfig(args.getOptionValues(n).get(0));
                } catch (IOException e) {
                    log.error("initGameConfig error, gameConfigFile:" + args.getOptionValues(n).get(0), e);
                }
            } else if (runConfig.getInit().equals(n)) {
                this.init();
            } else if (runConfig.getInitQuery().equals(n)) {
                this.finder.initQuery();
            } else if (runConfig.getSaveQuery().equals(n)) {
                this.finder.saveQueryFromConfig();
            }
        });
//        log.debug(queryConfig.toString());
//        log.debug(dingConf.toString());
//        log.debug(cbgReturnKey.toString());
//        this.init();
//        this.finder.initQuery();
//        System.out.println(this.queryConfig.getHero().getClass());
//        this.finder.find();
//        this.parseFile2Json("src/main/resources/tmp.json");

    }

    @Scheduled(fixedDelayString = "#{scheduleConf.findDelay}", initialDelay = 30000)
    public void findJob() {
        finder.find();
    }

    @Scheduled(fixedDelayString = "#{scheduleConf.noticeDelay}")
    public void noticeJob() {
        try {
            notifier.notice();
        } catch (NoSuchAlgorithmException | ApiException | InvalidKeyException | UnsupportedEncodingException e) {
            log.error("执行notice失败, ", e);
        }
    }

    @Scheduled(fixedDelayString = "#{scheduleConf.clearDelay}")
    public void clearJob() {
        try {
            this.cleaner.clear();
        } catch (Exception e) {
            log.error("清理角色失败,", e);
        }
    }


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
        File f = new File(file);
        log.debug(file + ".exists:" + String.valueOf(f.exists()));
        if (!f.exists()) {
            return null;
        }
        log.debug(f.getCanonicalPath());
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        String s;
        StringBuilder sb = new StringBuilder();
        while ((s = reader.readLine()) != null) {
            sb.append(s);
        }
        reader.close();
        JSONObject j = JSON.parseObject(sb.toString());
        log.trace(JSON.toJSONString(j, true));
        return j;
    }
}
