package io.netty.channel.nio;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * An arbitrary task that can be executed by {@link NioEventLoop} when a {@link SelectableChannel} becomes ready.
 *
 * 当SelectableChannel准备就绪时，可由NioEventLoop执行的任意任务。
 *
 * @see NioEventLoop#register(SelectableChannel, int, NioTask)
 */
public interface NioTask<C extends SelectableChannel> {

    /**
     * Invoked when the {@link SelectableChannel} has been selected by the {@link Selector}.
     *
     * 当选择器选择了SelectableChannel时调用。
     */
    void channelReady(C ch, SelectionKey key) throws Exception;

    /**
     * Invoked when the {@link SelectionKey} of the specified {@link SelectableChannel} has been cancelled and thus
     * this {@link NioTask} will not be notified anymore.
     *
     * 当指定的SelectableChannel的SelectionKey取消时调用，因此不再通知此NioTask。
     *
     * @param cause the cause of the unregistration. {@code null} if a user called {@link SelectionKey#cancel()} or
     * the event loop has been shut down.
     */
    void channelUnregistered(C ch, Throwable cause) throws Exception;
}
