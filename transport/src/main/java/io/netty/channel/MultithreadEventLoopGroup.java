package io.netty.channel;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorChooserFactory;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * Abstract base class for {@link EventLoopGroup} implementations that handles their tasks with multiple threads at
 * the same time.
 */
public abstract class MultithreadEventLoopGroup extends MultithreadEventExecutorGroup implements EventLoopGroup {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultithreadEventLoopGroup.class);

    /**
     * 如果用户没有指定线程数量，则使用该值
     */
    private static final int DEFAULT_EVENT_LOOP_THREADS;

    static {
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1,

                /**
                 * io.netty.eventLoopThreads：配置项
                 * 如果存在，则使用配置项，否则使用 NettyRuntime.availableProcessors() * 2 ( 8 核 则 16)
                 */
                SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));

        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.eventLoopThreads: {}", DEFAULT_EVENT_LOOP_THREADS);
        }
    }

    /**
     * 传入的值：
     *
     * nThreads, //0
     * executor, //null
     * selectorProvider,//SelectorProvider.provider()
     *
     * @param args selectStrategyFactory, RejectedExecutionHandlers.reject()
     * @see DefaultSelectStrategy
     * selectStrategyFactory, //DefaultSelectStrategyFactory.INSTANCE
     * @see RejectedExecutionHandler 直接抛出异常
     * RejectedExecutionHandlers.reject() // 直接抛出异常的策略
     * @see NioEventLoopGroup#NioEventLoopGroup(int, java.util.concurrent.Executor, java.nio.channels.spi.SelectorProvider, io.netty.channel.SelectStrategyFactory)
     * super(nThreads, executor, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
     * @see MultithreadEventExecutorGroup#MultithreadEventExecutorGroup(int, Executor, Object...)
     */
    protected MultithreadEventLoopGroup(
            int nThreads,
            Executor executor,

            /**
             * args[0]=selectProvider（选择器提供器，用于获取jdk层面的选择器selector实例）
             * @see java.nio.channels.spi.SelectorProvider
             *
             * args[1]=selectStrategy（选择器工作策略）
             * @see DefaultSelectStrategyFactory
             *
             * args[2]=线程池拒绝策略
             * @see io.netty.util.concurrent.RejectedExecutionHandler
             */
            Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, args);
    }

    /**
     * @see MultithreadEventExecutorGroup#MultithreadEventExecutorGroup(int, ThreadFactory, Object...)
     */
    protected MultithreadEventLoopGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, threadFactory, args);
    }

    /**
     * @see MultithreadEventExecutorGroup#MultithreadEventExecutorGroup(int, Executor,
     * EventExecutorChooserFactory, Object...)
     */
    protected MultithreadEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory,
            Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, chooserFactory, args);
    }

    @Override
    protected ThreadFactory newDefaultThreadFactory() {
        return new DefaultThreadFactory(getClass(), Thread.MAX_PRIORITY);
    }

    @Override
    public EventLoop next() {
        return (EventLoop) super.next();
    }

    @Override
    protected abstract EventLoop newChild(Executor executor, Object... args) throws Exception;

    @Override
    public ChannelFuture register(Channel channel) {
        EventLoop next = next();
        return next.register(channel);
    }

    @Override
    public ChannelFuture register(ChannelPromise promise) {
        return next().register(promise);
    }

    @Deprecated
    @Override
    public ChannelFuture register(Channel channel, ChannelPromise promise) {
        return next().register(channel, promise);
    }
}
