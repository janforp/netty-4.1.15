package io.netty.channel;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * This implementation allows to register {@link ChannelFuture} instances which will get notified once some amount of
 * data was written and so a checkpoint was reached.
 *
 * 此实现允许注册ChannelFuture实例，该实例将在写入一定数量的数据并因此到达检查点时得到通知。
 */
public final class ChannelFlushPromiseNotifier {

    private long writeCounter;

    /**
     * 一些通过add方法添加的 ChannelFuture，他们会实现 FlushCheckpoint 或者被封装到一个 FlushCheckpoint 实例中
     */
    private final Queue<FlushCheckpoint> flushCheckpoints = new ArrayDeque<FlushCheckpoint>();

    /**
     * 如果为true，则将通过ChannelPromise.trySuccess（）和ChannelPromise.tryFailure（Throwable）通知ChannelPromises。
     * 否则，将使用ChannelPromise.setSuccess（）和ChannelPromise.setFailure（Throwable）
     */
    private final boolean tryNotify;

    /**
     * Create a new instance
     *
     * @param tryNotify if {@code true} the {@link ChannelPromise}s will get notified with
     * {@link ChannelPromise#trySuccess()} and {@link ChannelPromise#tryFailure(Throwable)}.
     * Otherwise {@link ChannelPromise#setSuccess()} and {@link ChannelPromise#setFailure(Throwable)}
     * is used
     *
     * 如果为true，则将通过ChannelPromise.trySuccess（）和ChannelPromise.tryFailure（Throwable）通知ChannelPromises。
     * 否则，将使用ChannelPromise.setSuccess（）和ChannelPromise.setFailure（Throwable）
     */
    public ChannelFlushPromiseNotifier(boolean tryNotify) {
        this.tryNotify = tryNotify;
    }

    /**
     * Create a new instance which will use {@link ChannelPromise#setSuccess()} and
     * {@link ChannelPromise#setFailure(Throwable)} to notify the {@link ChannelPromise}s.
     */
    public ChannelFlushPromiseNotifier() {
        this(false);
    }

    /**
     * @deprecated use {@link #add(ChannelPromise, long)}
     */
    @Deprecated
    public ChannelFlushPromiseNotifier add(ChannelPromise promise, int pendingDataSize) {
        return add(promise, (long) pendingDataSize);
    }

    /**
     * Add a {@link ChannelPromise} to this {@link ChannelFlushPromiseNotifier} which will be notified after the given
     * {@code pendingDataSize} was reached.
     *
     * @param promise 注册的 ChannelFuture 的实例（ChannelPromise 扩展自 ChannelFuture）,不能空
     * @param pendingDataSize 待处理数据大小，必须要 >= 0
     * @return 此实现允许注册ChannelFuture实例，该实例将在写入一定数量的数据并因此到达检查点时得到通知。
     */
    public ChannelFlushPromiseNotifier add(ChannelPromise promise, long pendingDataSize) {
        if (promise == null) {
            throw new NullPointerException("promise");
        }
        if (pendingDataSize < 0) {
            throw new IllegalArgumentException("pendingDataSize must be >= 0 but was " + pendingDataSize);
        }
        //TODO 检查点，当写入的数据量达到该值的时候就会通知?
        long checkpoint = writeCounter + pendingDataSize;

        //如果传入的 promise 实现了 FlushCheckpoint 接口，则可以直接使用
        if (promise instanceof FlushCheckpoint) {
            FlushCheckpoint cp = (FlushCheckpoint) promise;
            //把该 promise 的通知数据保存进去
            cp.flushCheckpoint(checkpoint);
            //放入一个双端队列
            flushCheckpoints.add(cp);
        } else {
            //如果传入的 promise 没有实现 FlushCheckpoint 接口，把该 promise 封装到 DefaultFlushCheckpoint 中
            flushCheckpoints.add(new DefaultFlushCheckpoint(checkpoint, promise));
        }
        return this;
    }

