package io.netty.channel.socket;

import io.netty.channel.ServerChannel;

import java.net.InetSocketAddress;

/**
 * 一个TCP / IP ServerChannel，它接受传入的TCP / IP连接。
 *
 * <p></p>
 * A TCP/IP {@link ServerChannel} which accepts incoming TCP/IP connections.
 */
public interface ServerSocketChannel extends ServerChannel {

    @Override
    ServerSocketChannelConfig config();

    @Override
    InetSocketAddress localAddress();

    @Override
    InetSocketAddress remoteAddress();
}
