package io.netty.util.concurrent;

/**
 * Marker interface for {@link EventExecutor}s that will process all submitted tasks in an ordered / serial fashion.
 *
 * 标志任务可以按顺序执行
 */
public interface OrderedEventExecutor extends EventExecutor {

}
