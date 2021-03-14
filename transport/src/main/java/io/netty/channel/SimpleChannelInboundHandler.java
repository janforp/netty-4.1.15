package io.netty.channel;

import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;

/**
 * ChannelInboundHandlerAdapter，它允许仅显式处理特定类型的消息
 * 而他的父类并不是一个泛型类，这个类避免了类型转换
 *
 *
 * <p></p>
 * {@link ChannelInboundHandlerAdapter} which allows to explicit only handle a specific type of messages.
 *
 * For example here is an implementation which only handle {@link String} messages.
 *
 * <pre>
 *     public class StringHandler extends
 *             {@link SimpleChannelInboundHandler}&lt;{@link String}&gt; {
 *
 *         {@code @Override}
 *         protected void channelRead0({@link ChannelHandlerContext} ctx, {@link String} message)
 *                 throws {@link Exception} {
 *             System.out.println(message);
 *         }
 *     }
 * </pre>
 *
 * Be aware that depending of the constructor parameters it will release all handled messages by passing them to
 * {@link ReferenceCountUtil#release(Object)}. In this case you may need to use
 * {@link ReferenceCountUtil#retain(Object)} if you pass the object to the next handler in the {@link ChannelPipeline}.
 *
 * <h3>Forward compatibility notice</h3>
 * <p>
 * Please keep in mind that {@link #channelRead0(ChannelHandlerContext, I)} will be renamed to
 * {@code messageReceived(ChannelHandlerContext, I)} in 5.0.
 * </p>
 */
public abstract class SimpleChannelInboundHandler<I>

        /**
         * {@link ChannelInboundHandlerAdapter}这个类实
         * 现类{@link ChannelInboundHandler} 的所有方法
         * 但是这个都是啥都不做的实现，只是单纯的通过{@link ChannelHandlerContext}
         * 把事件 fireXXX 到ChannelPipeline的下一个ChannelHandler处理器
         */
        extends ChannelInboundHandlerAdapter {

    /**
     * 看看消息（类型）是否匹配
     */
    private final TypeParameterMatcher matcher;

    private final boolean autoRelease;

    /**
     * see {@link #SimpleChannelInboundHandler(boolean)} with {@code true} as boolean parameter.
     */
    protected SimpleChannelInboundHandler() {
        this(true);
    }

    /**
     * Create a new instance which will try to detect the types to match out of the type parameter of the class.
     *
     * @param autoRelease {@code true} if handled messages should be released automatically by passing them to
     * {@link ReferenceCountUtil#release(Object)}.
     */
    protected SimpleChannelInboundHandler(boolean autoRelease) {
        matcher = TypeParameterMatcher.find(this, SimpleChannelInboundHandler.class, "I");
        this.autoRelease = autoRelease;
    }

    /**
     * see {@link #SimpleChannelInboundHandler(Class, boolean)} with {@code true} as boolean value.
     */
    protected SimpleChannelInboundHandler(Class<? extends I> inboundMessageType) {
        this(inboundMessageType, true);
    }

    /**
     * Create a new instance
     *
     * @param inboundMessageType The type of messages to match
     * @param autoRelease {@code true} if handled messages should be released automatically by passing them to
     * {@link ReferenceCountUtil#release(Object)}.
     */
    protected SimpleChannelInboundHandler(Class<? extends I> inboundMessageType, boolean autoRelease) {
        matcher = TypeParameterMatcher.get(inboundMessageType);
        this.autoRelease = autoRelease;
    }

    /**
     * Returns {@code true} if the given message should be handled. If {@code false} it will be passed to the next
     * {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     */
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return matcher.match(msg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {

        //对于每个传入的消息都要调用

        boolean release = true;
        try {
            if (acceptInboundMessage(msg)) {
                @SuppressWarnings("unchecked")
                I imsg = (I) msg;
                //模版方法设计模式
                //该方法推迟到子类执行
                channelRead0(ctx, imsg);
            } else {
                //不释放，让下一个处理器处理该消息
                release = false;
                ctx.fireChannelRead(msg);
            }
        } finally {
            //由于 SimpleChannelInboundHandler 会自动释放资源，
            // 所以你不应该存储指向任何消 息的引用供将来使用，因为这些引用都将会失效。
            if (autoRelease && release) {
                //对该对象(msg)的引用数 -1
                //最好不要在用户代码中保持该对象的引用
                //如果收到该消息之后，业务逻辑是异步的，并且业务逻辑中还使用了该消息，那么就可能存在，业务逻辑还没有
                //处理完毕的情况，该消息被 release 了，这样就可能会产生问题
                ReferenceCountUtil.release(msg);
            }
        }
    }

    /**
     * 这个方法是用户必须要实现的方法
     *
     * <p></p>
     *
     * <strong>Please keep in mind that this method will be renamed to
     * {@code messageReceived(ChannelHandlerContext, I)} in 5.0.</strong>
     *
     * Is called for each message of type {@link I}.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
     * belongs to
     * @param msg the message to handle
     * @throws Exception is thrown if an error occurred
     */
    protected abstract void channelRead0(ChannelHandlerContext ctx, I msg) throws Exception;
}
