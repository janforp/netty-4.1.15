package io.netty.channel;

/**
 * 适配器模式
 *
 * 这个类的方法实现原则；直接调用 ChannelPipeline 中的下一个处理器，所以对于当前处理器来说这是一个空实现
 *
 * 如果用户只对处理器接口中的某一个或者几个方法感兴趣，则只需要继承该适配器，然后重写感兴趣的方法即可，其他的方法不做任何逻辑，直接传播给下一个处理器即可
 *
 * @see SimpleChannelInboundHandler
 *
 *
 * <p></p>
 * Abstract base class for {@link ChannelInboundHandler} implementations which provide
 * implementations of all of their methods.
 *
 * <p>
 * This implementation just forward the operation to the next {@link ChannelHandler} in the
 * {@link ChannelPipeline}. Sub-classes may override a method implementation to change this.
 * </p>
 * <p>
 * Be aware that messages are not released after the {@link #channelRead(ChannelHandlerContext, Object)}
 * method returns automatically. If you are looking for a {@link ChannelInboundHandler} implementation that
 * releases the received messages automatically, please see {@link SimpleChannelInboundHandler}.
 * </p>
 *
 * <<netty in action>>
 * 因为你的 Echo 服务器会响应传入的消息，所以它需要实现 ChannelInboundHandler 接口，
 * 用 来定义响应入站事件的方法。这个简单的应用程序只需要用到少量的这些方法，所以继承 ChannelInboundHandlerAdapter 类也就足够了，它提供了 ChannelInboundHandler 的默认实现。
 *
 * ChannelInboundHandlerAdapter 有一个直观的 API，并且它的每个方法都可以被重写以 挂钩到事件生命周期的恰当点上
 *
 *
 * 为什么需要适配器类
 * 有一些适配器类可以将编写自定义的 ChannelHandler 所需要的努力降到最低限度，因为它们提 供了定义在对应接口中的所有方法的默认实现。
 * 下面这些是编写自定义 ChannelHandler 时经常会用到的适配器类:
 *  ChannelHandlerAdapter
 *  ChannelInboundHandlerAdapter
 *  ChannelOutboundHandlerAdapter
 *  ChannelDuplexHandler
 */
public class ChannelInboundHandlerAdapter

        /**
         * 实现了 ChannelHandler 的方法
         */
        extends ChannelHandlerAdapter

        /**
         * 处理入站数据以及各种状态变化
         */
        implements ChannelInboundHandler {

    /**
     * 这个类实现类{@link ChannelInboundHandler} 的所有方法
     * 但是这个都是啥都不做的实现，只是单纯的通过{@link ChannelHandlerContext}
     * 把事件 fireXXX 到ChannelPipeline的下一个ChannelHandler处理器
     *
     * 在 ChannelInboundHandlerAdapter 和 ChannelOutboundHandlerAdapter
     * 中所 提供的方法体调用了其相关联的 ChannelHandlerContext 上的等效方法，
     * 从而将事件转发到 了 ChannelPipeline 中的下一个 ChannelHandler 中。
     *
     * 你要想在自己的 ChannelHandler 中使用这些适配器类，
     * 只需要简单地扩展它们，并且重 写那些你想要自定义的方法。
     */
    // ************************************************************************************************************

    /**
     * Calls {@link ChannelHandlerContext#fireChannelRegistered()} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        //直接调用 ChannelPipeline 中的下一个处理器，所以对于当前处理器来说这是一个空实现
        ctx.fireChannelRegistered();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelUnregistered()} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelUnregistered();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelActive()} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelInactive()} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelRead(Object)} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     *
     * 对于每个传入的消息都要调用
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.fireChannelRead(msg);
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelReadComplete()} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelReadComplete();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireUserEventTriggered(Object)} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        ctx.fireUserEventTriggered(evt);
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelWritabilityChanged()} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelWritabilityChanged();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireExceptionCaught(Throwable)} to forward
     * to the next {@link ChannelHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.fireExceptionCaught(cause);
    }
}
