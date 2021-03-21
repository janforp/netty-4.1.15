package com.nettyinaction.codes;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

/**
 * 代码清单 11-1 展示了如何使用 ChannelInitializer 来将 SslHandler 添加到 Channel- Pipeline 中。
 *
 * @author zhucj
 * @since 20210325
 */
public class _26_SslChannelInitializer {

}

class SslChannelInitializer extends ChannelInitializer<Channel> {

    private final SslContext sslContext;

    private final boolean startTls;

    public SslChannelInitializer(SslContext sslContext, boolean startTls) {
        //传入要使用的SslContext
        this.sslContext = sslContext;
        // 如果设置为 true，第一个 写入的消息将不会被加密 客户端应该设置为 true)
        this.startTls = startTls;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        // 对于每个 SslHandler 实例， 都使用 Channel 的 ByteBufAllocator 从 SslContext 获 取一个新的 SSLEngine
        SSLEngine engine = sslContext.newEngine(ch.alloc());
        //将 SslHandler 作为第一个 ChannelHandler 添加到 ChannelPipeline 中

        /**
         * 在大多数情况下，SslHandler将是ChannelPipeline中的第一个ChannelHandler。
         */
        ch.pipeline().addFirst("ssl", new SslHandler(engine, startTls));
    }
}
