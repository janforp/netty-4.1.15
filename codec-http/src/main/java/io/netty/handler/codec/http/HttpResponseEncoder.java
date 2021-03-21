package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import static io.netty.handler.codec.http.HttpConstants.SP;

/**
 * Encodes an {@link HttpResponse} or an {@link HttpContent} into
 * a {@link ByteBuf}.
 */
public class HttpResponseEncoder extends HttpObjectEncoder<HttpResponse> {
    //将 HttpResponse、HttpContent 和 LastHttpContent 消息编码为字节

    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return super.acceptOutboundMessage(msg) && !(msg instanceof HttpRequest);
    }

    @Override
    protected void encodeInitialLine(ByteBuf buf, HttpResponse response) throws Exception {
        response.protocolVersion().encode(buf);
        buf.writeByte(SP);
        response.status().encode(buf);
        ByteBufUtil.writeShortBE(buf, CRLF_SHORT);
    }
}