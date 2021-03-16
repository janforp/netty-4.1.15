package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;
import io.netty.util.concurrent.EventExecutor;

import java.nio.channels.Channels;

/**
 * 如您在图中所示，您可能会注意到，处理程序必须调用 ChannelHandlerContext 中的事件传播方法以将事件转发到其下一个处理程序
 * 图：是指 ChannelPipeline 中的图
 * 那么该  ChannelHandlerContext 中就定义了很多事件传播方法
 *
 * <p></p>
 * Enables a {@link ChannelHandler} to interact with its {@link ChannelPipeline}
 * and other handlers. Among other things a handler can notify the next {@link ChannelHandler} in the
 * {@link ChannelPipeline} as well as modify the {@link ChannelPipeline} it belongs to dynamically.
 *
 * <h3>Notify</h3>
 *
 * You can notify the closest handler in the same {@link ChannelPipeline} by calling one of the various methods
 * provided here.
 *
 * Please refer to {@link ChannelPipeline} to understand how an event flows.
 *
 * <h3>Modifying a pipeline</h3>
 *
 * You can get the {@link ChannelPipeline} your handler belongs to by calling
 * {@link #pipeline()}.  A non-trivial application could insert, remove, or
 * replace handlers in the pipeline dynamically at runtime.
 *
 * <h3>Retrieving for later use</h3>
 *
 * You can keep the {@link ChannelHandlerContext} for later use, such as
 * triggering an event outside the handler methods, even from a different thread.
 * <pre>
 * public class MyHandler extends {@link ChannelDuplexHandler} {
 *
 *     <b>private {@link ChannelHandlerContext} ctx;</b>
 *
 *     public void beforeAdd({@link ChannelHandlerContext} ctx) {
 *         <b>this.ctx = ctx;</b>
 *     }
 *
 *     public void login(String username, password) {
 *         ctx.write(new LoginMessage(username, password));
 *     }
 *     ...
 * }
 * </pre>
 *
 * <h3>Storing stateful information</h3>
 *
 * {@link #attr(AttributeKey)} allow you to
 * store and access stateful information that is related with a handler and its
 * context.  Please refer to {@link ChannelHandler} to learn various recommended
 * ways to manage stateful information.
 *
 * <h3>A handler can have more than one context</h3>
 *
 * Please note that a {@link ChannelHandler} instance can be added to more than
 * one {@link ChannelPipeline}.  It means a single {@link ChannelHandler}
 * instance can have more than one {@link ChannelHandlerContext} and therefore
 * the single instance can be invoked with different
 * {@link ChannelHandlerContext}s if it is added to one or more
 * {@link ChannelPipeline}s more than once.
 * <p>
 * For example, the following handler will have as many independent {@link AttributeKey}s
 * as how many times it is added to pipelines, regardless if it is added to the
 * same pipeline multiple times or added to different pipelines multiple times:
 * <pre>
 * public class FactorialHandler extends {@link ChannelInboundHandlerAdapter} {
 *
 *   private final {@link AttributeKey}&lt;{@link Integer}&gt; counter = {@link AttributeKey}.valueOf("counter");
 *
 *   // This handler will receive a sequence of increasing integers starting
 *   // from 1.
 *   {@code @Override}
 *   public void channelRead({@link ChannelHandlerContext} ctx, Object msg) {
 *     Integer a = ctx.attr(counter).get();
 *
 *     if (a == null) {
 *       a = 1;
 *     }
 *
 *     attr.set(a * (Integer) msg);
 *   }
 * }
 *
 * // Different context objects are given to "f1", "f2", "f3", and "f4" even if
 * // they refer to the same handler instance.  Because the FactorialHandler
 * // stores its state in a context object (using an {@link AttributeKey}), the factorial is
 * // calculated correctly 4 times once the two pipelines (p1 and p2) are active.
 * FactorialHandler fh = new FactorialHandler();
 *
 * {@link ChannelPipeline} p1 = {@link Channels}.pipeline();
 * p1.addLast("f1", fh);
 * p1.addLast("f2", fh);
 *
 * {@link ChannelPipeline} p2 = {@link Channels}.pipeline();
 * p2.addLast("f3", fh);
 * p2.addLast("f4", fh);
 * </pre>
 *
 * <h3>Additional resources worth reading</h3>
 * <p>
 * Please refer to the {@link ChannelHandler}, and
 * {@link ChannelPipeline} to find out more about inbound and outbound operations,
 * what fundamental differences they have, how they flow in a  pipeline,  and how to handle
 * the operation in your application.
 */
