package com.uv.config;

import com.uv.Executor.ExecutorPool;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author uvsun 2020/3/14 9:06 下午
 * 本类完成
 * 1.线程池的基本配置从配置文件application.yml中获取
 * 2.配置线程池实体类两个功能
 * * 也可以分放两个类
 * 3.定时任务线程池Bean配置生成
 */
@Configuration
@ConfigurationProperties(prefix = "run")
@Data
public class RunConfig {
    /**
     * 线程池相关配置,从application.yml中 run: 下的配置
     */
    private String scheduleThreadNamePrefix;
    private int scheduleThreadNum;

    private String threadPoolNamePrefix;
    private int threadPoolCoreSize;
    private int threadPoolMaxSize;
    private int threadPoolQueueCapacity;
    private int threadPoolKeepAliveSec;

    private String cmdLineInit;
    private String cmdLineInitQuery;
    private String cmdLineSaveQuery;
    private String cmdLineGameAutoConfig;
    private String cmdLineGetHero;
    private String cmdLineGetSkill;
    private String cmdLineParseHero;
    private String cmdLineParseSkill;


    // 配置查找,通知,清理定时任务 以及 线程池

    /**
     * 配置定时任务执行线程池
     *
     * @return
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

        taskScheduler.setPoolSize(this.scheduleThreadNum);

        if (this.scheduleThreadNamePrefix != null && !"".equals(this.scheduleThreadNamePrefix)) {
            taskScheduler.setThreadNamePrefix(this.scheduleThreadNamePrefix);
        }

        return taskScheduler;
    }

    /**
     * * 配置线程池实体类;
     * * spring默认使用的类型为TaskExecutor;因此不能用Executors.newXXX构建
     *
     * @return
     */
    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 设置核心线程数
        executor.setCorePoolSize(this.threadPoolCoreSize == 0 ? 1 : this.threadPoolCoreSize);
        // 设置最大线程数
        executor.setMaxPoolSize(this.threadPoolMaxSize == 0 ? 5 : this.threadPoolMaxSize);
        // 设置队列容量
        executor.setQueueCapacity(this.threadPoolQueueCapacity == 0 ? 20 : this.threadPoolQueueCapacity);
        // 设置线程活跃时间（秒）
        executor.setKeepAliveSeconds(this.threadPoolKeepAliveSec == 0 ? 60 : this.threadPoolKeepAliveSec);
        // 设置默认线程名称
        executor.setThreadNamePrefix("DefaultExecutor-");

        // 设置拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean
    public ExecutorPool uvExecutor() {
        ExecutorPool pool = new ExecutorPool(this.threadPoolCoreSize, this.threadPoolMaxSize, this.threadPoolKeepAliveSec, this.threadPoolQueueCapacity, this.threadPoolNamePrefix);
        return pool;
    }


}
