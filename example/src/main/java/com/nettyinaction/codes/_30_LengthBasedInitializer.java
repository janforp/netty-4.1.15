package com.nettyinaction.codes;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * _30_LengthBasedInitializer
 *
 * @author zhucj
 * @since 20210325
 */
public class _30_LengthBasedInitializer {

}

class LengthBasedInitializer extends ChannelInitializer<Channel> {

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(
                /**
                 * 一个帧的最大长度是 60 * 1024
                 * 由前8个字节表示的数据来表示地区帧的 具体大小
                 */
                new LengthFieldBasedFrameDecoder(
                        60 * 1024,
                        0,//帧长度字段的起始位置
                        8)); // 帧长度字段的长度
        pipeline.addLast(new FrameHandler());

    }
}

class FrameHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        //处理数据
    }
}