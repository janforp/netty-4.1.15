package com.nettyinaction.codes._32_chat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.ImmediateEventExecutor;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;

/**
 * ChatServer
 *
 * @author zhucj
 * @since 20210325
 */
public class ChatServer {

    // 创建 DefaultChannelGroup 其将保存所有已经连接的 WebSocket Channel
    private final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

    private final EventLoopGroup group = new NioEventLoopGroup();

    private Channel channel;

    public ChannelFuture start(InetSocketAddress address) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(group).channel(NioServerSocketChannel.class).childHandler(createInitializer(channelGroup));

        ChannelFuture future = bootstrap.bind(address);
        future.syncUninterruptibly();
        channel = future.channel();
        return future;
    }

    public ChannelInitializer<Channel> createInitializer(ChannelGroup channelGroup) {
        return new ChatServerInitializer(channelGroup);
    }

    public void destroy() {
        if (channel != null) {
            channel.close();
        }
        channelGroup.close();
        group.shutdownGracefully();
    }

    public static void main(String[] args) throws CertificateException, SSLException {
        int port = 9999;

        ChatServer endPoint = new ChatServer();
        ChannelFuture future = endPoint.start(new InetSocketAddress(port));

        Runtime.getRuntime().addShutdownHook(new Thread(endPoint::destroy));

        future.channel().closeFuture().syncUninterruptibly();
    }
}
