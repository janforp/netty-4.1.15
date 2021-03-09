package io.netty.channel;

import io.netty.util.concurrent.OrderedEventExecutor;

/**
 * Will handle all the I/O operations for a {@link Channel} once registered.
 *
 * One {@link EventLoop} instance will usually handle more than one {@link Channel} but this may depend on
 * implementation details and internals.
 *
 * 注册后将处理通道的所有I / O操作。一个EventLoop实例通常将处理多个Channel，但这可能取决于实现细节和内部。
 *
 * 也就是说：一个 Channel 的所有操作都被同一个 EventLoop 处理
 *
 * 都是一个 EventLoop 可以处理多个 Channel
 */
public interface EventLoop extends OrderedEventExecutor, EventLoopGroup {

    /**
     * 返回该事件循环所在的组
     *
     * @return
     */
    @Override
    EventLoopGroup parent();
}