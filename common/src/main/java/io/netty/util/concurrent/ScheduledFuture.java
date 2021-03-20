package io.netty.util.concurrent;

/**
 * The result of an scheduled asynchronous operation.
 *
 * 要想取消或者检查(被调度任务的)执行状态，可以使用每个异步操作所返回的 ScheduledFuture。
 */
@SuppressWarnings("ClassNameSameAsAncestorName")
public interface ScheduledFuture<V>

        /**
         * netty 在 juc 的 Future 基础之上进行了扩展
         */
        extends Future<V>,
        java.util.concurrent.ScheduledFuture<V> {

    /**
     * ScheduledFuture<?> future = ch.eventLoop().scheduleAtFixedRate(...);
     * boolean mayInterruptIfRunning = false;
     *
     * //取消任务，防止它再次运行
     * future.cancel(mayInterruptIfRunning);
     */
}