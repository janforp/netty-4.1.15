package com.nettyinaction.codes;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * _27_HttpPipelineInitializer
 *
 * @author zhucj
 * @since 20210325
 */
public class _27_HttpPipelineInitializer {

}

class HttpPipelineInitializer extends ChannelInitializer<Channel> {

    private final boolean client;

    public HttpPipelineInitializer(boolean client) {
        this.client = client;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if (client) {
            //如果是客户端，则添加 HttpResponseDecoder 以 处理来自服务器的响应
            pipeline.addLast("decoder", new HttpResponseDecoder());
            //如果是客户端，则添加 HttpRequestEncoder 以向服务器发送请求
            pipeline.addLast("encoder", new HttpRequestEncoder());
        } else {
            //如果是服务器，则添加 HttpRequestDecoder 以接收来自客户端的请求
            pipeline.addLast("decoder", new HttpRequestDecoder());
            //如果是服务器，则添加 HttpResponseEncoder 以向客户端发送响应
            pipeline.addLast("encoder", new HttpResponseEncoder());
        }
    }
}

class HttpCompressionInitializer extends ChannelInitializer<Channel> {

    private final boolean isClient;

    public HttpCompressionInitializer(boolean isClient) {
        this.isClient = isClient;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if (isClient) {
            pipeline.addLast("codec", new HttpClientCodec());// 如果是客户端，则添 加 HttpClientCodec

            //如果是客户端，则添加 HttpContentDecompressor 以 处理来自服务器的压缩内容
            pipeline.addLast("decompressor", new HttpContentDecompressor());
        } else {

            // 如果是服务器，则添 加 HttpServerCodec
            pipeline.addLast("codec", new HttpServerCodec());

            // 如果是服务器，则添加 HttpContentCompressor 来压缩数据(如果客户端支持它)
            pipeline.addLast("compressor", new HttpContentCompressor());
        }
    }
}