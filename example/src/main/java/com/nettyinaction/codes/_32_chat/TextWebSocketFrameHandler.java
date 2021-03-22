package com.nettyinaction.codes._32_chat;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

/**
 * TextWebSocketFrameHandler
 *
 * @author zhucj
 * @since 20210325
 */
public class TextWebSocketFrameHandler

        /**
         * 扩展 SimpleChannelInboundHandler， 并处理 TextWebSocketFrame 消息
         */
        extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final ChannelGroup group;

    public TextWebSocketFrameHandler(ChannelGroup group) {
        this.group = group;
    }

    //重写 userEventTriggered() 方法以处理自定义事件
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        //如果该事件 表示握手成 功，则从该 ChannelPipeline 中移除 HttpRequestHandler 因为将不会 接收到任何 HTTP 消息了
        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            ctx.pipeline().remove(HttpRequestHandler.class);
            //1.通知所有已经连接的 WebSocket 客户端新 的客户端已经连接上了
            group.writeAndFlush(new TextWebSocketFrame("客户端 " + ctx.channel() + " 加入了"));
            //2.将新的 WebSocket Channel 添加到 ChannelGroup 中，以 便它可以接收到所有的消息
            group.add(ctx.channel());
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        //3.增加消息的引用计数，并将 它写到 ChannelGroup 中所有 已经连接的客户端
        /**
         * 如果接收到了 TextWebSocketFrame 消息   ，TextWebSocketFrameHandler 将调用 TextWebSocketFrame 消息上的 retain()方法，
         * 并使用 writeAndFlush()方法来将它传 输给 ChannelGroup，以便所有已经连接的 WebSocket Channel 都将接收到它。
         */
        group.writeAndFlush(

                /**
                 * 和之前一样，对于 retain()方法的调用是必需的，因为当 channelRead0()方法返回时，
                 * TextWebSocketFrame 的 引 用 计 数 将 会 被 减 少 。
                 * 由 于 所 有 的 操 作 都 是 异 步 的 ，因 此 ，writeAndFlush()方法可能会在 channelRead0()方法返回之后完成，
                 * 而且它绝对不能访问一个已经失 效的引用。
                 *
                 * 所以在此处保留引用！！！！
                 */
                msg.retain()
        );
    }
}
