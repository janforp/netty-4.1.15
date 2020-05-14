package io.netty.util.concurrent;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
final class ScheduledFutureTask<V> extends PromiseTask<V> implements ScheduledFuture<V> {

    /**
     * 原子自增id，这个属性是全局唯一的
     */
    private static final AtomicLong nextTaskId = new AtomicLong();

    /**
     * 实例化该类的时候的时间
     */
    private static final long START_TIME = System.nanoTime();

    /**
     * 当前时间 - START_TIME
     *
     * @return
     */
    static long nanoTime() {
        return System.nanoTime() - START_TIME;
    }

    /**
     * 当前时间 - START_TIME + delay
     *
     * @param delay
     * @return
     */
    static long deadlineNanos(long delay) {
        return nanoTime() + delay;
    }

    /**
     * 这个属性是每个对象都会有的，所以每次实例化一个该类的对象的时候，该id++，因为 nextTaskId 是全局唯一的
     */
    private final long id = nextTaskId.getAndIncrement();

    private long deadlineNanos;

    /* 0 - no repeat, >0 - repeat at fixed rate, <0 - repeat with fixed delay */
    //0：不重复

    /**
     * 周期：
     * 0：不重复
     * >0：固定频率重复
     * <0：每次执行之后延迟一段时间在执行
     */
    private final long periodNanos;

    ScheduledFutureTask(
            AbstractScheduledEventExecutor executor,
            Runnable runnable, V result, long nanoTime) {

        this(executor, toCallable(runnable, result), nanoTime);
    }

    ScheduledFutureTask(
            AbstractScheduledEventExecutor executor,
            Callable<V> callable, long nanoTime, long period) {

        super(executor, callable);
        if (period == 0) {
            throw new IllegalArgumentException("period: 0 (expected: != 0)");
        }
        deadlineNanos = nanoTime;
        periodNanos = period;
    }

    /**
     * @param executor
     * @param callable Runnable runnable, V result -> toCallable(Runnable runnable, T result)
     * @param nanoTime
     */
    ScheduledFutureTask(
            AbstractScheduledEventExecutor executor,
            Callable<V> callable, long nanoTime) {

        super(executor, callable);
        deadlineNanos = nanoTime;
        //初始化，不重复
        periodNanos = 0;
    }

    @Override
    protected EventExecutor executor() {
        return super.executor();
    }

    public long deadlineNanos() {
        return deadlineNanos;
    }

    /**
     * 获取该任务延迟的时间，最小是0
     *
     * 延迟到的时间戳（传进来的一个未来的时刻） - 当前时间 + START_TIME
     *
     * @return
     */
    public long delayNanos() {
        return Math.max(0, deadlineNanos() - nanoTime());
    }

    /**
     * 获取从当前时间开始的延迟时间
     *
     * @param currentTimeNanos
     * @return
     */
    public long delayNanos(long currentTimeNanos) {
        return Math.max(0, deadlineNanos() - (currentTimeNanos - START_TIME));
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(delayNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (this == o) {
            return 0;
        }

        //先按时间比较，如果时间一样按id比较，id不可能是一样的
        ScheduledFutureTask<?> that = (ScheduledFutureTask<?>) o;
        long d = deadlineNanos() - that.deadlineNanos();
        if (d < 0) {
            return -1;
        } else if (d > 0) {
            return 1;
        } else if (id < that.id) {
            return -1;
        } else if (id == that.id) {
            throw new Error();
        } else {
            return 1;
        }
    }

    @Override
    public void run() {
        assert executor().inEventLoop();
        try {
            //0 - no repeat, >0 - repeat at fixed rate, <0 - repeat with fixed delay
            if (periodNanos == 0) {
                //只执行一次
                if (setUncancellableInternal()) {
                    V result = task.call();
                    //设置成功
                    setSuccessInternal(result);
                }
            } else {
                // check if is done as it may was cancelled
                if (!isCancelled()) {
                    task.call();
                    if (!executor().isShutdown()) {
                        long p = periodNanos;
                        if (p > 0) {
                            deadlineNanos += p;
                        } else {// p <0
                            deadlineNanos = nanoTime() - p;
                        }
                        if (!isCancelled()) {
                            // scheduledTaskQueue can never be null as we lazy init it before submit the task!
                            Queue<ScheduledFutureTask<?>> scheduledTaskQueue =
                                    ((AbstractScheduledEventExecutor) executor()).scheduledTaskQueue;
                            assert scheduledTaskQueue != null;
                            //把当前任务添加到队列中
                            scheduledTaskQueue.add(this);
                        }
                    }
                }
            }
        } catch (Throwable cause) {
            setFailureInternal(cause);
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean canceled = super.cancel(mayInterruptIfRunning);
        if (canceled) {
            ((AbstractScheduledEventExecutor) executor()).removeScheduled(this);
        }
        return canceled;
    }

    boolean cancelWithoutRemove(boolean mayInterruptIfRunning) {
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    protected StringBuilder toStringBuilder() {
        StringBuilder buf = super.toStringBuilder();
        buf.setCharAt(buf.length() - 1, ',');

        return buf.append(" id: ")
                .append(id)
                .append(", deadline: ")
                .append(deadlineNanos)
                .append(", period: ")
                .append(periodNanos)
                .append(')');
    }
}
