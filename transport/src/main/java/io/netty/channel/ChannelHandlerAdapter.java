package io.netty.channel;

import io.netty.util.internal.InternalThreadLocalMap;

import java.util.Map;

/**
 * ChannelHandler 的一个骨架实现
 *
 * 适配器模式
 *
 * <p></p>
 * Skeleton implementation of a {@link ChannelHandler}.
 *
 * 为什么需要适配器类
 * 有一些适配器类可以将编写自定义的 ChannelHandler 所需要的努力降到最低限度，因为它们提 供了定义在对应接口中的所有方法的默认实现。
 * 下面这些是编写自定义 ChannelHandler 时经常会用到的适配器类:
 *  ChannelHandlerAdapter
 *  ChannelInboundHandlerAdapter
 *  ChannelOutboundHandlerAdapter
 *  ChannelDuplexHandler
 */
public abstract class ChannelHandlerAdapter implements ChannelHandler {

    // Not using volatile because it's used only for a sanity check.
    //不使用volatile，因为它仅用于完整性检查。
    boolean added;

    /**
     * 该方法确保当前处理器不能共享
     *
     * Throws {@link IllegalStateException} if {@link ChannelHandlerAdapter#isSharable()} returns {@code true}
     */
    protected void ensureNotSharable() {
        if (isSharable()) {
            throw new IllegalStateException("ChannelHandler " + getClass().getName() + " is not allowed to be shared");
        }
    }

    /**
     * 如果实现是ChannelHandler.Sharable，则返回true，因此同一个处理器实例可以将其添加到不同的ChannelPipelines中。
     *
     * Return {@code true} if the implementation is {@link Sharable} and so can be added
     * to different {@link ChannelPipeline}s.
     */
    public boolean isSharable() {
        /**
         * Cache the result of {@link Sharable} annotation detection to workaround a condition. We use a
         * {@link ThreadLocal} and {@link WeakHashMap} to eliminate the volatile write/reads. Using different
         * {@link WeakHashMap} instances per {@link Thread} is good enough for us and the number of
         * {@link Thread}s are quite limited anyway.
         *
         * See <a href="https://github.com/netty/netty/issues/2289">#2289</a>.
         */
        //拿到实现的类型
        Class<?> clazz = getClass();
        //缓存起来，提供性能
        Map<Class<?>, Boolean> cache = InternalThreadLocalMap.get().handlerSharableCache();
        Boolean sharable = cache.get(clazz);
        if (sharable == null) {
            sharable = clazz.isAnnotationPresent(Sharable.class);
            //丢如缓存，下一次就不会再判断啦
            cache.put(clazz, sharable);
        }
        return sharable;
    }

    /**
     * 让子类实现具体的逻辑
     *
     * <p></p>
     * Do nothing by default, sub-classes may override this method.
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // NOOP
    }

    /**
     * 让子类实现具体的逻辑
     * <p></p>
     * Do nothing by default, sub-classes may override this method.
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // NOOP
    }

    /**
     * 调用ChannelHandlerContext.fireExceptionCaught（Throwable）以转发到ChannelPipeline中的下一个ChannelHandler。子类可以重写此方法以更改行为。
     *
     * <p></p>
     * Calls {@link ChannelHandlerContext#fireExceptionCaught(Throwable)} to forward
     * to the next {@link ChannelHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.fireExceptionCaught(cause);
    }
}
