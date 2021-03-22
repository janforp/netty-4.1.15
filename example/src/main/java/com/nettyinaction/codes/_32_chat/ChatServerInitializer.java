package com.nettyinaction.codes._32_chat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 为了将 ChannelHandler 安装到 ChannelPipeline 中，你扩展 了 ChannelInitializer，并实现了 initChannel()方法。
 * 代码清单 12-3 展示了由此生成 的 ChatServerInitializer 的代码。
 *
 * @author zhucj
 * @since 20210325
 */
public class ChatServerInitializer extends ChannelInitializer<Channel> {

    private final ChannelGroup group;

    public ChatServerInitializer(ChannelGroup group) {
        this.group = group;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        /**
         * 对于 initChannel()方法的调用，通过安装所有必需的 ChannelHandler 来设置该新注 册的 Channel 的 ChannelPipeline。
         * 这些 ChannelHandler 以及它们各自的职责都被总结 在了表 12-2 中。
         */
        ChannelPipeline pipeline = ch.pipeline();

        //将字节解码为 HttpRequest、HttpContent 和 LastHttpContent。并将 HttpRequest、HttpContent 和 LastHttpContent 编码为字节
        pipeline.addLast(new HttpServerCodec());

        //写入一个文件的内容
        pipeline.addLast(new ChunkedWriteHandler());

        //将一个 HttpMessage 和跟随它的多个 HttpContent 聚合
        //为单个 FullHttpRequest 或者 FullHttpResponse(取
        //决于它是被用来处理请求还是响应)。
        // 安装了这个之后， ChannelPipeline 中的下一个 ChannelHandler 将只会 收到完整的 HTTP 请求或响应!!!!!!!!!
        pipeline.addLast(new HttpObjectAggregator(60 * 1024));

        //处理 FullHttpRequest(那些不发送到/ws URI 的请求)
        pipeline.addLast(new HttpRequestHandler("/ws"));

        //按照 WebSocket 规范的要求，处理 WebSocket 升级握手、 PingWebSocketFrame 、 PongWebSocketFrame 和 CloseWebSocketFrame
        pipeline.addLast(

                /**
                 * 当 WebSocket 协议升级完成之后，WebSocketServerProtocolHandler
                 * 将会把 HttpRequestDecoder 替换为 WebSocketFrameDecoder，
                 * 把 HttpResponseEncoder 替换为 WebSocketFrameEncoder。为了性能最大化，
                 * 它将移除任何不再被 WebSocket 连接所需要的 ChannelHandler。
                 * 这也包括了图 12-3 所示的 HttpObjectAggregator 和 HttpRequestHandler。
                 */
                new WebSocketServerProtocolHandler("/ws")
        );

        //处理 TextWebSocketFrame 和握手完成事件
        pipeline.addLast(new TextWebSocketFrameHandler(group));
    }
}
