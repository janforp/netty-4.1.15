package com.nettyinaction.codes;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;

/**
 * _20_ChannelOption
 *
 * @author zhucj
 * @since 20210325
 */
public class _20_ChannelOption {

    public static void main(String[] args) {

        //创建一个 AttributeKey 以标识该属性
        final AttributeKey<Integer> id = AttributeKey.newInstance("ID");

        // 创建一个 Bootstrap 类的实例以 创建客户端 Channel 并连接它们
        Bootstrap bootstrap = new Bootstrap();

        bootstrap.group(new NioEventLoopGroup())

                // 指定Channel的实现
                .channel(NioSocketChannel.class)
                .handler(new SimpleChannelInboundHandler<ByteBuf>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {

                        //使用 AttributeKey 检索 属性以及它的值
                        Integer idValue = ctx.channel().attr(id).get();
                        System.out.println(idValue);
                    }

                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                        System.out.println("收到了数据");
                    }
                });

        bootstrap.option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);

        // 存储该 id 属性
        bootstrap.attr(id, 123456);

        ChannelFuture future = bootstrap.connect(new InetSocketAddress("127.0.0.1", 9999));
        future.syncUninterruptibly();
    }
}