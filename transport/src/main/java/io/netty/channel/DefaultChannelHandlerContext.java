package io.netty.channel;

import io.netty.util.concurrent.EventExecutor;

/**
 * 默认的 AbstractChannelHandlerContext
 *
 * @see DefaultChannelPipeline#newContext(io.netty.util.concurrent.EventExecutorGroup, java.lang.String, io.netty.channel.ChannelHandler)
 *
 * DefaultChannelPipeline 中就是使用该类型的实例
 */
final class DefaultChannelHandlerContext extends AbstractChannelHandlerContext {

    /**
     * 该处理器上下文封装的处理器
     */
    private final ChannelHandler handler;

    DefaultChannelHandlerContext(DefaultChannelPipeline pipeline, EventExecutor executor, String name, ChannelHandler handler) {

        super(pipeline, executor, name, isInbound(handler), isOutbound(handler));

        if (handler == null) {
            throw new NullPointerException("handler");
        }
        this.handler = handler;
    }

    @Override
    public ChannelHandler handler() {
        return handler;
    }

    /**
     * 是不是入站处理器
     *
     * @param handler
     * @return
     */
    private static boolean isInbound(ChannelHandler handler) {
        return handler instanceof ChannelInboundHandler;
    }

    /**
     * 是不是出站处理器
     *
     * @param handler
     * @return
     */
    private static boolean isOutbound(ChannelHandler handler) {
        return handler instanceof ChannelOutboundHandler;
    }
}
