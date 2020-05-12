package io.netty.channel;

import io.netty.util.concurrent.EventExecutor;

/**
 * The {@link CompleteChannelFuture} which is succeeded already.  It is
 * recommended to use {@link Channel#newSucceededFuture()} instead of
 * calling the constructor of this future.
 *
 * 成功状态
 * 设计模式：状态模式
 */
final class SucceededChannelFuture extends CompleteChannelFuture {

    /**
     * Creates a new instance.
     *
     * @param channel the {@link Channel} associated with this future
     */
    SucceededChannelFuture(Channel channel, EventExecutor executor) {
        super(channel, executor);
    }

    @Override
    public Throwable cause() {
        return null;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }
}
