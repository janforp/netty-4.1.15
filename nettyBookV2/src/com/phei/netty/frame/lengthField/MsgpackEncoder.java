package com.phei.netty.frame.lengthField;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.MessagePack;

/**
 * Created by hmh on 21/08/2017.
 */
public class MsgpackEncoder extends MessageToByteEncoder<UserInfo> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, UserInfo o, ByteBuf byteBuf) throws Exception {
        MessagePack messagePack = new MessagePack();
        byte[] raw = messagePack.write(o);
        byteBuf.writeBytes(raw);
    }

    public static void main(String[] args) throws Exception{
        UserInfo userInfo = new UserInfo();
        userInfo.age = 18;
        userInfo.name = "zs";
        MessagePack pack = new MessagePack();
        byte[] raw = pack.write(userInfo);

        UserInfo userInfo2 = pack.read(raw, UserInfo.class);
        System.out.println(userInfo2.age);
        System.out.println(userInfo2.name);
    }
}
