package io.netty.util.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * 每个任务都使用一个线程
 *
 * 用户只需要传入一个 Runnable，该执行器则会生成一个线程去执行该任务
 */
public final class ThreadPerTaskExecutor implements Executor {

    /**
     * 通过该工厂创建的线程去执行任务
     *
     * @see MultithreadEventExecutorGroup#newDefaultThreadFactory()
     */
    private final ThreadFactory threadFactory;

    public ThreadPerTaskExecutor(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        this.threadFactory = threadFactory;
    }

    @Override
    public void execute(Runnable command) {
        threadFactory.newThread(command) // 每个任务都创建一个线程去执行
                .start();
    }
}
