package io.netty.util.concurrent;

import io.netty.util.internal.UnstableApi;

/**
 * Factory that creates new {@link EventExecutorChooser}s.
 *
 * 创建新的EventExecutorChooserFactory.EventExecutorChoosers的工厂。
 *
 * 每一个工厂实现中都会实现 EventExecutorChooser 接口，然后返回该类型的 EventExecutorChooser
 *
 * 抽象工厂方法设计模式
 */
@UnstableApi
public interface EventExecutorChooserFactory {

    /**
     * Returns a new {@link EventExecutorChooser}.
     */
    EventExecutorChooser newChooser(EventExecutor[] executors);

    /**
     * Chooses the next {@link EventExecutor} to use.
     */
    @UnstableApi
    interface EventExecutorChooser {

        /**
         * Returns the new {@link EventExecutor} to use.
         *
         * 从 EventExecutor[] 数组中选择一个 EventExecutor 去执行任务
         */
        EventExecutor next();
    }
}