public interface ChannelHandlerContext
        extends AttributeMap, ChannelInboundInvoker, ChannelOutboundInvoker {
    /**
     * ChannelHandlerContext使得ChannelHandler能够和它的ChannelPipeline以及其他的 ChannelHandler 交 互 。
     * ChannelHandler 可 以 通 知 其 所 属 的 ChannelPipeline 中 的 下 一 个 ChannelHandler，甚至可以动态修改它所属的ChannelPipeline。
     * ChannelHandlerContext 具有丰富的用于处理事件和执行 I/O 操作的 API。6.3 节将提供有关 ChannelHandlerContext 的更多内容。
     */

    /**
     * ChannelHandlerContext 代表了 ChannelHandler 和 ChannelPipeline 之间的关 联，
     * 每当有 ChannelHandler 添加到 ChannelPipeline 中时，都会创建 ChannelHandlerContext。
     * ChannelHandlerContext 的主要功能是管理它所关联的 ChannelHandler 和在 同一个 ChannelPipeline 中的其他 ChannelHandler 之间的交互。
     *
     * ChannelHandlerContext 有很多的方法，其中一些方法也存在于 Channel 和 ChannelPipeline 本身上，但是有一点重要的不同。
     * 如果调用 Channel 或者 ChannelPipeline 上的这 些方法，它们将沿着整个 ChannelPipeline 进行传播。
     * 而调用位于 ChannelHandlerContext 上的相同方法，则将从当前所关联的 ChannelHandler 开始，并且只会传播给位于该 ChannelPipeline 中的下一个能够处理该事件的 ChannelHandler。
     *
     * 当使用 ChannelHandlerContext 的 API 的时候，请牢记以下两点:
     * ChannelHandlerContext 和 ChannelHandler 之间的关联(绑定)是永远不会改变的，所以缓存对它的引用是安全的;
     * 如同我们在本节开头所解释的一样，相对于其他类的同名方法，ChannelHandlerContext的方法将产生更短的事件流，应该尽可能地利用这个特性来获得最大的性能。
     */

    /**
     * Return the {@link Channel} which is bound to the {@link ChannelHandlerContext}.
     *
     * 返回绑定到这个实例的 Channel
     */
    Channel channel();

    /**
     * Returns the {@link EventExecutor} which is used to execute an arbitrary task.
     *
     * 返回调度事件的 EventExecutor
     */
    EventExecutor executor();

    /**
     * 返回这个实例的唯一名称
     *
     * The unique name of the {@link ChannelHandlerContext}.The name was used when then {@link ChannelHandler}
     * was added to the {@link ChannelPipeline}. This name can also be used to access the registered
     * {@link ChannelHandler} from the {@link ChannelPipeline}.
     */
    String name();

    /**
     * The {@link ChannelHandler} that is bound this {@link ChannelHandlerContext}.
     */
    ChannelHandler handler();

    /**
     * 如果所关联的 ChannelHandler 已经被从 ChannelPipeline 中移除则返回 true
     *
     * Return {@code true} if the {@link ChannelHandler} which belongs to this context was removed
     * from the {@link ChannelPipeline}. Note that this method is only meant to be called from with in the
     * {@link EventLoop}.
     */
    boolean isRemoved();

    //触发对下一个 ChannelInboundHandler 上的 fireChannelRegistered()方法的调用
    @Override
    ChannelHandlerContext fireChannelRegistered();

    //触发对下一个 ChannelInboundHandler 上的 fireChannelUnregistered()方法的调用
    @Override
    ChannelHandlerContext fireChannelUnregistered();

    //触发对下一个 ChannelInboundHandler 上的 channelActive()方法(已连接)的调用
    @Override
    ChannelHandlerContext fireChannelActive();

    //触发对下一个 ChannelInboundHandler 上的 channelInactive()方法(已关闭)的调用
    @Override
    ChannelHandlerContext fireChannelInactive();

    @Override
    ChannelHandlerContext fireExceptionCaught(Throwable cause);

    //触发对下一个 ChannelInboundHandler 上的 fireUserEventTriggered(Object evt)方法的调用
    @Override
    ChannelHandlerContext fireUserEventTriggered(Object evt);

    //触发对下一个 ChannelInboundHandler 上的 channelRead()方法(已接收的消息)的调用
    @Override
    ChannelHandlerContext fireChannelRead(Object msg);

    //触发对下一个 ChannelInboundHandler 上的 channelReadComplete()方法的调用
    @Override
    ChannelHandlerContext fireChannelReadComplete();

    //触发对下一个ChannelInboundHandler上的
    //  fireExceptionCaught fireUserEventTriggered
    //fireChannelWritabilityChanged()方法的调用
    @Override
    ChannelHandlerContext fireChannelWritabilityChanged();

    //将数据从Channel读取到第一个入站缓冲区;如果读取成功则触
    //发 一个channelRead事件，并(在最后一个消息被读取完成后) 通 知 ChannelInboundHandler 的 channelReadComplete (ChannelHandlerContext)方法
    @Override
    ChannelHandlerContext read();

    @Override
    ChannelHandlerContext flush();

    /**
     * 返回这个实例所关联的 ChannelPipeline
     *
     * Return the assigned {@link ChannelPipeline}
     */
    ChannelPipeline pipeline();

    /**
     * Return the assigned {@link ByteBufAllocator} which will be used to allocate {@link ByteBuf}s.
     *
     * 返回和这个实例相关联的 Channel 所配置的 ByteBufAllocator
     */
    ByteBufAllocator alloc();

    /**
     * @deprecated Use {@link Channel#attr(AttributeKey)}
     */
    @Deprecated
    @Override
    <T> Attribute<T> attr(AttributeKey<T> key);

    /**
     * @deprecated Use {@link Channel#hasAttr(AttributeKey)}
     */
    @Deprecated
    @Override
    <T> boolean hasAttr(AttributeKey<T> key);
}
