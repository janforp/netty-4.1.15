package io.netty.util.concurrent;

/**
 * 拒绝策略
 * Similar to {@link java.util.concurrent.RejectedExecutionHandler} but specific to {@link SingleThreadEventExecutor}.
 */
public interface RejectedExecutionHandler {

    /**
     * Called when someone tried to add a task to {@link SingleThreadEventExecutor} but this failed due capacity
     * restrictions.
     *
     * 当有人尝试向SingleThreadEventExecutor中添加任务但由于容量限制而失败时调用。
     */
    void rejected(Runnable task, SingleThreadEventExecutor executor);
}
