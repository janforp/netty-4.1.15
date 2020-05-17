package io.netty.util.concurrent;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class for {@link EventExecutorGroup} implementations that handles their tasks with multiple threads at
 * the same time.
 * EventExecutorGroup实现的抽象基类，可同时处理多个线程的任务。
 *
 *
 * 子类只需要实现方法：protected abstract EventExecutor newChild(Executor executor, Object... args) throws Exception;
 */
public abstract class MultithreadEventExecutorGroup extends AbstractEventExecutorGroup {

    /**
     * children = new EventExecutor[nThreads];
     *
     * 数组大小由用户传入的线程数量而定，如果没指定，则使用系统默认的值
     *
     * 其中的每一个元素为：NioEventLoop
     *
     * <p></p>
     * the threads that will be used by this instance
     */
    private final EventExecutor[] children;

    /**
     * EventExecutor[] children 的不可变的副本
     */
    private final Set<EventExecutor> readonlyChildren;

    /**
     * ·用于记录 所有 NioEventLoop 的终止数量
     */
    private final AtomicInteger terminatedChildren = new AtomicInteger();

    private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);

    /**
     * @see DefaultEventExecutorChooserFactory
     */
    private final EventExecutorChooserFactory.EventExecutorChooser chooser;

    /**
     * Create a new instance.
     *
     * @param nThreads the number of threads that will be used by this instance.
     * @param threadFactory the ThreadFactory to use, or {@code null} if the default should be used.
     * @param args arguments which will passed to each {@link #newChild(Executor, Object...)} call
     */
    protected MultithreadEventExecutorGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
        this(nThreads, threadFactory == null ? null : new ThreadPerTaskExecutor(threadFactory), args);
    }

    /**
     * (nThreads, executor, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject())
     *
     * <p></p>
     *
     * Create a new instance.
     *
     * @param nThreads the number of threads that will be used by this instance.
     * @param executor the Executor to use, or {@code null} if the default should be used.(要使用的执行器；如果应使用默认值，则为null)
     * @param args arguments which will passed to each {@link #newChild(Executor, Object...)} call
     *
     * 一般为：（selectStrategyFactory, RejectedExecutionHandlers.reject()）
     */
    protected MultithreadEventExecutorGroup(int nThreads, Executor executor, Object... args) {
        this(nThreads, executor, DefaultEventExecutorChooserFactory.INSTANCE, args);
    }

    /**
     * Create a new instance.
     *
     * @param nThreads the number of threads that will be used by this instance.
     * @param executor the Executor to use, or {@code null} if the default should be used.
     * @param chooserFactory the {@link EventExecutorChooserFactory} to use.
     * @param args arguments which will passed to each {@link #newChild(Executor, Object...)} call
     *
     * 一般为：EventExecutorChooserFactory chooserFactory,selectStrategyFactory, RejectedExecutionHandlers.reject()
     */
    protected MultithreadEventExecutorGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, Object... args) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException(String.format("nThreads: %d (expected: > 0)", nThreads));
        }

        //确保 executor 被正确赋值
        if (executor == null) {
            //如果用户没有指定类型，则使用默认的 Executor 类型
            //默认的线程工厂：DefaultThreadFactory
            //默认的Executor：ThreadPerTaskExecutor
            //用户只需要传入一个 Runnable，该执行器则会生成一个线程去执行该任务
            executor = new ThreadPerTaskExecutor(newDefaultThreadFactory());
        }

        //创建事件处理器数组
        children = new EventExecutor[nThreads];

        //根据线程数量循环实例化 EventExecutor
        for (int i = 0; i < nThreads; i++) {
            //每次循环创建执行器是否成功的标志
            boolean success = false;
            try {
                /**
                 * 由具体子类实现的 newChild 逻辑，模版方法设计模式
                 *
                 * 第二个参数：selectStrategyFactory, RejectedExecutionHandlers.reject()）
                 *
                 * @see io.netty.channel.nio.NioEventLoopGroup#newChild(java.util.concurrent.Executor, java.lang.Object...)
                 */
                children[i] = newChild(executor, args);
                success = true;
            } catch (Exception e) {
                // TODO: Think about if this is a good exception type
                throw new IllegalStateException("failed to create a child event loop", e);
            } finally {
                //如果其中一个创建失败
                if (!success) {
                    for (int j = 0; j < i; j++) {
                        //关闭之前创建成功的
                        children[j].shutdownGracefully();
                    }

                    for (int j = 0; j < i; j++) {
                        EventExecutor e = children[j];
                        try {
                            while (!e.isTerminated()) {
                                //保证终止
                                e.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
                            }
                        } catch (InterruptedException interrupted) {
                            // Let the caller handle the interruption.
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }

        //所有的执行器都创建完毕之后的逻辑

        //实例化选择器
        chooser = chooserFactory.newChooser(children);

        /**
         * 实例化一个监听器
         * 用于监听每一个子 EventLoop 是不是已经终止
         */
        final FutureListener<Object> terminationListener = new FutureListener<Object>() {
            @Override
            public void operationComplete(Future<Object> future) throws Exception {
                //成员变量
                if (terminatedChildren.incrementAndGet() == children.length) {
                    //当所有的执行器都关闭的时候，就发出通知
                    terminationFuture.setSuccess(null);
                }
            }
        };

        //循环初始化每一个 EventLoop 的终止回调器
        for (EventExecutor e : children) {
            /**
             * 把 terminationListener 分别注册到每一个执行器
             * 这样的话在关闭的时候就会通过该 terminationListener 通知所有的执行器了
             * @see SingleThreadEventExecutor#terminationFuture
             */
            e.terminationFuture().addListener(terminationListener);
        }

        Set<EventExecutor> childrenSet = new LinkedHashSet<EventExecutor>(children.length);
        //把 EventExecutor[] children 全部加入到 Set<EventExecutor> childrenSet
        Collections.addAll(childrenSet, children);
        readonlyChildren = Collections.unmodifiableSet(childrenSet);
    }

    protected ThreadFactory newDefaultThreadFactory() {
        return new DefaultThreadFactory(getClass());
    }

    @Override
    public EventExecutor next() {
        return chooser.next();
    }

    @Override
    public Iterator<EventExecutor> iterator() {
        //不可变
        return readonlyChildren.iterator();
    }

    /**
     * Return the number of {@link EventExecutor} this implementation uses. This number is the maps
     * 1:1 to the threads it use.
     */
    public final int executorCount() {
        return children.length;
    }

    /**
     * Create a new EventExecutor which will later then accessible via the {@link #next()}  method. This method will be
     * called for each thread that will serve this {@link MultithreadEventExecutorGroup}.
     */
    protected abstract EventExecutor newChild(Executor executor, Object... args) throws Exception;

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        for (EventExecutor l : children) {
            l.shutdownGracefully(quietPeriod, timeout, unit);
        }
        return terminationFuture();
    }

    @Override
    public Future<?> terminationFuture() {
        return terminationFuture;
    }

    @Override
    @Deprecated
    public void shutdown() {
        for (EventExecutor l : children) {
            l.shutdown();
        }
    }

    @Override
    public boolean isShuttingDown() {
        for (EventExecutor l : children) {
            if (!l.isShuttingDown()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isShutdown() {
        for (EventExecutor l : children) {
            if (!l.isShutdown()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isTerminated() {
        for (EventExecutor l : children) {
            if (!l.isTerminated()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        loop:
        for (EventExecutor l : children) {
            for (; ; ) {
                long timeLeft = deadline - System.nanoTime();
                if (timeLeft <= 0) {
                    break loop;
                }
                if (l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS)) {
                    break;
                }
            }
        }
        return isTerminated();
    }
}
