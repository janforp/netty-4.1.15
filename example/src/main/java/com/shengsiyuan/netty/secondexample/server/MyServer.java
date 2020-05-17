package com.shengsiyuan.netty.secondexample.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;

/**
 * 类说明：opPY_wsCsxEKJ1K6Hd83trnakuXg
 * opPY_wrYDlEFuwPqiyD8nc6ledXo
 *
 * @author zhucj
 * @since 20200423
 */
public class MyServer {

    public static void main(String[] args) throws InterruptedException {
        //监听客户端连接，转发到 childGroup
        //底层就是一个死循环
        EventLoopGroup parentGroup = new NioEventLoopGroup();
        //真正处理业务的
        EventLoopGroup childGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    .group(parentGroup, childGroup)
                    //                    .option(ChannelOption.SO_RCVBUF, 123)
                    //                    .attr(AttributeKey.valueOf("login"), "123")
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.WARN))
                    //给worker，如果用handler，则给boss
                    .childHandler(new MyServerInitializer());
            //上面的代码都是赋值，也就初始化对象

            //bind中启动
            ChannelFuture channelFuture = serverBootstrap.bind(8899);

            /**
             * 调用该方法，阻塞到上面的 bind 完成
             * @see DefaultChannelPromise#sync()
             *
             * 确保整个服务端的注册初始化操作真正的完成
             */
            channelFuture = channelFuture.sync();
            //NioServerSocketChannel
            Channel channel = channelFuture.channel();
            ChannelFuture closeFuture = channel.closeFuture();

            //等待异步任务 closeFuture 完成

            //正常的服务器的主线程就会一直阻塞在这行代码，直到 显示的调用 channel.close 的时候才返回
            closeFuture.sync();
        } finally {
            parentGroup.shutdownGracefully();
            childGroup.shutdownGracefully();
        }
    }
}
