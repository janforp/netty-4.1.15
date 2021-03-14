package com.nettyinaction.codes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * _13_ByteBufSLice
 *
 * @author zhucj
 * @since 20210325
 */
public class _13_ByteBufSLice {

    public static void main(String[] args) {
        System.out.println("********** slice ************");
        slice();
        System.out.println("********** copy ************");
        copy();
        System.out.println("********** get ************");
        get();
    }

    private static void slice() {
        Charset utf8 = StandardCharsets.UTF_8;
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks", utf8);
        ByteBuf sliced = buf.slice(0, 15);
        System.out.println(sliced.toString(utf8));
        buf.setByte(0, (byte) 'J');
        System.out.println(buf.getByte(0));
        System.out.println(sliced.getByte(0));
    }

    private static void copy() {
        Charset utf8 = StandardCharsets.UTF_8;
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks", utf8);
        ByteBuf copy = buf.copy(0, 15);
        System.out.println(copy.toString(utf8));
        buf.setByte(0, (byte) 'J');
        System.out.println(buf.getByte(0));
        System.out.println(copy.getByte(0));
    }

    private static void get() {
        Charset utf8 = StandardCharsets.UTF_8;
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks", utf8);
        System.out.println((char) buf.getByte(0));
        int readerIndex = buf.readerIndex();
        int writerIndex = buf.writerIndex();

        buf.setByte(0, (byte) 'B');

        System.out.println((char) buf.getByte(0));
        //   将会成功，因为这些操作
        // 并不会修改相应的索引
        System.out.println(readerIndex == buf.readerIndex());//true
        System.out.println(writerIndex == buf.writerIndex());//true
    }
}
