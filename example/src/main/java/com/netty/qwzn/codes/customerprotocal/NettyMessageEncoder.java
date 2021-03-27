package com.netty.qwzn.codes.customerprotocal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * NettyMessageEncoder
 *
 * @author zhucj
 * @since 20210325
 */
public class NettyMessageEncoder extends MessageToMessageEncoder<NettyMessage> {

    MarshallingEncoder marshallingEncoder;

    public NettyMessageEncoder() throws IOException {
        marshallingEncoder = new MarshallingEncoder();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, NettyMessage msg, List<Object> out) throws Exception {
        if (msg == null) {
            throw new Exception("待解码的消息为 null");
        }
        Header header = msg.getHeader();
        if (header == null) {
            throw new Exception("待解码消息头为 null");
        }
        ByteBuf sendBuf = Unpooled.buffer();

        sendBuf.writeInt(header.getCrcCode());
        sendBuf.writeInt(header.getLength());
        sendBuf.writeLong(header.getSessionID());
        sendBuf.writeByte(header.getType());
        sendBuf.writeByte(header.getPriority());
        sendBuf.writeInt(header.getAttachment().size());

        String key;
        byte[] keyArray;
        Object value;
        for (Map.Entry<String, Object> param : header.getAttachment().entrySet()) {
            key = param.getKey();
            keyArray = key.getBytes(CharsetUtil.UTF_8);
            sendBuf.writeInt(keyArray.length);
            sendBuf.writeBytes(keyArray);

            value = param.getValue();
            marshallingEncoder.encode(value, sendBuf);
        }
        key = null;
        keyArray = null;

        if (msg.getBody() != null) {
            marshallingEncoder.encode(msg.getBody(), sendBuf);
        } else {
            sendBuf.writeInt(0);
            //最后设置消息长度
            sendBuf.setInt(4, sendBuf.readableBytes());
        }
    }
}