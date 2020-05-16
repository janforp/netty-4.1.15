package io.netty.channel;

public interface ChannelInboundInvoker {

    /**
     * Channel 已注册到 EventLoop的时候触发,
     * 这将导致Channel的ChannelPipeline中包含的下一个ChannelInboundHandler调用ChannelInboundHandler.channelRegistered（ChannelHandlerContext）方法。
     * <p></p>
     * 一个 ChannelPipeline 中有很多的 处理器
     * <p>
     * A {@link Channel} was registered to its {@link EventLoop}.
     *
     * This will result in having the  {@link ChannelInboundHandler#channelRegistered(ChannelHandlerContext)} method
     * called of the next  {@link ChannelInboundHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     */
    ChannelInboundInvoker fireChannelRegistered();

    /**
     * 事件传播给方法：该 Channel 的 ChannelPipeline 中的下一个ChannelInboundHandler，并且调用他的
     * {@link ChannelInboundHandler#channelUnregistered(ChannelHandlerContext)}
     * <p>
     * A {@link Channel} was unregistered from its {@link EventLoop}.
     *
     * This will result in having the  {@link ChannelInboundHandler#channelUnregistered(ChannelHandlerContext)} method
     * called of the next  {@link ChannelInboundHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     */
    ChannelInboundInvoker fireChannelUnregistered();

    /**
     * 连接建立的时候
     * <p>
     * A {@link Channel} is active now, which means it is connected.
     *
     * This will result in having the  {@link ChannelInboundHandler#channelActive(ChannelHandlerContext)} method
     * called of the next  {@link ChannelInboundHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     */
    ChannelInboundInvoker fireChannelActive();

    /**
     * A {@link Channel} is inactive now, which means it is closed.
     *
     * This will result in having the  {@link ChannelInboundHandler#channelInactive(ChannelHandlerContext)} method
     * called of the next  {@link ChannelInboundHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     */
    ChannelInboundInvoker fireChannelInactive();

    /**
     * A {@link Channel} received an {@link Throwable} in one of its inbound operations.
     *
     * This will result in having the  {@link ChannelInboundHandler#exceptionCaught(ChannelHandlerContext, Throwable)}
     * method  called of the next  {@link ChannelInboundHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     */
    ChannelInboundInvoker fireExceptionCaught(Throwable cause);

    /**
     * 用户事件触发
     * <p></p>
     * A {@link Channel} received an user defined event.
     *
     * This will result in having the  {@link ChannelInboundHandler#userEventTriggered(ChannelHandlerContext, Object)}
     * method  called of the next  {@link ChannelInboundHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     */
    ChannelInboundInvoker fireUserEventTriggered(Object event);

    /**
     * 收到数据
     * <p></p>
     * A {@link Channel} received a message.
     *
     * This will result in having the {@link ChannelInboundHandler#channelRead(ChannelHandlerContext, Object)}
     * method  called of the next {@link ChannelInboundHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     */
    ChannelInboundInvoker fireChannelRead(Object msg);

    /**
     * Triggers an {@link ChannelInboundHandler#channelReadComplete(ChannelHandlerContext)}
     * event to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     */
    ChannelInboundInvoker fireChannelReadComplete();

    /**
     * Triggers an {@link ChannelInboundHandler#channelWritabilityChanged(ChannelHandlerContext)}
     * event to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     */
    ChannelInboundInvoker fireChannelWritabilityChanged();
}
