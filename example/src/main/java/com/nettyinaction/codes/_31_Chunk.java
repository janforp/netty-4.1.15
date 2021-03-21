package com.nettyinaction.codes;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.File;
import java.io.FileInputStream;

/**
 * _31_Chunk
 *
 * @author zhucj
 * @since 20210325
 */
public class _31_Chunk {

}

/**
 * 当 Channel 的状态变为活动的时，WriteStreamHandler 将会逐块地把来自文件中的数
 * 据作为 ChunkedStream 写入。数据在传输之前将会由 SslHandler 加密。
 */
class ChunkedWriteHandlerInitializer extends ChannelInitializer<Channel> {

    private final File file;

    private final SslContext sslCtx;

    public ChunkedWriteHandlerInitializer(File file, SslContext sslCtx) {
        this.file = file;
        this.sslCtx = sslCtx;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new SslHandler(sslCtx.newEngine(ch.alloc())));
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new WriteStreamHandler());
    }

    class WriteStreamHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            ctx.writeAndFlush(
                    // 当连接建立时，
                    //channelActive()
                    //方法将使用
                    //ChunkedInput
                    //写文件数据
                    new ChunkedStream(new FileInputStream(file))
            );
        }
    }
}