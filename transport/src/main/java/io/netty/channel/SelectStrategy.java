package io.netty.channel;

import io.netty.util.IntSupplier;

/**
 * Select strategy interface.
 *
 * Provides the ability to control the behavior of the select loop. For example a blocking select
 * operation can be delayed or skipped entirely if there are events to process immediately.
 *
 * 选择策略界面。提供控制选择循环行为的功能。例如，如果有事件要立即处理，则阻塞选择操作可以被延迟或完全跳过。
 */
public interface SelectStrategy {

    /**
     * Indicates a blocking select should follow.
     * 指示应遵循阻止选择。
     * 阻塞
     */
    int SELECT = -1;
    /**
     * Indicates the IO loop should be retried, no blocking select to follow directly.
     *
     * 表示应重试IO循环，没有阻塞选择可直接执行。
     */
    int CONTINUE = -2;

    /**
     * The {@link SelectStrategy} can be used to steer the outcome of a potential select
     * call.
     *
     * @param selectSupplier The supplier with the result of a select result.
     * @param hasTasks true if tasks are waiting to be processed.
     * @return {@link #SELECT} if the next step should be blocking select {@link #CONTINUE} if
     * the next step should be to not select but rather jump back to the IO loop and try
     * again. Any value >= 0 is treated as an indicator that work needs to be done.
     */
    int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception;
}
