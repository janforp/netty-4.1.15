package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;

/**
 * {@link ChannelOutboundHandlerAdapter} which encodes message in a stream-like fashion from one message to an
 * {@link ByteBuf}.
 *
 *
 * Example implementation which encodes {@link Integer}s to a {@link ByteBuf}.
 *
 * <pre>
 *     public class IntegerEncoder extends {@link MessageToByteEncoder}&lt;{@link Integer}&gt; {
 *         {@code @Override}
 *         public void encode({@link ChannelHandlerContext} ctx, {@link Integer} msg, {@link ByteBuf} out)
 *                 throws {@link Exception} {
 *             out.writeInt(msg);
 *         }
 *     }
 * </pre>
 */
public abstract class MessageToByteEncoder<I> extends ChannelOutboundHandlerAdapter {

    /**
     * 编码器实现了 ChannelOutboundHandler，并将出站数据从 一种格式转换为另一种格式，和我们方才学习的解码器的功能正好相反。
     */

    private final TypeParameterMatcher matcher;

    /**
     * 如果应尝试将直接{@link ByteBuf}用作编码消息的目标，则{@code true}。如果使用{@code false}，它将分配一个堆{@link ByteBuf}，该堆由字节数组支持。
     *
     * @see MessageToByteEncoder#allocateBuffer(io.netty.channel.ChannelHandlerContext, java.lang.Object, boolean) 看这个方法就能理解了
     *
     * 是字节堆内存还是使用堆外内存
     */
    private final boolean preferDirect;

    /**
     * see {@link #MessageToByteEncoder(boolean)} with {@code true} as boolean parameter.
     */
    protected MessageToByteEncoder() {
        this(true);
    }

    /**
     * see {@link #MessageToByteEncoder(Class, boolean)} with {@code true} as boolean value.
     */
    protected MessageToByteEncoder(Class<? extends I> outboundMessageType) {
        this(outboundMessageType, true);
    }

    /**
     * Create a new instance which will try to detect the types to match out of the type parameter of the class.
     *
     * @param preferDirect {@code true} if a direct {@link ByteBuf} should be tried to be used as target for the encoded messages. If {@code false} is used it will allocate a heap {@link ByteBuf}, which is backed by an byte array.
     *
     * 如果应尝试将直接{@link ByteBuf}用作编码消息的目标，则{@code true}。如果使用{@code false}，它将分配一个堆{@link ByteBuf}，该堆由字节数组支持。
     */
    protected MessageToByteEncoder(boolean preferDirect) {
        matcher = TypeParameterMatcher.find(this, MessageToByteEncoder.class, "I");
        this.preferDirect = preferDirect;
    }

    /**
     * Create a new instance
     *
     * @param outboundMessageType The type of messages to match
     * @param preferDirect {@code true} if a direct {@link ByteBuf} should be tried to be used as target for
     * the encoded messages. If {@code false} is used it will allocate a heap
     * {@link ByteBuf}, which is backed by an byte array.
     */
    protected MessageToByteEncoder(Class<? extends I> outboundMessageType, boolean preferDirect) {
        matcher = TypeParameterMatcher.get(outboundMessageType);
        this.preferDirect = preferDirect;
    }

    /**
     * Returns {@code true} if the given message should be handled.
     * If {@code false} it will be passed to the next
     * {@link ChannelOutboundHandler} in the {@link ChannelPipeline}.
     *
     * -- 如果应处理给定的消息，则返回{@code true}。
     * 如果{@code false}，它将被传递到{@link ChannelPipeline}中的下一个{@link ChannelOutboundHandler}。
     *
     * @see MessageToByteEncoder#write(io.netty.channel.ChannelHandlerContext, java.lang.Object, io.netty.channel.ChannelPromise)
     */
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return matcher.match(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        //负责将 POJO 转换成 ByteBuf

        ByteBuf buf = null;
        try {
            if (acceptOutboundMessage(msg)) {
                //如果当前类型满足当前对象需要的类型，则进入当前分支
                @SuppressWarnings("unchecked")
                I cast = (I) msg;
                buf = allocateBuffer(ctx, cast, preferDirect);
                try {
                    /**
                     * 由子类实现，模版方法设计模式
                     *
                     * 把对象 cast 编码成字节，并且放到 buf 中
                     */
                    encode(ctx, cast, buf);
                } finally {
                    ReferenceCountUtil.release(cast);
                }

                if (buf.isReadable()) {
                    // 如果缓冲区包含可以发送的字节，则发送

                    //触发 pipeline 中的下一个处理器
                    ctx.write(buf, promise);
                } else {
                    // 如果缓冲区不包含可以发送的字节，则释放，并且写入一个空 ByteBuf 到 ctx

                    buf.release();
                    ctx.write(Unpooled.EMPTY_BUFFER, promise);
                }
                buf = null;
            } else {
                /**
                 * 如果该处理器无法处理该消息，则往下传递
                 */
                ctx.write(msg, promise);
            }
        } catch (EncoderException e) {
            throw e;
        } catch (Throwable e) {
            throw new EncoderException(e);
        } finally {
            if (buf != null) {
                //方法退出之前释放

                buf.release();
            }
        }
    }

    /**
     * Allocate a {@link ByteBuf} which will be used as argument of {@link #encode(ChannelHandlerContext, I, ByteBuf)}.
     * Sub-classes may override this method to return {@link ByteBuf} with a perfect matching {@code initialCapacity}.
     */
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, I msg, boolean preferDirect) throws Exception {
        if (preferDirect) {
            return ctx.alloc().ioBuffer();
        } else {
            return ctx.alloc().heapBuffer();
        }
    }

    /**
     * Encode a message into a {@link ByteBuf}. This method will be called for each written message that can be handled
     * by this encoder.
     * -- 将消息编码到{@link ByteBuf}中。对于此编码器可以处理的每条书面消息，将调用此方法。
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link MessageToByteEncoder} belongs to
     * -- {@link MessageToByteEncoder}所属的{@link ChannelHandlerContext}
     * --
     * @param msg the message to encode -- 要编码的信息
     * @param out the {@link ByteBuf} into which the encoded message will be written -- 编码后的消息将写入的{@link ByteBuf}
     * @throws Exception is thrown if an error occurs
     *
     *
     * encode()方法是你需要实现的唯一抽象方法。
     * 它被调用时 将会传入要被该类编码为 ByteBuf 的(类型为 I 的)出站 消息。
     * 该 ByteBuf 随后将会被转发给 ChannelPipeline 中的下一个 ChannelOutboundHandler
     */
    protected abstract void encode(
            ChannelHandlerContext ctx, //{@link MessageToByteEncoder}所属的{@link ChannelHandlerContext}
            I msg, //   要编码的信息
            ByteBuf out // 编码后的消息将写入的{@link ByteBuf}
    ) throws Exception;

    /**
     * 你可能已经注意到了，这个类只有一个方法，而解码器有两个。
     * 原因是解码器通常需要在 Channel 关闭之后产生最后一个消息(因此也就有了 decodeLast()方法)。
     * 这显然不适用于 编码器的场景——在连接被关闭之后仍然产生一个消息是毫无意义的。
     */

    protected boolean isPreferDirect() {
        return preferDirect;
    }
}
