package com.nettyinaction.codes;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;

import java.net.InetSocketAddress;

/**
 * _19_ChannelInitializer
 *
 * @author zhucj
 * @since 20210325
 */
public class _19_ChannelInitializer {

    public static void main(String[] args) throws InterruptedException {
        //创建 ServerBootstrap 以创 建和绑定新的 Channel
        ServerBootstrap bootstrap = new ServerBootstrap();

        //设置 EventLoopGroup，其将提供用 以处理 Channel 事件的 EventLoop
        bootstrap.group(new NioEventLoopGroup(), new NioEventLoopGroup())

                // 指定 Channel 的 实现
                .channel(NioServerSocketChannel.class)

                //注册一个 ChannelInitializerImpl 实例来设置 ChannelPipeline
                .childHandler(new ChannelInitializerImpl());

        //绑定 到地址
        ChannelFuture future = bootstrap.bind(new InetSocketAddress(8080));
        future.sync();

    }

    static final class ChannelInitializerImpl extends ChannelInitializer<Channel> {

        @Override
        protected void initChannel(Channel ch) {
            ChannelPipeline pipeline = ch.pipeline();
            // 将所需的 ChannelHandler 添加到 ChannelPipeline
            pipeline.addLast(new HttpClientCodec());
            pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
        }
    }
}
