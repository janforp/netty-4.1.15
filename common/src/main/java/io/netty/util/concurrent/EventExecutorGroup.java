package io.netty.util.concurrent;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The {@link EventExecutorGroup} is responsible for providing the {@link EventExecutor}'s to use
 * via its {@link #next()} method. Besides this, it is also responsible for handling their
 * life-cycle and allows shutting them down in a global fashion.
 * 事件执行器 组，顾名思义：内部维护了一些事件执行器 （EventExecutor），而每个EventExecutor也继承了 EventExecutorGroup，是不是可以
 * TODO 理解成每个单独的 EventExecutor 是一个有且只有一个 EventExecutor 的 EventExecutorGroup？
 */
public interface EventExecutorGroup extends ScheduledExecutorService, Iterable<EventExecutor> {

    /**
     * Returns {@code true} if and only if all {@link EventExecutor}s managed by this {@link EventExecutorGroup}
     * are being {@linkplain #shutdownGracefully() shut down gracefully} or was {@linkplain #isShutdown() shut down}.
     *
     * 该事件执行器组中的每一个执行器都关闭的时候该组就关闭
     */
    boolean isShuttingDown();

    /**
     * Shortcut method for {@link #shutdownGracefully(long, long, TimeUnit)} with sensible default values.
     *
     * @return the {@link #terminationFuture()}
     */
    Future<?> shutdownGracefully();

    /**
     * Signals this executor that the caller wants the executor to be shut down.  Once this method is called,
     * {@link #isShuttingDown()} starts to return {@code true}, and the executor prepares to shut itself down.
     * Unlike {@link #shutdown()}, graceful shutdown ensures that no tasks are submitted for <i>'the quiet period'</i>
     * (usually a couple seconds) before it shuts itself down.  If a task is submitted during the quiet period,
     * it is guaranteed to be accepted and the quiet period will start over.
     *
     * @param quietPeriod the quiet period as described in the documentation
     * @param timeout the maximum amount of time to wait until the executor is {@linkplain #shutdown()}
     * regardless if a task was submitted during the quiet period
     * @param unit the unit of {@code quietPeriod} and {@code timeout}
     * @return the {@link #terminationFuture()}
     */
    Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit);

    /**
     * Returns the {@link Future} which is notified when all {@link EventExecutor}s managed by this
     * {@link EventExecutorGroup} have been terminated.
     *
     * 通过该方法获取的 Future ，当所有的 EventExecutor 都终止的时候会被通知
     */
    Future<?> terminationFuture();

    /**
     * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
     */
    @Override
    @Deprecated
    void shutdown();

    /**
     * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
     */
    @Override
    @Deprecated
    List<Runnable> shutdownNow();

    /**
     * Returns one of the {@link EventExecutor}s managed by this {@link EventExecutorGroup}.
     * 获取下一个事件执行器
     */
    EventExecutor next();

    /**
     * Iterable 接口
     *
     * @return
     */
    @Override
    Iterator<EventExecutor> iterator();

    //下面是 ScheduledExecutorService 的接口，返回值与原接口不一样，但是只要类型更严格（具体）就可以
    //此处返回的都是 netty 自定义扩展了java的接口

    @Override
    Future<?> submit(Runnable task);

    @Override
    <T> Future<T> submit(Runnable task, T result);

    @Override
    <T> Future<T> submit(Callable<T> task);

    @Override
    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

    @Override
    <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

    /**
     * * Channel ch = ...
     * * ScheduledFuture<?> future = ch.eventLoop().scheduleAtFixedRate(
     * *           new Runnable() {
     * *              @Override
     * *              public void run() {
     * *                  System.out.println("Run every 60 seconds");
     * *              }
     * *           }, 60, 60, TimeUnit.Seconds);
     *
     * 调度在 60 秒之后，并且 以后每间隔 60 秒运行
     * 将一直运行，直到 future 被取消
     *
     * 如果要调度任
     * 务以每隔 60 秒执行一次，请使用 scheduleAtFixedRate()方法
     */
    @Override
    ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);

    @Override
    ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);
}
