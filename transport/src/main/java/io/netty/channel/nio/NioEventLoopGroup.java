package io.netty.channel.nio;

import io.netty.channel.Channel;
import io.netty.channel.DefaultSelectStrategyFactory;
import io.netty.channel.EventLoop;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.SelectStrategyFactory;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorChooserFactory;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.RejectedExecutionHandlers;

import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * {@link MultithreadEventLoopGroup} implementations which is used for NIO {@link Selector} based {@link Channel}s.
 *
 * 只需要实现 MultithreadEventExecutorGroup 的 newChild 创建一个新的 EventLoop（NioEventLoop） 即可
 */
public class NioEventLoopGroup extends MultithreadEventLoopGroup {

    /**
     * 使用默认线程数，默认ThreadFactory和SelectorProvider.provider（）返回的SelectorProvider创建一个新实例。
     *
     * <p></p>
     * Create a new instance using the default number of threads, the default {@link ThreadFactory} and
     * the {@link SelectorProvider} which is returned by {@link SelectorProvider#provider()}.
     */
    public NioEventLoopGroup() {
        this(0);
    }

    /**
     * Create a new instance using the specified number of threads, {@link ThreadFactory} and the
     * {@link SelectorProvider} which is returned by {@link SelectorProvider#provider()}.
     */
    public NioEventLoopGroup(int nThreads) {
        //强制把 null 转换为对应的类型，是为了指定具体的构造器，否则，编译器不知道使用哪个构造器
        this(
                nThreads,
                (Executor) null
        );
    }

    /**
     * Create a new instance using the specified number of threads, the given {@link ThreadFactory} and the
     * {@link SelectorProvider} which is returned by {@link SelectorProvider#provider()}.
     */
    public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        this(nThreads, threadFactory, SelectorProvider.provider());
    }

    public NioEventLoopGroup(int nThreads, Executor executor) {
        //生成一个java原生的选择器
        this(
                nThreads, // 线程数量
                executor, // 执行器
                SelectorProvider.provider() // 选择器提供器，通过这个参数可以获取到jdk层面的 selector 实例(这是java nio 的知识)
        );
    }

    /**
     * Create a new instance using the specified number of threads, the given {@link ThreadFactory} and the given
     * {@link SelectorProvider}.
     */
    public NioEventLoopGroup(
            int nThreads, ThreadFactory threadFactory, final SelectorProvider selectorProvider) {
        this(nThreads, threadFactory, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
    }

    public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory,
            final SelectorProvider selectorProvider, final SelectStrategyFactory selectStrategyFactory) {
        super(nThreads, threadFactory, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
    }

    /**
     * 构造一个 实例
     *
     * @param nThreads 线程数（>= 0）
     * @param executor 线程池(可以为 null)
     * @param selectorProvider java原生的选择器提供者
     */
    public NioEventLoopGroup(int nThreads, Executor executor, final SelectorProvider selectorProvider) {
        /**
         * 传入一个默认的选择策略工厂实例(使用了抽象工厂模式)
         * @see NioEventLoopGroup#NioEventLoopGroup() 如果使用默认构造方法，则，这里的参数情况如下面的注释
         */
        this(
                nThreads, //0
                executor, //null
                selectorProvider,//SelectorProvider.provider()
                /**
                 * @see DefaultSelectStrategy
                 */
                DefaultSelectStrategyFactory.INSTANCE //选择器工作策略
        );
    }

    // 线程池组
    public NioEventLoopGroup(int nThreads, Executor executor, final SelectorProvider selectorProvider, final SelectStrategyFactory selectStrategyFactory) {
        /**
         * 传入一个拒绝策略：直接抛出移除
         * @see NioEventLoopGroup#NioEventLoopGroup() 如果使用默认构造方法，则，这里的参数情况如下面的注释
         */
        super(
                nThreads, //0
                executor, //null
                selectorProvider,//SelectorProvider.provider()
                /**
                 * @see DefaultSelectStrategy
                 */
                selectStrategyFactory, //DefaultSelectStrategyFactory.INSTANCE
                /**
                 * 线程池拒绝策略
                 * @see RejectedExecutionHandler 直接抛出异常
                 */
                RejectedExecutionHandlers.reject() // 直接抛出异常的策略
        );
    }

    public NioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory,
            final SelectorProvider selectorProvider,
            final SelectStrategyFactory selectStrategyFactory) {
        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory,
                RejectedExecutionHandlers.reject());
    }

    public NioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory,
            final SelectorProvider selectorProvider,
            final SelectStrategyFactory selectStrategyFactory,
            final RejectedExecutionHandler rejectedExecutionHandler) {
        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory, rejectedExecutionHandler);
    }

    /**
     * Sets the percentage of the desired amount of time spent for I/O in the child event loops.  The default value is
     * {@code 50}, which means the event loop will try to spend the same amount of time for I/O as for non-I/O tasks.
     */
    public void setIoRatio(int ioRatio) {
        for (EventExecutor e : this) {
            ((NioEventLoop) e).setIoRatio(ioRatio);
        }
    }

    /**
     * Replaces the current {@link Selector}s of the child event loops with newly created {@link Selector}s to work
     * around the  infamous epoll 100% CPU bug.
     */
    public void rebuildSelectors() {
        for (EventExecutor e : this) {
            ((NioEventLoop) e).rebuildSelector();
        }
    }

    /**
     * @param executor = new ThreadPerTaskExecutor(newDefaultThreadFactory()) {@link io.netty.util.concurrent.ThreadPerTaskExecutor }
     * @param args 由该类的构造器传入
     * @return 一个 EventExecutor 实例
     * @throws Exception
     * @see NioEventLoopGroup#NioEventLoopGroup()
     * @see MultithreadEventExecutorGroup#MultithreadEventExecutorGroup(int, java.util.concurrent.Executor, io.netty.util.concurrent.EventExecutorChooserFactory, java.lang.Object...)
     */
    @Override
    protected EventLoop newChild(

            /**
             * ThreadPerTaskExecutor 实例，这个实例里面包含一个 ThreadFactory 实例，
             * 创建出来的实例的类型为{@link io.netty.util.concurrent.FastThreadLocalThread}
             * @see io.netty.util.concurrent.ThreadPerTaskExecutor
             */
            Executor executor,

            /**
             * args[0]=selectProvider（选择器提供器，用于获取jdk层面的选择器selector实例）
             * args[1]=selectStrategy（选择器工作策略）
             * args[2]=线程池拒绝策略
             */
            Object... args) throws Exception {

        return new NioEventLoop(
                /**
                 * NioEventLoopGroup
                 */
                this,

                /**
                 * ThreadPerTaskExecutor 实例，这个实例里面包含一个 ThreadFactory 实例，
                 * 创建出来的实例的类型为{@link io.netty.util.concurrent.FastThreadLocalThread}
                 * @see io.netty.util.concurrent.ThreadPerTaskExecutor
                 */
                executor,
                //java
                (SelectorProvider) args[0],
                //
                ((SelectStrategyFactory) args[1]).newSelectStrategy(),
                //拒绝策略
                (RejectedExecutionHandler) args[2]
        );
    }
}
