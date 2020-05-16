package io.netty.channel;

/**
 * {@link RecvByteBufAllocator} that limits the number of read operations that will be attempted when a read operation
 * is attempted by the event loop.
 */
public interface MaxMessagesRecvByteBufAllocator extends RecvByteBufAllocator {

    /**
     * Returns the maximum number of messages to read per read loop.
     * a {@link ChannelInboundHandler#channelRead(ChannelHandlerContext, Object) channelRead()} event.
     * If this value is greater than 1, an event loop might attempt to read multiple times to procure multiple messages.
     */
    int maxMessagesPerRead();

    /**
     * Sets the maximum number of messages to read per read loop.
     * If this value is greater than 1, an event loop might attempt to read multiple times to procure multiple messages.
     */
    MaxMessagesRecvByteBufAllocator maxMessagesPerRead(int maxMessagesPerRead);
}
