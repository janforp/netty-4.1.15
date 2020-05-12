package io.netty.util.concurrent;

/**
 * The {@link CompleteFuture} which is succeeded already.  It is
 * recommended to use {@link EventExecutor#newSucceededFuture(Object)} instead of
 * calling the constructor of this future.
 */
public final class SucceededFuture<V> extends CompleteFuture<V> {

    private final V result;

    /**
     * Creates a new instance.
     *
     * @param executor the {@link EventExecutor} associated with this future
     */
    public SucceededFuture(EventExecutor executor, V result) {
        super(executor);
        this.result = result;
    }

    @Override
    public Throwable cause() {
        //如果成功，则肯定没有失败原因
        return null;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public V getNow() {
        return result;
    }
}