    /**
     * Increase the current write counter by the given delta
     *
     * @param delta 增量值
     * @return chain return this
     */
    public ChannelFlushPromiseNotifier increaseWriteCounter(long delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("delta must be >= 0 but was " + delta);
        }
        writeCounter += delta;
        return this;
    }

    /**
     * Return the current write counter of this {@link ChannelFlushPromiseNotifier}
     */
    public long writeCounter() {
        return writeCounter;
    }

    /**
     * Notify all {@link ChannelFuture}s that were registered with {@link #add(ChannelPromise, int)} and
     * their pendingDataSize is smaller after the the current writeCounter returned by {@link #writeCounter()}.
     *
     * After a {@link ChannelFuture} was notified it will be removed from this {@link ChannelFlushPromiseNotifier} and
     * so not receive anymore notification.
     *
     * 通知由add（ChannelPromise，int）注册的所有ChannelFuture，并且在writeCounter（）返回的当前writeCounter之后，
     * 它们的pendingDataSize会变小。通知ChannelFuture之后，它将被从此ChannelFlushPromiseNotifier中删除，因此不再收到通知。
     */
    public ChannelFlushPromiseNotifier notifyPromises() {
        notifyPromises0(null);
        return this;
    }

    /**
     * @deprecated use {@link #notifyPromises()}
     */
    @Deprecated
    public ChannelFlushPromiseNotifier notifyFlushFutures() {
        return notifyPromises();
    }

    /**
     * Notify all {@link ChannelFuture}s that were registered with {@link #add(ChannelPromise, int)} and
     * their pendingDataSize isis smaller then the current writeCounter returned by {@link #writeCounter()}.
     *
     * After a {@link ChannelFuture} was notified it will be removed from this {@link ChannelFlushPromiseNotifier} and
     * so not receive anymore notification.
     *
     * The rest of the remaining {@link ChannelFuture}s will be failed with the given {@link Throwable}.
     *
     * So after this operation this {@link ChannelFutureListener} is empty.
     */
    public ChannelFlushPromiseNotifier notifyPromises(Throwable cause) {
        notifyPromises();
        for (; ; ) {
            FlushCheckpoint cp = flushCheckpoints.poll();
            if (cp == null) {
                break;
            }
            if (tryNotify) {
                cp.promise().tryFailure(cause);
            } else {
                cp.promise().setFailure(cause);
            }
        }
        return this;
    }

    /**
     * @deprecated use {@link #notifyPromises(Throwable)}
     */
    @Deprecated
    public ChannelFlushPromiseNotifier notifyFlushFutures(Throwable cause) {
        return notifyPromises(cause);
    }

    /**
     * Notify all {@link ChannelFuture}s that were registered with {@link #add(ChannelPromise, int)} and
     * their pendingDatasize is smaller then the current writeCounter returned by {@link #writeCounter()} using
     * the given cause1.
     *
     * After a {@link ChannelFuture} was notified it will be removed from this {@link ChannelFlushPromiseNotifier} and
     * so not receive anymore notification.
     *
     * The rest of the remaining {@link ChannelFuture}s will be failed with the given {@link Throwable}.
     *
     * So after this operation this {@link ChannelFutureListener} is empty.
     *
     * @param cause1 the {@link Throwable} which will be used to fail all of the {@link ChannelFuture}s which
     * pendingDataSize is smaller then the current writeCounter returned by {@link #writeCounter()}
     * @param cause2 the {@link Throwable} which will be used to fail the remaining {@link ChannelFuture}s
     */
    public ChannelFlushPromiseNotifier notifyPromises(Throwable cause1, Throwable cause2) {
        notifyPromises0(cause1);
        for (; ; ) {
            FlushCheckpoint cp = flushCheckpoints.poll();
            if (cp == null) {
                break;
            }
            if (tryNotify) {
                cp.promise().tryFailure(cause2);
            } else {
                cp.promise().setFailure(cause2);
            }
        }
        return this;
    }

    /**
     * @deprecated use {@link #notifyPromises(Throwable, Throwable)}
     */
    @Deprecated
    public ChannelFlushPromiseNotifier notifyFlushFutures(Throwable cause1, Throwable cause2) {
        return notifyPromises(cause1, cause2);
    }

    /**
     * 通知由add（ChannelPromise，int）注册的所有ChannelFuture，
     *
     * TODO 并且在writeCounter（）返回的当前writeCounter之后，它们的pendingDataSize会变小。(啥？)
     *
     * 通知ChannelFuture之后，它将被从此ChannelFlushPromiseNotifier中删除，因此不再收到通知。
     *
     * @param cause
     */
    private void notifyPromises0(Throwable cause) {
        if (flushCheckpoints.isEmpty()) {
            writeCounter = 0;
            return;
        }

        final long writeCounter = this.writeCounter;
        for (; ; ) {
            //看看是不是有一个 FlushCheckpoint （ChannelFuture）
            FlushCheckpoint cp = flushCheckpoints.peek();
            if (cp == null) {
                // Reset the counter if there's nothing in the notification list.
                //如果通知列表中没有任何内容，请重置计数器
                this.writeCounter = 0;
                break;
            }
            //如果列表中有东西，cp != null
            if (cp.flushCheckpoint() > writeCounter) {
                if (writeCounter > 0 && flushCheckpoints.size() == 1) {
                    this.writeCounter = 0;
                    cp.flushCheckpoint(cp.flushCheckpoint() - writeCounter);
                }
                break;
            }

            //从列表中移除当前获取到的
            flushCheckpoints.remove();
            //获取真正要通知的对象
            ChannelPromise promise = cp.promise();
            if (cause == null) {
                //tryNotify：
                //如果为true，则将通过ChannelPromise.trySuccess（）和ChannelPromise.tryFailure（Throwable）通知ChannelPromises。
                //否则，将使用ChannelPromise.setSuccess（）和ChannelPromise.setFailure（Throwable）
                if (tryNotify) {
                    promise.trySuccess();
                } else {
                    promise.setSuccess();
                }
            } else {
                if (tryNotify) {
                    promise.tryFailure(cause);
                } else {
                    promise.setFailure(cause);
                }
            }
        }

        // Avoid overflow
        final long newWriteCounter = this.writeCounter;
        //大于 549755813888
        if (newWriteCounter >= 0x8000000000L) {
            // Reset the counter only when the counter grew pretty large
            // so that we can reduce the cost of updating all entries in the notification list.
            //仅当计数器变得非常大时才重置计数器，这样我们可以减少更新通知列表中所有条目的成本。
            this.writeCounter = 0;
            for (FlushCheckpoint cp : flushCheckpoints) {
                cp.flushCheckpoint(cp.flushCheckpoint() - newWriteCounter);
            }
        }
    }

    interface FlushCheckpoint {

        long flushCheckpoint();

        void flushCheckpoint(long checkpoint);

        ChannelPromise promise();
    }

    private static class DefaultFlushCheckpoint implements FlushCheckpoint {

        private long checkpoint;

        private final ChannelPromise future;

        DefaultFlushCheckpoint(long checkpoint, ChannelPromise future) {
            this.checkpoint = checkpoint;
            this.future = future;
        }

        @Override
        public long flushCheckpoint() {
            return checkpoint;
        }

        @Override
        public void flushCheckpoint(long checkpoint) {
            this.checkpoint = checkpoint;
        }

        @Override
        public ChannelPromise promise() {
            return future;
        }
    }
}
