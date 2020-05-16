package io.netty.util.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * Default {@link SingleThreadEventExecutor} implementation which just execute all submitted task in a
 * serial fashion.
 *
 * 默认的SingleThreadEventExecutor实现仅以串行方式执行所有提交的任务。
 */
public final class DefaultEventExecutor extends SingleThreadEventExecutor {

    public DefaultEventExecutor() {
        this((EventExecutorGroup) null);
    }

    public DefaultEventExecutor(ThreadFactory threadFactory) {
        this(null, threadFactory);
    }

    public DefaultEventExecutor(Executor executor) {
        this(null, executor);
    }

    public DefaultEventExecutor(EventExecutorGroup parent) {
        this(parent, new DefaultThreadFactory(DefaultEventExecutor.class));
    }

    public DefaultEventExecutor(EventExecutorGroup parent, ThreadFactory threadFactory) {
        super(parent, threadFactory, true);
    }

    public DefaultEventExecutor(EventExecutorGroup parent, Executor executor) {
        super(parent, executor, true);
    }

    public DefaultEventExecutor(EventExecutorGroup parent, ThreadFactory threadFactory, int maxPendingTasks,
            RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, threadFactory, true, maxPendingTasks, rejectedExecutionHandler);
    }

    public DefaultEventExecutor(EventExecutorGroup parent, Executor executor, int maxPendingTasks,
            RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, executor, true, maxPendingTasks, rejectedExecutionHandler);
    }

    @Override
    protected void run() {
        for (; ; ) {
            Runnable task = takeTask();
            if (task != null) {
                task.run();
                updateLastExecutionTime();
            }

            if (confirmShutdown()) {
                //每执行一个任务之后都要确认下是否已经 shutdown
                break;
            }
        }
    }
}
