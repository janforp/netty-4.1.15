package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.TypeParameterMatcher;

import java.util.List;

/**
 * A Codec for on-the-fly encoding/decoding of bytes to messages and vise-versa.
 *
 * This can be thought of as a combination of {@link ByteToMessageDecoder} and {@link MessageToByteEncoder}.
 *
 * Be aware that sub-classes of {@link ByteToMessageCodec} <strong>MUST NOT</strong>
 * annotated with {@link @Sharable}.
 */
public abstract class ByteToMessageCodec<I> extends ChannelDuplexHandler {

    private final TypeParameterMatcher outboundMsgMatcher;

    private final MessageToByteEncoder<I> encoder;

    private final ByteToMessageDecoder decoder = new ByteToMessageDecoder() {
        @Override
        public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            ByteToMessageCodec.this.decode(ctx, in, out);
        }

        @Override
        protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            ByteToMessageCodec.this.decodeLast(ctx, in, out);
        }
    };

    /**
     * see {@link #ByteToMessageCodec(boolean)} with {@code true} as boolean parameter.
     */
    protected ByteToMessageCodec() {
        this(true);
    }

    /**
     * see {@link #ByteToMessageCodec(Class, boolean)} with {@code true} as boolean value.
     */
    protected ByteToMessageCodec(Class<? extends I> outboundMessageType) {
        this(outboundMessageType, true);
    }

    /**
     * Create a new instance which will try to detect the types to match out of the type parameter of the class.
     *
     * @param preferDirect {@code true} if a direct {@link ByteBuf} should be tried to be used as target for
     * the encoded messages. If {@code false} is used it will allocate a heap
     * {@link ByteBuf}, which is backed by an byte array.
     */
    protected ByteToMessageCodec(boolean preferDirect) {
        ensureNotSharable();
        outboundMsgMatcher = TypeParameterMatcher.find(this, ByteToMessageCodec.class, "I");
        encoder = new Encoder(preferDirect);
    }

    /**
     * Create a new instance
     *
     * @param outboundMessageType The type of messages to match
     * @param preferDirect {@code true} if a direct {@link ByteBuf} should be tried to be used as target for
     * the encoded messages. If {@code false} is used it will allocate a heap
     * {@link ByteBuf}, which is backed by an byte array.
     */
    protected ByteToMessageCodec(Class<? extends I> outboundMessageType, boolean preferDirect) {
        ensureNotSharable();
        outboundMsgMatcher = TypeParameterMatcher.get(outboundMessageType);
        encoder = new Encoder(preferDirect);
    }

    /**
     * Returns {@code true} if and only if the specified message can be encoded by this codec.
     *
     * @param msg the message
     */
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return outboundMsgMatcher.match(msg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        decoder.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        encoder.write(ctx, msg, promise);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        decoder.channelReadComplete(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        decoder.channelInactive(ctx);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        try {
            decoder.handlerAdded(ctx);
        } finally {
            encoder.handlerAdded(ctx);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        try {
            decoder.handlerRemoved(ctx);
        } finally {
            encoder.handlerRemoved(ctx);
        }
    }

    /**
     * @see MessageToByteEncoder#encode(ChannelHandlerContext, Object, ByteBuf)
     *
     * 对于每个将被编码并写入出站 ByteBuf 的(类型为 I 的) 消息来说，这个方法都将会被调用
     */
    protected abstract void encode(ChannelHandlerContext ctx, I msg, ByteBuf out) throws Exception;

    /**
     * @see ByteToMessageDecoder#decode(ChannelHandlerContext, ByteBuf, List)
     * 只要有字节可以被消费，这个方法就将会被调用。
     * 它将入站 ByteBuf 转换为指定的消息格式，并将其转发给 ChannelPipeline 中的下一个 ChannelInboundHandler
     */
    protected abstract void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception;

    /**
     * @see ByteToMessageDecoder#decodeLast(ChannelHandlerContext, ByteBuf, List)
     *
     * 这个方法的默认实现委托给了 decode()方法。它只会在 Channel 的状态变为非活动时被调用一次。它可以被重写以 实现特殊的处理
     */
    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.isReadable()) {
            // Only call decode() if there is something left in the buffer to decode.
            // See https://github.com/netty/netty/issues/4386
            decode(ctx, in, out);
        }
    }

    private final class Encoder extends MessageToByteEncoder<I> {

        Encoder(boolean preferDirect) {
            super(preferDirect);
        }

        @Override
        public boolean acceptOutboundMessage(Object msg) throws Exception {
            return ByteToMessageCodec.this.acceptOutboundMessage(msg);
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, I msg, ByteBuf out) throws Exception {
            ByteToMessageCodec.this.encode(ctx, msg, out);
        }
    }
}
