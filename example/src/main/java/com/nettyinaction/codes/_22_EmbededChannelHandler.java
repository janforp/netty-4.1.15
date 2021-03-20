package com.nettyinaction.codes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.TooLongFrameException;

import java.util.List;

/**
 * _22_EmbeedHandler
 *
 * @author zhucj
 * @since 20210325
 */
public class _22_EmbededChannelHandler {

    public static void main(String[] args) {
        testFramesDecoded();
        System.out.println("************");
        testFramesDecoded2();
    }

    public static void testFramesDecoded() {
        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            buf.writeByte(i);
        }
        ByteBuf input = buf.duplicate();
        EmbeddedChannel channel = new EmbeddedChannel(new FixedLengthFrameDecoder(3));
        System.out.println(channel.writeInbound(input.retain()));
        System.out.println(channel.finish());

        ByteBuf read = channel.readInbound();
        System.out.println(buf.readSlice(3).equals(read));
        read.release();

        read = channel.readInbound();
        System.out.println(buf.readSlice(3).equals(read));
        read.release();

        read = channel.readInbound();
        System.out.println(buf.readSlice(3).equals(read));
        read.release();

        System.out.println(channel.readInbound() == null);
        buf.release();
    }

    public static void testFramesDecoded2() {
        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            buf.writeByte(i);
        }
        ByteBuf input = buf.duplicate();
        EmbeddedChannel channel = new EmbeddedChannel(new FixedLengthFrameDecoder(3));
        System.out.println(!channel.writeInbound(input.readBytes(2)));
        System.out.println(channel.writeInbound(input.readBytes(7)));
        System.out.println(channel.finish());

        ByteBuf read = channel.readInbound();
        System.out.println(buf.readSlice(3).equals(read));
        read.release();

        read = channel.readInbound();
        System.out.println(buf.readSlice(3).equals(read));
        read.release();

        read = channel.readInbound();
        System.out.println(buf.readSlice(3).equals(read));
        read.release();

        System.out.println(channel.readInbound() == null);
        buf.release();
    }
}

/**
 * 扩展 ByteToMessageDecoder 以处理入 站字节，并将它们解码为消息
 */
class FixedLengthFrameDecoder extends ByteToMessageDecoder {

    /**
     * 指定要生成 的帧的长度
     */
    private final int frameLength;

    public FixedLengthFrameDecoder(int frameLength) {
        this.frameLength = frameLength;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        //检查是否有足够的字 节可以被读取，以生 成下一个帧
        while (in.readableBytes() >= frameLength) {
            //从 ByteBuf 中 读取一个新帧
            ByteBuf buf = in.readBytes(frameLength);
            //将该帧添加到已被 解码的消息列表中
            out.add(buf);
        }
    }
}

/**
 * 扩展 MessageToMessageEncoder 以 将一个消息编码为另外一种格式
 */
class AbsIntegerEncode extends MessageToMessageEncoder<ByteBuf> {

    public static void main(String[] args) {
        ByteBuf buf = Unpooled.buffer();
        for (int i = 1; i < 10; i++) {
            //将 4 字节的负整数写到一个新的 ByteBuf 中。
            buf.writeInt(i * -1);
        }
        //创建一个 EmbeddedChannel，并为它分配一个 AbsIntegerEncoder
        EmbeddedChannel channel = new EmbeddedChannel(new AbsIntegerEncode());

        //调用 EmbeddedChannel 上的 writeOutbound()方法来写入该 ByteBuf。
        System.out.println(channel.writeOutbound(buf));

        //标记该 Channel 为已完成状态。
        System.out.println(channel.finish());

        for (int i = 1; i < 10; i++) {
            //从 EmbeddedChannel 的出站端读取所有的整数，并验证是否只产生了绝对值。
            System.out.println(((int) channel.readOutbound() == i));
        }
        System.out.println(channel.readOutbound() == null);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (in.readableBytes() >= 4) {//检查是否有足够的字节用来编码
            int value = Math.abs(in.readInt());//从输入的 ByteBuf 中读取下一个整数， 并且计算其绝对值
            out.add(value);// 将该整数写入到编码 消息的 List 中
        }
    }
}

class FrameChunkDecoder extends ByteToMessageDecoder {

    private final int maxFrameSize;

    public FrameChunkDecoder(int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int readableBytes = in.readableBytes();
        if (readableBytes > maxFrameSize) {
            // 如果该帧太大，则丢弃它并抛出一 个 TooLongFrameException......
            // discard the bytes
            in.clear();
            throw new TooLongFrameException();
        }
        ByteBuf buf = in.readBytes(readableBytes);
        out.add(buf);
    }
}