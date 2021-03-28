package io.netty.channel;

import java.net.SocketAddress;

/**
 * Represents the properties of a {@link Channel} implementation.
 */
public final class ChannelMetadata {

    /**
     * // Translate disconnect to close if the channel has no notion of disconnect-reconnect.（如果通道没有断开-重新连接的概念，则将断开转换为关闭。）
     * // So far, UDP/IP is the only transport that has such behavior.（到目前为止，UDP / IP是唯一具有这种行为的传输）
     */
    private final boolean hasDisconnect;

    /**
     * 默认每次读取最大消息数
     */
    private final int defaultMaxMessagesPerRead;

    /**
     * Create a new instance
     *
     * @param hasDisconnect {@code true} if and only if the channel has the {@code disconnect()} operation
     * that allows a user to disconnect and then call {@link Channel#connect(SocketAddress)}
     * again, such as UDP/IP.
     */
    public ChannelMetadata(boolean hasDisconnect) {
        this(hasDisconnect, 1);
    }

    /**
     * Create a new instance
     *
     * @param hasDisconnect {@code true} if and only if the channel has the {@code disconnect()} operation
     * that allows a user to disconnect and then call {@link Channel#connect(SocketAddress)}
     * again, such as UDP/IP.
     * @param defaultMaxMessagesPerRead If a {@link MaxMessagesRecvByteBufAllocator} is in use, then this value will be set for {@link MaxMessagesRecvByteBufAllocator#maxMessagesPerRead()}. Must be {@code > 0}.
     */
    public ChannelMetadata(boolean hasDisconnect, int defaultMaxMessagesPerRead) {
        if (defaultMaxMessagesPerRead <= 0) {
            throw new IllegalArgumentException("defaultMaxMessagesPerRead: " + defaultMaxMessagesPerRead +
                    " (expected > 0)");
        }
        this.hasDisconnect = hasDisconnect;
        this.defaultMaxMessagesPerRead = defaultMaxMessagesPerRead;
    }

    /**
     * Returns {@code true} if and only if the channel has the {@code disconnect()} operation
     * that allows a user to disconnect and then call {@link Channel#connect(SocketAddress)} again,
     * such as UDP/IP.
     */
    public boolean hasDisconnect() {
        return hasDisconnect;
    }

    /**
     * If a {@link MaxMessagesRecvByteBufAllocator} is in use, then this is the default value for
     * {@link MaxMessagesRecvByteBufAllocator#maxMessagesPerRead()}.
     */
    public int defaultMaxMessagesPerRead() {
        return defaultMaxMessagesPerRead;
    }
}
