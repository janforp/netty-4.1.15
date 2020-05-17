package io.netty.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

/**
 * JDK所提供的 Future 只能通过手工的方式检查执行结果，而这个操作是会阻塞的；netty 则对 ChannelFuture 进行了增强，通过 ChannelFutureListener 以回调的方式来获取执行结果，
 * 去除了手工检查阻塞的操作；值得注意的是：ChannelFutureListener 的 operationComplete 方法是由 I/O 线程执行的，因此，要注意的是不要在这里执行耗时操作，否则需要通过另外的
 * 线程或线程池来执行
 */


/**
 * The result of an asynchronous operation.
 *
 * 该对象封装了一个异步操作的结果，扩展了java原生的 Future
 *
 * 使用了观察者模式，所有的监听器一般都存储在一个集合或者其他对象中，当时间发生的时候由主题对象来遍历这些观察者进行回调
 */
@SuppressWarnings("ClassNameSameAsAncestorName")
public interface Future<V> extends java.util.concurrent.Future<V> {

    /**
     * Returns {@code true} if and only if the I/O operation was completed
     * successfully.
     */
    boolean isSuccess();

    /**
     * returns {@code true} if and only if the operation can be cancelled via {@link #cancel(boolean)}.
     */
    boolean isCancellable();

    /**
     * Returns the cause of the failed I/O operation if the I/O operation has
     * failed.
     *
     * @return the cause of the failure.
     * {@code null} if succeeded or this future is not
     * completed yet.
     *
     * 只有真正失败的时候次方法返回才有对象，否则 null
     */
    Throwable cause();

    /**
     * Adds the specified listener to this future.  The
     * specified listener is notified when this future is
     * {@linkplain #isDone() done}.  If this future is already
     * completed, the specified listener is notified immediately.
     *
     * 如果在添加的时候已经完成，则会立刻通知，这可以理解为该接口定义的一个规范，要求实现必须满足此规范
     */
    Future<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * Adds the specified listeners to this future.  The
     * specified listeners are notified when this future is
     * {@linkplain #isDone() done}.  If this future is already
     * completed, the specified listeners are notified immediately.
     *
     * 如果在添加的时候已经完成，则会立刻通知，这可以理解为该接口定义的一个规范，要求实现必须满足此规范
     */
    Future<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * Removes the first occurrence of the specified listener from this future.
     * The specified listener is no longer notified when this
     * future is {@linkplain #isDone() done}.  If the specified
     * listener is not associated with this future, this method
     * does nothing and returns silently.
     *
     * 从此 future 中删除第一次出现的指定侦听器。
     */
    Future<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * Removes the first occurrence for each of the listeners from this future.
     * The specified listeners are no longer notified when this
     * future is {@linkplain #isDone() done}.  If the specified
     * listeners are not associated with this future, this method
     * does nothing and returns silently.
     */
    Future<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.
     *
     * 阻塞到该异步操作完成，如果失败则会重新抛出异常
     */
    Future<V> sync() throws InterruptedException;

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.
     */
    Future<V> syncUninterruptibly();

    /**
     * Waits for this future to be completed.
     * 等待的时候如果线程终止了，则会抛出异常
     *
     * @throws InterruptedException if the current thread was interrupted
     */
    Future<V> await() throws InterruptedException;

    /**
     * Waits for this future to be completed without
     * interruption.  This method catches an {@link InterruptedException} and
     * discards it silently.
     *
     * 等待的时候如果线程终止了，则不会抛出异常
     *
     * 等待这个 Future 完成而不中断。此方法捕获InterruptedException并以静默方式将其丢弃。
     */
    Future<V> awaitUninterruptibly();

    /**
     * Waits for this future to be completed within the
     * specified time limit.
     *
     * @return {@code true} if and only if the future was completed within
     * the specified time limit
     * @throws InterruptedException if the current thread was interrupted
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Waits for this future to be completed within the
     * specified time limit.
     *
     * @return {@code true} if and only if the future was completed within
     * the specified time limit
     * @throws InterruptedException if the current thread was interrupted
     */
    boolean await(long timeoutMillis) throws InterruptedException;

    /**
     * Waits for this future to be completed within the
     * specified time limit without interruption.  This method catches an
     * {@link InterruptedException} and discards it silently.
     *
     * @return {@code true} if and only if the future was completed within
     * the specified time limit
     */
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);

    /**
     * Waits for this future to be completed within the
     * specified time limit without interruption.  This method catches an
     * {@link InterruptedException} and discards it silently.
     *
     * @return {@code true} if and only if the future was completed within
     * the specified time limit
     */
    boolean awaitUninterruptibly(long timeoutMillis);

    /**
     * Return the result without blocking. If the future is not done yet this will return {@code null}.
     *
     * As it is possible that a {@code null} value is used to mark the future as successful you also need to check
     * if the future is really done with {@link #isDone()} and not relay on the returned {@code null} value.
     *
     * 完成则返回完成的值，否则返回null，但是真正完成的情况下也可能返回一个null，所以不能通过该方法的返回值去判断是否完成
     * 而是需要通过方法 isDone() 去进行判断
     */
    V getNow();

    /**
     * {@inheritDoc}
     *
     * If the cancellation was successful it will fail the future with an {@link CancellationException}.
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning);
}
