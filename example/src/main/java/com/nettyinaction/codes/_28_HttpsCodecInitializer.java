package com.nettyinaction.codes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

import javax.net.ssl.SSLEngine;
import java.util.concurrent.TimeUnit;

/**
 * _28_HttpsCodecInitializer
 *
 * @author zhucj
 * @since 20210325
 */
public class _28_HttpsCodecInitializer {

}

class HttpsCodecInitializer extends ChannelInitializer<Channel> {

    private final SslContext context;

    private final boolean isClient;

    public HttpsCodecInitializer(SslContext context, boolean isClient) {
        this.context = context;
        this.isClient = isClient;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        SSLEngine engine = context.newEngine(ch.alloc());
        pipeline.addFirst("ssl", new SslHandler(engine)); // 将 SslHandler 添加到 ChannelPipeline 中以 使用 HTTPS
        if (isClient) {
            pipeline.addLast("codec", new HttpClientCodec()); // 如果是客户端，则添 加 HttpClientCodec
        } else {
            pipeline.addLast("codec", new HttpServerCodec()); // 如果是服务器，则添 加 HttpServerCodec
        }
    }
}

/**
 * 这个示例演示了如何使用 IdleStateHandler 来测试远程节点是否仍然还活着，并且在它 失活时通过关闭连接来释放资源
 */
class IdleStateHandlerInitializer extends ChannelInitializer<Channel> {

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        //IdleStateHandler 将 在被触发时发送一 个 IdleStateEvent 事件
        pipeline.addLast(new IdleStateHandler(0, 0, 60, TimeUnit.SECONDS));

        // 将一个 Heart- beatHandler 添加到Chan- nelPipeline 中
        pipeline.addLast(new HeartbeatHandler());
    }
}

/**
 * 如果连接超过60秒没有接收或者发送任何的数据，那么IdleStateHandler
 * 将会使用一个 IdleStateEvent 事件来调用 fireUserEventTriggered()方法。
 * HeartbeatHandler 实现 了 userEventTriggered()方法，如果这个方法检测到 IdleStateEvent 事件，
 * 它将会发送心 跳消息，并且添加一个将在发送操作失败时关闭该连接的 ChannelFutureListener   。
 */
final class HeartbeatHandler extends ChannelInboundHandlerAdapter {

    // 发送到远程节点的心跳消息
    private static final ByteBuf HEARTBEAT_SEQUENCE =
            Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("HEARTBEAT", CharsetUtil.ISO_8859_1));

    //实现 userEvent- Triggered()方法 以发送心跳消息
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.writeAndFlush(HEARTBEAT_SEQUENCE.duplicate())

                    // 发送心跳消息，
                    //并在发送失败
                    //时关闭该连接
                    .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        } else {

            //不是 IdleStateEvent 事件，所以将它传递 给下一个 ChannelInboundHandler
            super.userEventTriggered(ctx, evt);
        }
    }
}