package com.uv.Executor;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by uv2sun on 2017/5/17.
 */
@Slf4j(topic = "[EP]")
public class ExecutorPool extends ThreadPoolExecutor {

    //默认尝试次数
    private static final int DEFAULT_TRY_EXEC_TIMES = 100;
    //默认尝试时间间隔
    private static final int DEFAULT_TRY_DURATION = 10;
    //默认空闲线程存活时长
    private static final int DEFAULT_KEEP_ALIVE_TIME = 120;

    /**
     * 尝试加入执行队列执行，如果报异常执行队列满，则每隔 tryDuration毫秒再执行一次，执行 tryExecTimes次。
     */
    private int tryExecTimes;

    /**
     * 尝试加入执行队列执行，如果报异常执行队列满，则每隔 tryDuration毫秒再执行一次，执行 tryExecTimes次。
     */
    private int tryDuration;
    /**
     * 定义一个成员变量，用于记录当前线程池中已提交的任务数量
     */
    private final AtomicInteger submittedTaskCount = new AtomicInteger(0);

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        // ThreadPoolExecutor的勾子方法，在task执行完后需要将池中已提交的任务数 - 1
        submittedTaskCount.decrementAndGet();
    }

    @Override
    public void execute(Runnable command) {
        log.debug("execute:" + command);
        if (command == null) {
            throw new NullPointerException();
        }
        // do not increment in method beforeExecute!
        // 将池中已提交的任务数 + 1
        submittedTaskCount.incrementAndGet();
        try {
            super.execute(command);
        } catch (RejectedExecutionException rx) {
            // retry to offer the task into queue.
            final TaskQueue queue = (TaskQueue) super.getQueue();
            try {
                boolean offerOk = false;
                for (int i = 0; i < this.getTryExecTimes(); i++) {
                    offerOk = queue.retryOffer(command, this.getTryDuration(), TimeUnit.MILLISECONDS);
                }
                if (!offerOk) {
                    submittedTaskCount.decrementAndGet();
                    //todo 不会被下面的Throwable截获到么?
                    throw new RejectedExecutionException("Queue capacity is full.", rx);
                }
            } catch (InterruptedException x) {
                submittedTaskCount.decrementAndGet();
                throw new RejectedExecutionException(x);
            }
        } catch (Throwable t) {
            // decrease any way
            submittedTaskCount.decrementAndGet();
            throw t;
        }
    }

    public int getSubmittedTaskCount() {
        return submittedTaskCount.get();
    }


    public ExecutorPool(int corePoolSize, int maximumPoolSize, int queueSize) {
        super(corePoolSize, maximumPoolSize, ExecutorPool.DEFAULT_KEEP_ALIVE_TIME, TimeUnit.SECONDS, new TaskQueue<>(queueSize));
        ((TaskQueue) this.getQueue()).setExecutor(this);
    }

    public ExecutorPool(int corePoolSize, int maximumPoolSize, int queueSize, String threadPoolName) {
        super(corePoolSize, maximumPoolSize, ExecutorPool.DEFAULT_KEEP_ALIVE_TIME, TimeUnit.SECONDS, new TaskQueue<>(queueSize), new ThreadFactory(threadPoolName));
        ((TaskQueue) this.getQueue()).setExecutor(this);
    }


    public ExecutorPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, int queueSize) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, new TaskQueue<>(queueSize));
        ((TaskQueue) this.getQueue()).setExecutor(this);
    }

    public ExecutorPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, int queueSize, String threadPoolName) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, new TaskQueue<>(queueSize), new ThreadFactory(threadPoolName));
        ((TaskQueue) this.getQueue()).setExecutor(this);
    }


    public ExecutorPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, TaskQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        workQueue.setExecutor(this);
    }

    public ExecutorPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, TaskQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        workQueue.setExecutor(this);
    }

    public ExecutorPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, TaskQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        workQueue.setExecutor(this);
    }

    public ExecutorPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, TaskQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        workQueue.setExecutor(this);
    }


    public int getTryExecTimes() {
        return this.tryExecTimes == 0 ? ExecutorPool.DEFAULT_TRY_EXEC_TIMES : this.tryExecTimes;
    }

    /**
     * 尝试加入执行队列执行，如果报异常执行队列满，则每隔 tryDuration 毫秒再执行一次，执行 tryExecTimes次。
     */
    public void setTryExecTimes(int tryExecTimes) {
        this.tryExecTimes = tryExecTimes;
    }

    public int getTryDuration() {
        return this.tryDuration == 0 ? ExecutorPool.DEFAULT_TRY_DURATION : this.tryDuration;
    }

    /**
     * 尝试加入执行队列执行，如果报异常执行队列满，则每隔 tryDuration毫秒再执行一次，执行 tryExecTimes次。
     */
    public void setTryDuration(int tryDuration) {
        this.tryDuration = tryDuration;
    }


}
