package io.netty.channel.socket;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;

/**
 * A TCP/IP socket {@link Channel}.
 */
public interface SocketChannel extends DuplexChannel {

    @Override
    ServerSocketChannel parent();

    @Override
    SocketChannelConfig config();

    @Override
    InetSocketAddress localAddress();

    @Override
    InetSocketAddress remoteAddress();
}
