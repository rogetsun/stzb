package com.uv.stab;

import com.alibaba.fastjson.JSON;
import com.taobao.api.ApiException;
import com.uv.cbg.Cleaner;
import com.uv.cbg.Finder;
import com.uv.cbg.Notifier;
import com.uv.config.CbgReturnKey;
import com.uv.config.DingConf;
import com.uv.config.QueryConfig;
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
        this.finder.init();
        notifier.deleteAll();
    }

    @Override
    public void run(ApplicationArguments args) {
        log.debug("stzb finder starting");
//        log.debug(queryConfig.toString());
//        log.debug(dingConf.toString());
//        log.debug(cbgReturnKey.toString());
//        this.init();
//        this.finder.find();
//        this.parseTmpJson();


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


    private void parseTmpJson() throws IOException {
        File f = new File("src/main/resources/tmp.json");
        log.debug(String.valueOf(f.exists()));
        log.debug(f.getCanonicalPath());
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        String s;
        StringBuilder sb = new StringBuilder();
        while ((s = reader.readLine()) != null) {
            sb.append(s);
        }
        reader.close();
        log.debug(JSON.toJSONString(JSON.parseObject(sb.toString()), true));
    }


}
