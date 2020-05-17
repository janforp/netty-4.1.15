package io.netty.channel;

import io.netty.channel.socket.ServerSocketChannel;

/**
 * 一个接受传入连接尝试并通过接受它们创建其子通道的Channel。 ServerSocketChannel是一个很好的例子。
 *
 * 接收连接的 通道，即服务端通道
 *
 * <p></p>
 * A {@link Channel} that accepts an incoming connection attempt and creates
 * its child {@link Channel}s by accepting them.  {@link ServerSocketChannel} is
 * a good example.
 */
public interface ServerChannel extends Channel {
    // This is a tag interface.
}
