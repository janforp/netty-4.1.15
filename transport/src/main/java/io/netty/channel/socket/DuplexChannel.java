package io.netty.channel.socket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;

import java.net.Socket;

/**
 * 双工 Channel:一个双工通道，其两侧可以独立关闭。
 *
 * 双工：输入，输出
 *
 * <p></p>
 * A duplex {@link Channel} that has two sides that can be shutdown independently.
 */
public interface DuplexChannel extends Channel {

    /**
     * 当且仅当远程对等方关闭其输出，以便不再从该通道接收到任何数据时，才返回true。请注意，此方法的语义与Socket.shutdownInput（）和Socket.isInputShutdown（）的语义不同。
     * <p></p>
     * Returns {@code true} if and only if the remote peer shut down its output so that no more
     * data is received from this channel.  Note that the semantic of this method is different from
     * that of {@link Socket#shutdownInput()} and {@link Socket#isInputShutdown()}.
     */
    boolean isInputShutdown();

    /**
     * @see Socket#shutdownInput()
     */
    ChannelFuture shutdownInput();

    /**
     * Will shutdown the input and notify {@link ChannelPromise}.
     *
     * @see Socket#shutdownInput()
     */
    ChannelFuture shutdownInput(ChannelPromise promise);

    /**
     * @see Socket#isOutputShutdown()
     */
    boolean isOutputShutdown();

    /**
     * @see Socket#shutdownOutput()
     */
    ChannelFuture shutdownOutput();

    /**
     * Will shutdown the output and notify {@link ChannelPromise}.
     *
     * @see Socket#shutdownOutput()
     */
    ChannelFuture shutdownOutput(ChannelPromise promise);

    /**
     * Determine if both the input and output of this channel have been shutdown.
     */
    boolean isShutdown();

    /**
     * Will shutdown the input and output sides of this channel.
     *
     * @return will be completed when both shutdown operations complete.
     */
    ChannelFuture shutdown();

    /**
     * Will shutdown the input and output sides of this channel.
     *
     * @param promise will be completed when both shutdown operations complete.
     * @return will be completed when both shutdown operations complete.
     */
    ChannelFuture shutdown(ChannelPromise promise);
}
