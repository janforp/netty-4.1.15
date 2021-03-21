package com.nettyinaction.codes;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * _23_EncodeAndDecode
 *
 * @author zhucj
 * @since 20210325
 */
public class _23_EncodeAndDecode {

}

/**
 * 扩展 ByteToMessage- Decoder 类，以将字 节解码为特定的格式
 *
 * 下面举一个如何使用这个类的示例，假设你接收了一个包含简单 int 的字节流，每个 int 都需要被单独处理。在这种情况下，
 * 你需要从入站 ByteBuf 中读取每个 int，并将它传递给 ChannelPipeline 中的下一个 ChannelInboundHandler。为了解码这个字节流，
 * 你要扩展 ByteToMessageDecoder 类。(需要注意的是，原子类型的 int 在被添加到 List 中时，会被 自动装箱为 Integer。)
 *
 * 该设计如图 10-1 所示。
 * 每次从入站 ByteBuf 中读取 4 字节，将其解码为一个 int，然后将它添加到一个 List 中。
 * 当没有更多的元素可以被添加到该 List 中时，它的内容将会被发送给下一个 ChannelInboundHandler。
 */
class ToIntegerDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() >= 4) {
            // 从入站 ByteBuf 中读取 一个 int，并将其添加到 解码消息的 List 中
            out.add(in.readInt());
        }
    }
}

/**
 * 虽然 ByteToMessageDecoder 使得可以很简单地实现这种模式，但是你可能会发现，
 * 在调 用 readInt()方法前不得不验证所输入的 ByteBuf 是否具有足够的数据有点繁琐。在下一节中，
 * 我们将讨论 ReplayingDecoder，它是一个特殊的解码器，以少量的开销消除了这个步骤。
 */
class ToIntegerDecoder2 extends ReplayingDecoder<Void> {

    /**
     * 类型参数 S 指定了用于状态管理的类型，其中 Void 代表不需要状态管理。
     */

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        /**
         * ReplayingDecoder扩展了ByteToMessageDecoder类(如代码清单 10-1 所示)，
         * 使 得我们不必调用readableBytes()方法。它通过使用一个自定义的ByteBuf实现， ReplayingDecoderByteBuf，
         * 包装传入的ByteBuf实现了这一点，其将在内部执行该调用
         */
        out.add(in.readInt());
    }
}