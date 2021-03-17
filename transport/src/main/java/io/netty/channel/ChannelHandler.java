package io.netty.channel;

import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * AttributeKey跟@Sharable注释可以配合使用，使一个处理器实例可以添加到不同的Channel
 * <p></p>
 * ChannelInboundHandler,ChannelOutboundHandler
 * </p>
 * Handles an I/O event or intercepts an I/O operation, and forwards it to its next handler in
 * its {@link ChannelPipeline}.
 *
 * 处理I / O事件或拦截I / O操作，并将其转发到其ChannelPipeline中的下一个处理程序。
 *
 * <h3>Sub-types</h3>
 * <p>
 * {@link ChannelHandler} itself does not provide many methods, but you usually have to implement one of its subtypes:
 * <ul>
 * <li>{@link ChannelInboundHandler} to handle inbound I/O events, and</li>
 * <li>{@link ChannelOutboundHandler} to handle outbound I/O operations.</li>
 * </ul>
 * </p>
 * <p>
 * Alternatively, the following adapter classes are provided for your convenience:
 * <ul>
 * <li>{@link ChannelInboundHandlerAdapter} to handle inbound I/O events,</li>
 * <li>{@link ChannelOutboundHandlerAdapter} to handle outbound I/O operations, and</li>
 * <li>{@link ChannelDuplexHandler} to handle both inbound and outbound events</li>
 * </ul>
 * </p>
 * <p>
 * For more information, please refer to the documentation of each subtype.
 * </p>
 *
 * <h3>The context object</h3>
 * 上下文对象，
 * ChannelHandler随ChannelHandlerContext对象一起提供。
 * ChannelHandler应该通过上下文对象与其所属的ChannelPipeline进行交互
 * <p>
 * A {@link ChannelHandler} is provided with a {@link ChannelHandlerContext}
 * object.  A {@link ChannelHandler} is supposed to interact with the
 * {@link ChannelPipeline} it belongs to via a context object.  Using the
 * context object, the {@link ChannelHandler} can pass events upstream or
 * downstream, modify the pipeline dynamically, or store the information
 * (using {@link AttributeKey}s) which is specific to the handler.
 *
 * <h3>State management</h3>
 *
 * A {@link ChannelHandler} often needs to store some stateful information.
 * The simplest and recommended approach is to use member variables:
 * （ChannelHandler通常需要存储一些状态信息。推荐的最简单方法是使用成员变量：）
 * <pre>
 * public interface Message {
 *     // your methods here
 * }
 *
 * public class DataServerHandler extends {@link SimpleChannelInboundHandler}&lt;Message&gt; {
 *
 *     <b>private boolean loggedIn;</b>
 *
 *     {@code @Override}
 *     public void channelRead0({@link ChannelHandlerContext} ctx, Message message) {
 *         {@link Channel} ch = e.getChannel();
 *         if (message instanceof LoginMessage) {
 *             authenticate((LoginMessage) message);
 *             <b>loggedIn = true;</b>
 *         } else (message instanceof GetDataMessage) {
 *             if (<b>loggedIn</b>) {
 *                 ch.write(fetchSecret((GetDataMessage) message));
 *             } else {
 *                 fail();
 *             }
 *         }
 *     }
 *     ...
 * }
 * </pre>
 * Because the handler instance has a state variable which is dedicated to
 * one connection, you have to create a new handler instance for each new
 * channel to avoid a race condition where a unauthenticated client can get
 * the confidential information:
 * <pre>
 * // Create a new handler instance per channel.
 * // See {@link ChannelInitializer#initChannel(Channel)}.
 * public class DataServerInitializer extends {@link ChannelInitializer}&lt;{@link Channel}&gt; {
 *     {@code @Override}
 *     public void initChannel({@link Channel} channel) {
 *         channel.pipeline().addLast("handler", <b>new DataServerHandler()</b>);
 *     }
 * }
 *
 * </pre>
 *
 * <h4>Using {@link AttributeKey}s</h4>
 *
 * Although it's recommended to use member variables to store the state of a
 * handler, for some reason you might not want to create many handler instances.
 * In such a case, you can use {@link AttributeKey}s which is provided by
 * {@link ChannelHandlerContext}:
 * <pre>
 * public interface Message {
 *     // your methods here
 * }
 *
 * {@code @Sharable}
 * public class DataServerHandler extends {@link SimpleChannelInboundHandler}&lt;Message&gt; {
 *     private final {@link AttributeKey}&lt;{@link Boolean}&gt; auth =
 *           {@link AttributeKey#valueOf(String) AttributeKey.valueOf("auth")};
 *
 *     {@code @Override}
 *     public void channelRead({@link ChannelHandlerContext} ctx, Message message) {
 *         {@link Attribute}&lt;{@link Boolean}&gt; attr = ctx.attr(auth);
 *         {@link Channel} ch = ctx.channel();
 *         if (message instanceof LoginMessage) {
 *             authenticate((LoginMessage) o);
 *             <b>attr.set(true)</b>;
 *         } else (message instanceof GetDataMessage) {
 *             if (<b>Boolean.TRUE.equals(attr.get())</b>) {
 *                 ch.write(fetchSecret((GetDataMessage) o));
 *             } else {
 *                 fail();
 *             }
 *         }
 *     }
 *     ...
 * }
 * </pre>
 * Now that the state of the handler is attached to the {@link ChannelHandlerContext}, you can add the
 * same handler instance to different pipelines:
 * <pre>
 * public class DataServerInitializer extends {@link ChannelInitializer}&lt;{@link Channel}&gt; {
 *
 *     private static final DataServerHandler <b>SHARED</b> = new DataServerHandler();
 *
 *     {@code @Override}
 *     public void initChannel({@link Channel} channel) {
 *         channel.pipeline().addLast("handler", <b>SHARED</b>);
 *     }
 * }
 * </pre>
 *
 *
 * <h4>The {@code @Sharable} annotation</h4>
 * <p>
 * In the example above which used an {@link AttributeKey},
 * you might have noticed the {@code @Sharable} annotation.
 * <p>
 * If a {@link ChannelHandler} is annotated with the {@code @Sharable}
 * annotation, it means you can create an instance of the handler just once and
 * add it to one or more {@link ChannelPipeline}s multiple times without
 * a race condition.
 * <p>
 * If this annotation is not specified, you have to create a new handler
 * instance every time you add it to a pipeline because it has unshared state
 * such as member variables.
 * <p>
 * This annotation is provided for documentation purpose, just like
 * <a href="http://www.javaconcurrencyinpractice.com/annotations/doc/">the JCIP annotations</a>.
 *
 * <h3>Additional resources worth reading</h3>
 * <p>
 * Please refer to the {@link ChannelHandler}, and
 * {@link ChannelPipeline} to find out more about inbound and outbound operations,
 * what fundamental differences they have, how they flow in a  pipeline,  and how to handle
 * the operation in your application.
 */
public interface ChannelHandler {

    /**
     * ChannelHandler,它是一个接口族的父接口,它的实现负责接收并响应事件通知。在 Netty 应用程序中，
     * 所有的数据处理逻辑都包含在这些核心抽象的实现中。
     *
     * 在下一章(netty in action 第六章)中，我们将专注于 ChannelHandler，它为你的数据处理逻辑提供了载体。因为
     * ChannelHandler 大量地使用了 ByteBuf，
     * 你将开始看到 Netty 的整体架构的各个重要部分最 终走到了一起。
     *
     * Netty 定义了下面两个重要的 ChannelHandler 子接口:
     * ChannelInboundHandler——处理入站数据以及各种状态变化;
     * ChannelOutboundHandler——处理出站数据并且允许拦截所有的操作。
     * @see ChannelInboundHandler -- 处理入站数据以及各种状态变化
     * @see ChannelOutboundHandler -- 处理出站数据并且允许拦截所有的操作
     *
     * 6.1.5 ChannelHandler 适配器
     *
     * 你可以使用
     * ChannelInboundHandlerAdapter
     * 和 ChannelOutboundHandlerAdapter
     * 类作为自己的 ChannelHandler 的起始点。
     * 这两个适配器分别提供了 ChannelInboundHandler 和 ChannelOutboundHandler 的基本实现。
     * 通过扩展抽象类 ChannelHandlerAdapter，
     * 它们 获得了它们共同的超接口 ChannelHandler 的方法。生成的类的层次结构如图 6-2 所示。
     */

    /**
     * Gets called after the {@link ChannelHandler} was added to the actual context and it's ready to handle events.
     *
     * 当把 ChannelHandler 添加到 ChannelPipeline 中时被调用
     */
    void handlerAdded(ChannelHandlerContext ctx) throws Exception;

    /**
     * Gets called after the {@link ChannelHandler} was removed from the actual context and it doesn't handle events
     * anymore.
     *
     * 当从 ChannelPipeline 中移除 ChannelHandler 时被调用
     */
    void handlerRemoved(ChannelHandlerContext ctx) throws Exception;

    /**
     * Gets called if a {@link Throwable} was thrown.
     *
     * @deprecated is part of {@link ChannelInboundHandler}
     *
     * 当处理过程中在 ChannelPipeline 中有错误产生时被调用
     *
     * 总结一下:
     * ChannelHandler.exceptionCaught()的默认实现是简单地将当前异常转发给
     * ChannelPipeline 中的下一个 ChannelHandler;
     * 如果异常到达了 ChannelPipeline 的尾端，它将会被记录为未被处理;
     * 要想定义自定义的处理逻辑，你需要重写 exceptionCaught()方法。然后你需要决定
     *     是否需要将该异常传播出去。
     */
    @Deprecated
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;

    /**
     * Indicates that the same instance of the annotated {@link ChannelHandler}
     * can be added to one or more {@link ChannelPipeline}s multiple times
     * without a race condition.
     * <p>
     * If this annotation is not specified, you have to create a new handler
     * instance every time you add it to a pipeline because it has unshared
     * state such as member variables.
     * <p>
     * This annotation is provided for documentation purpose, just like
     * <a href="http://www.javaconcurrencyinpractice.com/annotations/doc/">the JCIP annotations</a>.
     */
    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sharable {
        // no value
        /**
         * 为何要共享同一个ChannelHandler 在多个ChannelPipeline中安装同一个ChannelHandler
         * 的一个常见的原因是用于收集跨越多个 Channel 的统计信息。
         */
    }
}
