package io.netty.util.concurrent;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for {@link EventExecutor} implementations.
 *
 * 一个 EventExecutor 也是一个 EventExecutorGroup
 */
public abstract class AbstractEventExecutor extends AbstractExecutorService implements EventExecutor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractEventExecutor.class);

    /**
     * 优雅关闭的时候的默认参数
     */
    static final long DEFAULT_SHUTDOWN_QUIET_PERIOD = 2;

    /**
     * 优雅关闭的时候的默认参数
     */
    static final long DEFAULT_SHUTDOWN_TIMEOUT = 15;

    /**
     * 该线程池所在的线程池组 （一般情况下为 NioEventLoopGroup）
     *
     * @see NioEventLoopGroup
     */
    private final EventExecutorGroup parent;

    private final Collection<EventExecutor> selfCollection = Collections.<EventExecutor>singleton(this);

    protected AbstractEventExecutor() {
        //子类调用
        this(null);
    }

    /**
     * @param parent （一般情况下为NioEventLoopGroup）
     */
    protected AbstractEventExecutor(EventExecutorGroup parent) {
        //子类调用
        this.parent = parent;
    }

    @Override
    public EventExecutorGroup parent() {
        return parent;
    }

    @Override
    public EventExecutor next() {
        return this;
    }

    /**
     * @return
     * @see SingleThreadEventExecutor#inEventLoop(java.lang.Thread)
     */
    @Override
    public boolean inEventLoop() {
        //内部这个方法由具体的实现子类实现
        Thread currentThread = Thread.currentThread();
        return inEventLoop(currentThread);
    }

    @Override
    public Iterator<EventExecutor> iterator() {
        //返回一个元素的迭代器
        return selfCollection.iterator();
    }

    @Override
    public Future<?> shutdownGracefully() {
        //内部这个方法由具体的实现子类实现
        return shutdownGracefully(DEFAULT_SHUTDOWN_QUIET_PERIOD, DEFAULT_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
     */
    @Override
    @Deprecated
    public abstract void shutdown();

    /**
     * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
     */
    @Override
    @Deprecated
    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList();
    }

    @Override
    public <V> Promise<V> newPromise() {
        //返回一个默认的 Promise 实现
        return new DefaultPromise<V>(this);
    }

    @Override
    public <V> ProgressivePromise<V> newProgressivePromise() {
        return new DefaultProgressivePromise<V>(this);
    }

    @Override
    public <V> Future<V> newSucceededFuture(V result) {
        return new SucceededFuture<V>(this, result);
    }

    @Override
    public <V> Future<V> newFailedFuture(Throwable cause) {
        return new FailedFuture<V>(this, cause);
    }

    @Override
    public Future<?> submit(Runnable task) {
        //直接调用 JUC 方法
        return (Future<?>) super.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        //直接调用 JUC 方法
        return (Future<T>) super.submit(task, result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        //直接调用 JUC 方法
        return (Future<T>) super.submit(task);
    }

    @Override
    protected final <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        //模版方法模式，直接替换了 JUC 中的该方法
        return new PromiseTask<T>(this, runnable, value);
    }

    @Override
    protected final <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new PromiseTask<T>(this, callable);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay,
            TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    /**
     * Try to execute the given {@link Runnable} and just log if it throws a {@link Throwable}.
     */
    protected static void safeExecute(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            logger.warn("A task raised an exception. Task: {}", task, t);
        }
    }
}
