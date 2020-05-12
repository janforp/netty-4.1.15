package com.shengsiyuan.netty.secondexample.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

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
        EventLoopGroup parentGroup = new NioEventLoopGroup(5);
        //真正处理业务的
        EventLoopGroup childGroup = new NioEventLoopGroup(10);
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    .group(parentGroup, childGroup)
                    //.option(ChannelOption.SO_RCVBUF, 123)
                    //.attr()
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.WARN))
                    //给worker，如果用handler，则给boss
                    .childHandler(new MyServerInitializer());
            //上面的代码都是赋值，也就初始化对象

            //bind中启动
            ChannelFuture channelFuture = serverBootstrap.bind(8899);
            channelFuture = channelFuture.sync();

            Channel channel = channelFuture.channel();
            ChannelFuture closeFuture = channel.closeFuture();
            closeFuture.sync();
        } finally {
            parentGroup.shutdownGracefully();
            childGroup.shutdownGracefully();
        }
    }
}
