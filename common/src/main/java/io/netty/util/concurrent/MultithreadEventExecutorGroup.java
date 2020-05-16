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
     * the threads that will be used by this instance
     */
    private final EventExecutor[] children;

    /**
     * EventExecutor[] children 的不可变的副本
     */
    private final Set<EventExecutor> readonlyChildren;

    private final AtomicInteger terminatedChildren = new AtomicInteger();

    private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);

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
     * Create a new instance.
     *
     * @param nThreads the number of threads that will be used by this instance.
     * @param executor the Executor to use, or {@code null} if the default should be used.
     * @param args arguments which will passed to each {@link #newChild(Executor, Object...)} call
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
     */
    protected MultithreadEventExecutorGroup(int nThreads, Executor executor,
            EventExecutorChooserFactory chooserFactory, Object... args) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException(String.format("nThreads: %d (expected: > 0)", nThreads));
        }

        if (executor == null) {
            executor = new ThreadPerTaskExecutor(newDefaultThreadFactory());
        }

        children = new EventExecutor[nThreads];

        for (int i = 0; i < nThreads; i++) {
            //每次循环创建执行器是否成功的标志
            boolean success = false;
            try {
                //由具体子类实现的 newChild 逻辑
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

        chooser = chooserFactory.newChooser(children);

        final FutureListener<Object> terminationListener = new FutureListener<Object>() {
            @Override
            public void operationComplete(Future<Object> future) throws Exception {
                if (terminatedChildren.incrementAndGet() == children.length) {
                    //当所有的执行器都关闭的时候，就发出通知
                    terminationFuture.setSuccess(null);
                }
            }
        };

        for (EventExecutor e : children) {
            //把 terminationListener 分别注册到每一个执行器
            //这样的话在关闭的时候就会通过该 terminationListener 通知所有的执行器了
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
