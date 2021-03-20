package com.nettyinaction.codes;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

/**
 * _18_BootstrapServer
 *
 * @author zhucj
 * @since 20210325
 */
public class _18_BootstrapServer {

    public static void main(String[] args) {
        // 创建 ServerBootstrap 以创建 ServerSocketChannel，并绑定它
        ServerBootstrap bootstrap = new ServerBootstrap();
        // 设置 EventLoopGroup，其将提供用 以处理 Channel 事件的 EventLoop
        bootstrap.group(new NioEventLoopGroup(), new NioEventLoopGroup())

                // 指定要使用的 Channel 实现
                .channel(NioServerSocketChannel.class)

                //设置用于处理已被接受的 子 Channel 的 I/O 和数据的 ChannelInboundHandler
                .childHandler(new SimpleChannelInboundHandler<ByteBuf>() {

                    ChannelFuture connectFuture;

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) {
                        // 创建一个 Bootstrap 类的实例以连接 到远程主机
                        Bootstrap bootstrap = new Bootstrap();

                        // 指定 Channel 的实现
                        bootstrap.channel(NioSocketChannel.class)

                                // 为入站 I/O 设置 ChannelInboundHandler
                                .handler(new SimpleChannelInboundHandler<ByteBuf>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                        System.out.println("收到数据");
                                    }
                                });

                        // 使用与分配给 已被接受的子 Channel 相同的 EventLoop
                        bootstrap.group(ctx.channel().eventLoop());

                        // 连接到 远程节点
                        connectFuture = bootstrap.connect(new InetSocketAddress("127.0.0.1", 8666));
                    }

                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                        if (connectFuture.isDone()) {
                            // 当连接完成时，执行一些数据操作(如代理)
                            System.out.println("收到数据 " + msg);
                        }
                    }
                });

        // 通过配置好的 ServerBootstrap 绑定该 ServerSocketChannel
        ChannelFuture future = bootstrap.bind(new InetSocketAddress(8666));
        future.addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                System.out.println("服务器绑定完成");
            } else {
                System.err.println("服务器绑定失败");
                channelFuture.cause().printStackTrace();
            }
        });
    }
}