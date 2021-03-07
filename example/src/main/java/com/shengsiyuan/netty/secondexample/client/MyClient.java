package com.shengsiyuan.netty.secondexample.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;

/**
 * 类说明：
 *
 * @author zhucj
 * @since 20200423
 */
public class MyClient {

    public static void main(String[] args) throws InterruptedException {

        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap
                    .group(eventLoopGroup)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .attr(AttributeKey.valueOf("test"), "test")
                    .channel(NioSocketChannel.class)
                    .handler(new MyClientInitializer());

            ChannelFuture channelFuture = bootstrap.connect("localhost", 8888);
            channelFuture.sync();

            Channel channel = channelFuture.channel();
            channel.closeFuture().sync();
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }
}
