package io.netty.handler.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.internal.TypeParameterMatcher;

import java.util.List;

/**
 * {@link ChannelInboundHandlerAdapter} which decodes from one message to an other message.
 *
 *
 * For example here is an implementation which decodes a {@link String} to an {@link Integer} which represent
 * the length of the {@link String}.
 *
 * <pre>
 *     public class StringToIntegerDecoder extends
 *             {@link MessageToMessageDecoder}&lt;{@link String}&gt; {
 *
 *         {@code @Override}
 *         public void decode({@link ChannelHandlerContext} ctx, {@link String} message,
 *                            List&lt;Object&gt; out) throws {@link Exception} {
 *             out.add(message.length());
 *         }
 *     }
 * </pre>
 *
 * Be aware that you need to call {@link ReferenceCounted#retain()} on messages that are just passed through if they
 * are of type {@link ReferenceCounted}. This is needed as the {@link MessageToMessageDecoder} will call
 * {@link ReferenceCounted#release()} on decoded messages.
 *
 * @param <I> 类型参数 I 指定了 decode()方法的输入参数 msg 的类型，它是你必须实现的唯一方法。
 */
public abstract class MessageToMessageDecoder<I> extends ChannelInboundHandlerAdapter {

    /**
     * 解码器处理入站数据
     *
     * 该类的作用：将一种消息类型解码为另一种
     */

    private final TypeParameterMatcher matcher;

    /**
     * Create a new instance which will try to detect the types to match out of the type parameter of the class.
     *
     * -- 创建一个新实例，该实例将尝试检测与类的type参数匹配的类型。
     */
    protected MessageToMessageDecoder() {
        matcher = TypeParameterMatcher.find(this, MessageToMessageDecoder.class, "I");
    }

    /**
     * Create a new instance
     *
     * @param inboundMessageType The type of messages to match and so decode
     */
    protected MessageToMessageDecoder(Class<? extends I> inboundMessageType) {
        matcher = TypeParameterMatcher.get(inboundMessageType);
    }

    /**
     * Returns {@code true} if the given message should be handled. If {@code false} it will be passed to the next
     * {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     */
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return matcher.match(msg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        CodecOutputList out = CodecOutputList.newInstance();
        try {
            if (acceptInboundMessage(msg)) {
                @SuppressWarnings("unchecked")
                I cast = (I) msg;
                try {
                    decode(ctx, cast, out);
                } finally {
                    ReferenceCountUtil.release(cast);
                }
            } else {
                out.add(msg);
            }
        } catch (DecoderException e) {
            throw e;
        } catch (Exception e) {
            throw new DecoderException(e);
        } finally {
            int size = out.size();
            for (int i = 0; i < size; i++) {
                ctx.fireChannelRead(out.getUnsafe(i));
            }
            out.recycle();
        }
    }

    /**
     * Decode from one message to an other. This method will be called for each written message that can be handled by this encoder.
     *
     * 从一条消息解码为另一条消息。对于此编码器可以处理的每条书面消息，将调用此方法。
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link MessageToMessageDecoder} belongs to
     * @param msg the message to decode to an other one
     * @param out the {@link List} to which decoded messages should be added
     * @param <I> 类型参数 I 指定了 decode()方法的输入参数 msg 的类型，它是你必须实现的唯一方法。
     * @throws Exception is thrown if an error occurs
     *
     * 对于每个需要被解码为另一种格式的入站消息来说，该方法都 将会被调用。
     * 解码消息随后会被传递给 ChannelPipeline 中的下一个 ChannelInboundHandler
     */
    protected abstract void decode(ChannelHandlerContext ctx, I msg, List<Object> out) throws Exception;
}
