package io.netty.util.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * Default implementation of {@link MultithreadEventExecutorGroup} which will use {@link DefaultEventExecutor} instances
 * to handle the tasks.
 *
 * MultithreadEventExecutorGroup的默认实现，它将使用DefaultEventExecutor实例来处理任务。
 */
public class DefaultEventExecutorGroup extends MultithreadEventExecutorGroup {

    /**
     * @see #DefaultEventExecutorGroup(int, ThreadFactory)
     */
    public DefaultEventExecutorGroup(int nThreads) {
        this(nThreads, null);
    }

    /**
     * Create a new instance.
     *
     * @param nThreads the number of threads that will be used by this instance. 此实例将使用的线程数。
     * @param threadFactory the ThreadFactory to use, or {@code null} if the default should be used.
     * 要使用的ThreadFactory；如果应使用默认值，则为null
     */
    public DefaultEventExecutorGroup(int nThreads, ThreadFactory threadFactory) {
        this(nThreads, threadFactory, SingleThreadEventExecutor.DEFAULT_MAX_PENDING_EXECUTOR_TASKS,
                RejectedExecutionHandlers.reject());
    }

    /**
     * Create a new instance.
     *
     * @param nThreads the number of threads that will be used by this instance.
     * @param threadFactory the ThreadFactory to use, or {@code null} if the default should be used.
     * @param maxPendingTasks the maximum number of pending tasks before new tasks will be rejected.
     * @param rejectedHandler the {@link RejectedExecutionHandler} to use.
     */
    public DefaultEventExecutorGroup(int nThreads, ThreadFactory threadFactory, int maxPendingTasks,
            RejectedExecutionHandler rejectedHandler) {
        super(nThreads, threadFactory, maxPendingTasks, rejectedHandler);
    }

    @Override
    protected EventExecutor newChild(Executor executor, Object... args) throws Exception {
        //一个单线程执行器
        return new DefaultEventExecutor(this, executor, (Integer) args[0], (RejectedExecutionHandler) args[1]);
    }
}
