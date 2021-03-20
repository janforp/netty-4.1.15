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
     运行任务来处理在连接的生命周期内发生的事件是任何网络框架的基本功能。
     与之相应的编 程上的构造通常被称为事件循环 ---- 一个
     Netty 使用了 interface io.netty.channel. EventLoop 来适配的术语。
     *
     *      * 在事件循环中执行任务,如下代码：
     *
     *      * while (!terminated) {
     *      *      List<Runnable> readyEvents = blockUntilEventsReady();        //阻塞，直到有事件已经就绪，可以被运行
     *      *      for (Runnable ev: readyEvents) {
     *      *          ev.run();                                                //循环遍历，并处理所有的事件
     *      *      }
     *      * }
     *
     * 在这个模型中，一个 EventLoop 将由一个永远都不会改变的 Thread 驱动，
     * 同时任务 (Runnable 或者 Callable)可以直接提交给 EventLoop 实现，
     * 以立即执行或者调度执行。 根据配置和可用核心的不同，
     * 可能会创建多个 EventLoop 实例用以优化资源的使用，并且单个
     * EventLoop 可能会被指派用于服务多个 Channel。
     *
     * 事件和任务是以先进先出(FIFO)的顺序执行的。这样可以通过保证字 节内容总是按正确的顺序被处理，消除潜在的数据损坏的可能性
     *
     *
     * Netty 4 中所采用的线程模型，通过在同一个线程中处理某个给定的 EventLoop 中所产生的 所有事件，解决了这个问题。
     * 这提供了一个更加简单的执行体系架构，并且消除了在多个 ChannelHandler 中进行同步的需要(除了任何可能需要在多个 Channel 中共享的)。
     *
     */

    /**
     * 返回该事件循环所在的组
     *
     * 用于返回到当前EventLoop实现的实例所属的EventLoopGroup的引用。
     *
     * @return
     */
    @Override
    EventLoopGroup parent();
}