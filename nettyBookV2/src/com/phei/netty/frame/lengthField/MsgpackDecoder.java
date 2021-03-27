package com.phei.netty.frame.lengthField;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.msgpack.MessagePack;

import java.util.List;

/**
 * Created by hmh on 21/08/2017.
 */
public class MsgpackDecoder extends MessageToMessageDecoder<ByteBuf> {


    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        final byte[] array;
        array = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(array);
        MessagePack messagePack = new MessagePack();
        UserInfo userInfo = messagePack.read(array, UserInfo.class);
        list.add(userInfo);
    }
}
