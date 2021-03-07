package com.nettyinaction.codes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

/**
 * 在第 1 章中，我们介绍了 Future 和回调，并且阐述了它们在事件驱动设计中的应用。我们 还讨论了 ChannelHandler，它是一个接口族的父接口，它的实现负责接收并响应事件通知。 在 Netty 应用程序中，所有的数据处理逻辑都包含在这些核心抽象的实现中。
 * 因为你的 Echo 服务器会响应传入的消息，所以它需要实现 ChannelInboundHandler 接口，用 来定义响应入站事件的方法。这个简单的应用程序只需要用到少量的这些方法，所以继承 Channel- InboundHandlerAdapter 类也就足够了，它提供了 ChannelInboundHandler 的默认实现。
 * 我们感兴趣的方法是:
 * channelRead()— 对于每个传入的消息都要调用; channelReadComplete()— 通知ChannelInboundHandler最后一次对channel-
 * Read()的调用是当前批量读取中的最后一条消息;
 * exceptionCaught()— 在读取操作期间，有异常抛出时会调用。
 * 该 Echo 服务器的 ChannelHandler 实现是 EchoServerHandler，如代码清单 2-1 所示。
 *
 * @author zhucj
 * @since 20210325
 */
@ChannelHandler.Sharable //表示一个 ChannelHandler 可以被多个 Channel 安全地共享使用
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        //将消息记录到控制台
        System.out.println("Server received: " + in.toString(CharsetUtil.UTF_8));
        //将收到的消息写给发送者，而不是冲刷出张消息
        ctx.write(in);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // 将未决消息冲刷到 远程节点，并且关 闭该 Channel
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //打印异常 栈跟踪
        cause.printStackTrace();
        // 关闭该Channel
        ctx.close();
    }
}
