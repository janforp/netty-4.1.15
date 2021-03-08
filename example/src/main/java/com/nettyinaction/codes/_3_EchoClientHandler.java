package com.nettyinaction.codes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * 2.4.1 通过 ChannelHandler 实现客户端逻辑
 * 如同服务器，客户端将拥有一个用来处理数据的 ChannelInboundHandler。在这 个场景 下，你将扩展 SimpleChannelInboundHandler 类以处理所有必须的任务，如代码清单 2-3 所示。这要求重写下面的方法:
 * channelActive()—— 在到服务器的连接已经建立之后将被调用;
 * channelRead0()—— 当从服务器接收到一条消息时被调用;
 * exceptionCaught()—— 在处理过程中引发异常时被调用。
 *
 * @author zhucj
 * @since 20210325
 */
@ChannelHandler.Sharable // 标记该类的实例可以被 多个 Channel 共享
public class _3_EchoClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    /**
     * 在到服务器的连接已经建立之后将被调用;
     *
     * 首先，你重写了 channelActive()方法，其将在一个连接建立时被调用。
     * 这确保了数据 将会被尽可能 快 !!!! 地写入服务器，其在这个场景下是一个编码了字符串"Netty rocks!"的字节 缓冲区。
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //当被通知 Channel 是活跃的时候，发 送一条消息
        //一旦客户端建立连接，它就发送它的消息——Netty rocks!;
        ctx.writeAndFlush(Unpooled.copiedBuffer("Netty rocks!", CharsetUtil.UTF_8));
    }

    /**
     * 当从服务器接收到一条消息时被调用;
     *
     * TODO 这里很重要！！！
     *
     * 接下来，你重写了 channelRead0()方法。
     * 每当接收数据时，都会调用这个方法。
     * 需要注 意的是，由服务器发送的消息可能会被分块接收。
     * 也就是说，如果服务器发送了 5 字节，那么不 能保证这 5 字节会被一次性接收。
     * 即使是对于这么少量的数据，channelRead0()方法也可能 会被调用两次，第一次使用一个持有 3 字节的 ByteBuf(Netty 的字节容器)，第二次使用一个 持有 2 字节的 ByteBuf。
     * 作为一个面向流的协议，TCP 保证了字节数组将会按照服务器发送它 们的顺序被接收。
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        // 记录已接收 消息的转储
        //客户端报告返回的消息并退出。
        System.out.println("Client received: " + msg.toString(CharsetUtil.UTF_8));
    }

    /**
     * 在处理过程中引发异常时被调用。
     *
     * 重写的第三个方法是 exceptionCaught()。如同在 EchoServerHandler(见代码清 单 2-2)中所示，记录 Throwable，关闭 Channel，在这个场景下，终止到服务器的连接。
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        //在发生异常时， 记录错误并关闭 Channel
        ctx.close();
    }
}