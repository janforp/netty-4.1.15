package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

/**
 * 一个 ChannelHandler 的列表，用于处理或拦截Channel的入站事件和出站操作。
 * <p></p>
 * ChannelPipeline实现了Intercepting Filter模式的高级形式，以使用户可以完全控制事件的处理方式以及管道中的ChannelHandlers如何交互
 * <p></p>
 * 每个 Channel 都有其自己的 ChannelPipeline，并且在创建新 channel 时会自动创建它。
 * 每个 ChannelPipeline 中都有一个 ChannelHandler 列表，专门用来处理该 Channel 的 io 操作
 *
 * <p></p>
 *
 * I / O事件由ChannelInboundHandler或ChannelOutboundHandler处理，
 * 并通过调用ChannelHandlerContext中定义的事件传播方法
 * （例如ChannelHandlerContext.fireChannelRead（Object）和ChannelHandlerContext.write（Object））转发到其最近的处理程序。
 *
 * <p></p>
 *
 * 如您在图中所示，您可能会注意到，处理程序必须调用ChannelHandlerContext中的事件传播方法以将事件转发到其下一个处理程序
 *
 * <p></p>
 *
 * 当该处理器逻辑比较复杂耗时的时候可以使用专门的线程池处理该业务
 *
 * 如果添加处理器的时候不指定线程池，则默认使用当前io线程执行，可能会发生阻塞其他 Channel 的任务的情况
 * <p></p>
 *
 * A list of {@link ChannelHandler}s which handles or intercepts inbound events and outbound operations of a
 * {@link Channel}.  {@link ChannelPipeline} implements an advanced form of the
 * <a href="http://www.oracle.com/technetwork/java/interceptingfilter-142169.html">Intercepting Filter</a> pattern
 * to give a user full control over how an event is handled and how the {@link ChannelHandler}s in a pipeline
 * interact with each other.
 *
 * <h3>Creation of a pipeline</h3>
 *
 * Each channel has its own pipeline and it is created automatically when a new channel is created.
 *
 * <h3>How an event flows in a pipeline</h3>
 *
 * The following diagram describes how I/O events are processed by {@link ChannelHandler}s in a {@link ChannelPipeline}
 * typically. An I/O event is handled by either a {@link ChannelInboundHandler} or a {@link ChannelOutboundHandler}
 * and be forwarded to its closest handler by calling the event propagation methods defined in
 * {@link ChannelHandlerContext}, such as {@link ChannelHandlerContext#fireChannelRead(Object)} and
 * {@link ChannelHandlerContext#write(Object)}.
 *
 * <pre>
 *                                                 I/O Request
 *                                            via {@link Channel} or
 *                                        {@link ChannelHandlerContext}
 *                                                      |
 *  +---------------------------------------------------+---------------+
 *  |                           ChannelPipeline         |               |
 *  |                                                  \|/              |
 *  |    +---------------------+            +-----------+----------+    |
 *  |    | Inbound Handler  N  |            | Outbound Handler  1  |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  |               |
 *  |               |                                  \|/              |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |    | Inbound Handler N-1 |            | Outbound Handler  2  |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  .               |
 *  |               .                                   .               |
 *  | ChannelHandlerContext.fireIN_EVT() ChannelHandlerContext.OUT_EVT()|
 *  |        [ method call]                       [method call]         |
 *  |               .                                   .               |
 *  |               .                                  \|/              |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |    | Inbound Handler  2  |            | Outbound Handler M-1 |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  |               |
 *  |               |                                  \|/              |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |    | Inbound Handler  1  |            | Outbound Handler  M  |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  |               |
 *  +---------------+-----------------------------------+---------------+
 *                  |                                  \|/
 *  +---------------+-----------------------------------+---------------+
 *  |               |                                   |               |
 *  |       [ Socket.read() ]                    [ Socket.write() ]     |
 *  |                                                                   |
 *  |  Netty Internal I/O Threads (Transport Implementation)            |
 *  +-------------------------------------------------------------------+
 * </pre>
 * An inbound event is handled by the inbound handlers in the bottom-up direction as shown on the left side of the
 * diagram.  An inbound handler usually handles the inbound data generated by the I/O thread on the bottom of the
 * diagram.  The inbound data is often read from a remote peer via the actual input operation such as
 * {@link SocketChannel#read(ByteBuffer)}.  If an inbound event goes beyond the top inbound handler, it is discarded
 * silently, or logged if it needs your attention.
 * <p>
 * An outbound event is handled by the outbound handler in the top-down direction as shown on the right side of the
 * diagram.  An outbound handler usually generates or transforms the outbound traffic such as write requests.
 * If an outbound event goes beyond the bottom outbound handler, it is handled by an I/O thread associated with the
 * {@link Channel}. The I/O thread often performs the actual output operation such as
 * {@link SocketChannel#write(ByteBuffer)}.
 * <p>
 * For example, let us assume that we created the following pipeline:
 * <pre>
 * {@link ChannelPipeline} p = ...;
 * p.addLast("1", new InboundHandlerA());
 * p.addLast("2", new InboundHandlerB());
 * p.addLast("3", new OutboundHandlerA());
 * p.addLast("4", new OutboundHandlerB());
 * p.addLast("5", new InboundOutboundHandlerX());
 * </pre>
 * In the example above, the class whose name starts with {@code Inbound} means it is an inbound handler.
 * The class whose name starts with {@code Outbound} means it is a outbound handler.
 * <p>
 * In the given example configuration, the handler evaluation order is 1, 2, 3, 4, 5 when an event goes inbound.
 * When an event goes outbound, the order is 5, 4, 3, 2, 1.  On top of this principle, {@link ChannelPipeline} skips
 * the evaluation of certain handlers to shorten the stack depth:
 * <ul>
 * <li>3 and 4 don't implement {@link ChannelInboundHandler}, and therefore the actual evaluation order of an inbound
 *     event will be: 1, 2, and 5.</li>
 * <li>1 and 2 don't implement {@link ChannelOutboundHandler}, and therefore the actual evaluation order of a
 *     outbound event will be: 5, 4, and 3.</li>
 * <li>If 5 implements both {@link ChannelInboundHandler} and {@link ChannelOutboundHandler}, the evaluation order of
 *     an inbound and a outbound event could be 125 and 543 respectively.</li>
 * </ul>
 *
 * <h3>Forwarding an event to the next handler</h3>
 *
 * As you might noticed in the diagram shows, a handler has to invoke the event propagation methods in
 * {@link ChannelHandlerContext} to forward an event to its next handler.  Those methods include:
 * <ul>
 * <li>Inbound event propagation methods:
 *     <ul>
 *     <li>{@link ChannelHandlerContext#fireChannelRegistered()}</li>
 *     <li>{@link ChannelHandlerContext#fireChannelActive()}</li>
 *     <li>{@link ChannelHandlerContext#fireChannelRead(Object)}</li>
 *     <li>{@link ChannelHandlerContext#fireChannelReadComplete()}</li>
 *     <li>{@link ChannelHandlerContext#fireExceptionCaught(Throwable)}</li>
 *     <li>{@link ChannelHandlerContext#fireUserEventTriggered(Object)}</li>
 *     <li>{@link ChannelHandlerContext#fireChannelWritabilityChanged()}</li>
 *     <li>{@link ChannelHandlerContext#fireChannelInactive()}</li>
 *     <li>{@link ChannelHandlerContext#fireChannelUnregistered()}</li>
 *     </ul>
 * </li>
 * <li>Outbound event propagation methods:
 *     <ul>
 *     <li>{@link ChannelHandlerContext#bind(SocketAddress, ChannelPromise)}</li>
 *     <li>{@link ChannelHandlerContext#connect(SocketAddress, SocketAddress, ChannelPromise)}</li>
 *     <li>{@link ChannelHandlerContext#write(Object, ChannelPromise)}</li>
 *     <li>{@link ChannelHandlerContext#flush()}</li>
 *     <li>{@link ChannelHandlerContext#read()}</li>
 *     <li>{@link ChannelHandlerContext#disconnect(ChannelPromise)}</li>
 *     <li>{@link ChannelHandlerContext#close(ChannelPromise)}</li>
 *     <li>{@link ChannelHandlerContext#deregister(ChannelPromise)}</li>
 *     </ul>
 * </li>
 * </ul>
 *
 * and the following example shows how the event propagation is usually done:
 *
 * <pre>
 * public class MyInboundHandler extends {@link ChannelInboundHandlerAdapter} {
 *     {@code @Override}
 *     public void channelActive({@link ChannelHandlerContext} ctx) {
 *         System.out.println("Connected!");
 *         ctx.fireChannelActive();
 *     }
 * }
 *
 * public class MyOutboundHandler extends {@link ChannelOutboundHandlerAdapter} {
 *     {@code @Override}
 *     public void close({@link ChannelHandlerContext} ctx, {@link ChannelPromise} promise) {
 *         System.out.println("Closing ..");
 *         ctx.close(promise);
 *     }
 * }
 * </pre>
 *
 * <h3>Building a pipeline</h3>
 * <p>
 * A user is supposed to have one or more {@link ChannelHandler}s in a pipeline to receive I/O events (e.g. read) and
 * to request I/O operations (e.g. write and close).  For example, a typical server will have the following handlers
 * in each channel's pipeline, but your mileage may vary depending on the complexity and characteristics of the
 * protocol and business logic:
 *
 * <p>例如，典型的服务器在每个通道的管道中将具有以下处理程序，但是您的里程可能会因协议和业务逻辑的复杂性和特征而有所不同：</p>
 *
 * <ol>
 * <li>Protocol Decoder - translates binary data (e.g. {@link ByteBuf}) into a Java object.</li>
 * <li>Protocol Encoder - translates a Java object into binary data.</li>
 * <li>Business Logic Handler - performs the actual business logic (e.g. database access).</li>
 * </ol>
 *
 * and it could be represented as shown in the following example:
 *
 * <pre>
 * static final {@link EventExecutorGroup} group = new {@link DefaultEventExecutorGroup}(16);
 * ...
 *
 * {@link ChannelPipeline} pipeline = ch.pipeline();
 *
 * pipeline.addLast("decoder", new MyProtocolDecoder());
 * pipeline.addLast("encoder", new MyProtocolEncoder());
 *
 * // Tell the pipeline to run MyBusinessLogicHandler's event handler methods
 * // in a different thread than an I/O thread so that the I/O thread is not blocked by
 * // a time-consuming task.
 * // If your business logic is fully asynchronous or finished very quickly, you don't
 * // need to specify a group.
 * pipeline.addLast(group, "handler", new MyBusinessLogicHandler());
 * </pre>
 *
 * <h3>Thread safety</h3>
 * <p>
 * A {@link ChannelHandler} can be added or removed at any time because a {@link ChannelPipeline} is thread safe.
 * For example, you can insert an encryption handler when sensitive information is about to be exchanged, and remove it
 * after the exchange.
 *
 * Pipeline:guandao
 *
 * ChannelPipeline 提供了 ChannelHandler 链的容器，并定义了用于在该链上传播入站 和出站事件流的 API。
 * 当 Channel 被创建时，它会被自动地分配到它专属的 ChannelPipeline。
 *
 * ChannelHandler 安装到 ChannelPipeline 中的过程如下所示:
 * 一个ChannelInitializer的实现被注册到了ServerBootstrap中 ;
 * 当 ChannelInitializer.initChannel()方法被调用时，ChannelInitializer将在 ChannelPipeline 中安装一组自定义的 ChannelHandler;
 * ChannelInitializer 将它自己从 ChannelPipeline 中移除。
 *
 * 为了审查发送或者接收数据时将会发生什么，让我们来更加深入地研究 ChannelPipeline 和 ChannelHandler 之间的共生关系吧。
 *
 * ChannelHandler 是专为支持广泛的用途而设计的，可以将它看作是处理往来 ChannelPipeline 事件(包括数据)的任何代码的通用容器。
 *
 * 图 3-2 说明了这一点，其展示了从 ChannelHandler 派生的 ChannelInboundHandler 和 ChannelOutboundHandler 接口。
 *
 * 使得事件流经 ChannelPipeline 是 ChannelHandler 的工作，它们是在应用程序的初 始化或者引导阶段被安装的。这些对象接收事件、执行它们所实现的处理逻辑，并将数据传递给 链中的下一个 ChannelHandler。它们的执行顺序是由它们被添加的顺序所决定的。实际上， 被我们称为 ChannelPipeline 的是这些 ChannelHandler 的编排顺序。
 */
public interface ChannelPipeline
        extends ChannelInboundInvoker,
        ChannelOutboundInvoker,
        Iterable<Entry<String, ChannelHandler>> {

    /**
     *
     * 总结一下:
     * ChannelPipeline 保存了与 Channel 相关联的 ChannelHandler;
     * ChannelPipeline 可以根据需要，通过添加或者删除 ChannelHandler 来动态地修改;
     * ChannelPipeline 有着丰富的 API 用以被调用，以响应入站和出站事件。
     *
     *
     * ============================================================================================================
     *
     *
     * 每一个新创建的 Channel 都将会被分配一个新的 ChannelPipeline。这项关联是永久性 的;
     * Channel 既不能附加另外一个 ChannelPipeline，也不能分离其当前的。
     * 在 Netty 组件 的生命周期中，这是一项固定的操作，不需要开发人员的任何干预。
     *
     * 如果你认为 ChannelPipeline 是一个拦截流经 Channel 的入站和出站事件的 ChannelHandler 实例链，
     * 那么就很容易看出这些 ChannelHandler 之间的交互是如何组成一个应用 程序数据和事件处理逻辑的核心的。
     *
     *
     * 根据事件的起源，事件将会被 ChannelInboundHandler 或者 ChannelOutboundHandler
     * 处理。随后，通过调用 ChannelHandlerContext 实现，它将被转发给同一超类型的下一个
     * ChannelHandler。
     *
     *
     * <------------- ChannelPipeline ------ 出站处理器 <----------- 出站处理器
     *                                                                  ^
     *                                                                  |
     *                                                                  |
     * --------------> 入站处理器 ------------> 入站处理器 ----------> 入站处理器
     *
     * ChannelPipeline 相对论
     * 你可能会说，从事件途经 ChannelPipeline 的角度来看，ChannelPipeline 的头部和尾端取
     * 决于该事件是入站的还是出站的。然而 Netty 总是将 ChannelPipeline 的入站口(图 6-3 中的左侧) 作为头部，而将出站口(该图的右侧)作为尾端。
     * 当你完成了通过调用 ChannelPipeline.add*()方法将入站处理器(ChannelInboundHandler) 和出站处理器(ChannelOutboundHandler)混合添加到 ChannelPipeline 之后，
     * 每一个 ChannelHandler 从头部到尾端的顺序位置正如同我们方才所定义它们的一样。因此，如果你将图 6-3 中 的处理器(ChannelHandler)从左到右进行编号，
     * 那么第一个被入站事件看到的 ChannelHandler 将是 1，而第一个被出站事件看到的 ChannelHandler 将是 5。
     *
     *
     * 在 ChannelPipeline 传播事件时，它会测试 ChannelPipeline 中的下一个 ChannelHandler 的类型是否和事件的运动方向相匹配。
     * 如果不匹配，ChannelPipeline 将跳过该 ChannelHandler 并前进到下一个，直到它找到和该事件所期望的方向相匹配的为止。
     * ChannelHandler 也可以同时实现 ChannelInboundHandler 接口和 ChannelOutboundHandler 接口。)
     */

    /**
     * Inserts a {@link ChannelHandler} at the first position of this pipeline.
     *
     * @param name the name of the handler to insert first
     * @param handler the handler to insert first
     * @throws IllegalArgumentException if there's an entry with the same name already in the pipeline
     * @throws NullPointerException if the specified handler is {@code null}
     */
    ChannelPipeline addFirst(String name, ChannelHandler handler);

    /**
     * ChannelHandler 的执行和阻塞
     * 通常 ChannelPipeline 中的每一个 ChannelHandler 都是通过它的 EventLoop(I/O 线程)来处
     * 理传递给它的事件的。所以至关重要的是不要阻塞这个线程，因为这会对整体的 I/O 处理产生负面的影响。
     * 但有时可能需要与那些使用阻塞 API 的遗留代码进行交互。对于这种情况，ChannelPipeline 有一些 接受一个 EventExecutorGroup 的 add()方法。
     * 如果一个事件被传递给一个自定义的 EventExecutorGroup，它将被包含在这个 EventExecutorGroup 中的某个 EventExecutor 所处理，
     * 从而被从该 Channel 本身的 EventLoop 中移除。对于这种用例，Netty 提供了一个叫 DefaultEventExecutor- Group 的默认实现。
     *
     *
     * Inserts a {@link ChannelHandler} at the first position of this pipeline.
     *
     * @param group the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     * methods( If your business logic is fully asynchronous or finished very quickly, you don't
     * need to specify a group.)
     * @param name the name of the handler to insert first
     * @param handler the handler to insert first
     * @throws IllegalArgumentException if there's an entry with the same name already in the pipeline
     * @throws NullPointerException if the specified handler is {@code null}
     */
    ChannelPipeline addFirst(EventExecutorGroup group, String name, ChannelHandler handler);

    /**
     * Appends a {@link ChannelHandler} at the last position of this pipeline.
     *
     * @param name the name of the handler to append
     * @param handler the handler to append
     * @throws IllegalArgumentException if there's an entry with the same name already in the pipeline
     * @throws NullPointerException if the specified handler is {@code null}
     */
    ChannelPipeline addLast(String name, ChannelHandler handler);

    /**
     * Appends a {@link ChannelHandler} at the last position of this pipeline.
     *
     * @param group the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     * methods，当该处理器逻辑比较复杂耗时的时候可以使用专门的线程池处理该业务
     * @param name the name of the handler to append
     * @param handler the handler to append
     * @throws IllegalArgumentException if there's an entry with the same name already in the pipeline
     * @throws NullPointerException if the specified handler is {@code null}
     */
    ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler);

    /**
     * Inserts a {@link ChannelHandler} before an existing handler of this
     * pipeline.
     *
     * @param baseName the name of the existing handler
     * @param name the name of the handler to insert before
     * @param handler the handler to insert before
     * @throws NoSuchElementException if there's no such entry with the specified {@code baseName}
     * @throws IllegalArgumentException if there's an entry with the same name already in the pipeline
     * @throws NullPointerException if the specified baseName or handler is {@code null}
     */
    ChannelPipeline addBefore(String baseName, String name, ChannelHandler handler);

    /**
     * Inserts a {@link ChannelHandler} before an existing handler of this
     * pipeline.
     *
     * @param group the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     * methods
     * @param baseName the name of the existing handler
     * @param name the name of the handler to insert before
     * @param handler the handler to insert before
     * @throws NoSuchElementException if there's no such entry with the specified {@code baseName}
     * @throws IllegalArgumentException if there's an entry with the same name already in the pipeline
     * @throws NullPointerException if the specified baseName or handler is {@code null}
     */
    ChannelPipeline addBefore(EventExecutorGroup group, String baseName, String name, ChannelHandler handler);

    /**
     * Inserts a {@link ChannelHandler} after an existing handler of this
     * pipeline.
     *
     * @param baseName the name of the existing handler
     * @param name the name of the handler to insert after
     * @param handler the handler to insert after
     * @throws NoSuchElementException if there's no such entry with the specified {@code baseName}
     * @throws IllegalArgumentException if there's an entry with the same name already in the pipeline
     * @throws NullPointerException if the specified baseName or handler is {@code null}
     */
    ChannelPipeline addAfter(String baseName, String name, ChannelHandler handler);

    /**
     * Inserts a {@link ChannelHandler} after an existing handler of this
     * pipeline.
     *
     * @param group the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     * methods
     * @param baseName the name of the existing handler
     * @param name the name of the handler to insert after
     * @param handler the handler to insert after
     * @throws NoSuchElementException if there's no such entry with the specified {@code baseName}
     * @throws IllegalArgumentException if there's an entry with the same name already in the pipeline
     * @throws NullPointerException if the specified baseName or handler is {@code null}
     */
    ChannelPipeline addAfter(EventExecutorGroup group, String baseName, String name, ChannelHandler handler);

    /**
     * Inserts {@link ChannelHandler}s at the first position of this pipeline.
     *
     * @param handlers the handlers to insert first
     */
    ChannelPipeline addFirst(ChannelHandler... handlers);

    /**
     * Inserts {@link ChannelHandler}s at the first position of this pipeline.
     *
     * @param group the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}s
     * methods.
     * @param handlers the handlers to insert first
     */
    ChannelPipeline addFirst(EventExecutorGroup group, ChannelHandler... handlers);

    /**
     * Inserts {@link ChannelHandler}s at the last position of this pipeline.
     *
     * @param handlers the handlers to insert last
     */
    ChannelPipeline addLast(ChannelHandler... handlers);

    /**
     * Inserts {@link ChannelHandler}s at the last position of this pipeline.
     *
     * @param group the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}s
     * methods.
     * @param handlers the handlers to insert last
     */
    ChannelPipeline addLast(EventExecutorGroup group, ChannelHandler... handlers);

    /**
     * Removes the specified {@link ChannelHandler} from this pipeline.
     *
     * @param handler the {@link ChannelHandler} to remove
     * @throws NoSuchElementException if there's no such handler in this pipeline
     * @throws NullPointerException if the specified handler is {@code null}
     */
    ChannelPipeline remove(ChannelHandler handler);

    /**
     * Removes the {@link ChannelHandler} with the specified name from this pipeline.
     *
     * @param name the name under which the {@link ChannelHandler} was stored.
     * @return the removed handler
     * @throws NoSuchElementException if there's no such handler with the specified name in this pipeline
     * @throws NullPointerException if the specified name is {@code null}
     */
    ChannelHandler remove(String name);

    /**
     * Removes the {@link ChannelHandler} of the specified type from this pipeline.
     *
     * @param <T> the type of the handler
     * @param handlerType the type of the handler
     * @return the removed handler
     * @throws NoSuchElementException if there's no such handler of the specified type in this pipeline
     * @throws NullPointerException if the specified handler type is {@code null}
     */
    <T extends ChannelHandler> T remove(Class<T> handlerType);

    /**
     * Removes the first {@link ChannelHandler} in this pipeline.
     *
     * @return the removed handler
     * @throws NoSuchElementException if this pipeline is empty
     */
    ChannelHandler removeFirst();

    /**
     * Removes the last {@link ChannelHandler} in this pipeline.
     *
     * @return the removed handler
     * @throws NoSuchElementException if this pipeline is empty
     */
    ChannelHandler removeLast();

    /**
     * Replaces the specified {@link ChannelHandler} with a new handler in this pipeline.
     *
     * @param oldHandler the {@link ChannelHandler} to be replaced
     * @param newName the name under which the replacement should be added
     * @param newHandler the {@link ChannelHandler} which is used as replacement
     * @return itself
     * @throws NoSuchElementException if the specified old handler does not exist in this pipeline
     * @throws IllegalArgumentException if a handler with the specified new name already exists in this
     * pipeline, except for the handler to be replaced
     * @throws NullPointerException if the specified old handler or new handler is
     * {@code null}
     */
    ChannelPipeline replace(ChannelHandler oldHandler, String newName, ChannelHandler newHandler);

    /**
     * Replaces the {@link ChannelHandler} of the specified name with a new handler in this pipeline.
     *
     * @param oldName the name of the {@link ChannelHandler} to be replaced
     * @param newName the name under which the replacement should be added
     * @param newHandler the {@link ChannelHandler} which is used as replacement
     * @return the removed handler
     * @throws NoSuchElementException if the handler with the specified old name does not exist in this pipeline
     * @throws IllegalArgumentException if a handler with the specified new name already exists in this
     * pipeline, except for the handler to be replaced
     * @throws NullPointerException if the specified old handler or new handler is
     * {@code null}
     */
    ChannelHandler replace(String oldName, String newName, ChannelHandler newHandler);

    /**
     * Replaces the {@link ChannelHandler} of the specified type with a new handler in this pipeline.
     *
     * @param oldHandlerType the type of the handler to be removed
     * @param newName the name under which the replacement should be added
     * @param newHandler the {@link ChannelHandler} which is used as replacement
     * @return the removed handler
     * @throws NoSuchElementException if the handler of the specified old handler type does not exist
     * in this pipeline
     * @throws IllegalArgumentException if a handler with the specified new name already exists in this
     * pipeline, except for the handler to be replaced
     * @throws NullPointerException if the specified old handler or new handler is
     * {@code null}
     */
    <T extends ChannelHandler> T replace(Class<T> oldHandlerType, String newName,
            ChannelHandler newHandler);

    /**
     * Returns the first {@link ChannelHandler} in this pipeline.
     *
     * @return the first handler.  {@code null} if this pipeline is empty.
     */
    ChannelHandler first();

    /**
     * 返回此管道中第一个 ChannelHandler 的上下文。
     *
     * 说明：每一个 ChannelHandler 都有自己的 ChannelHandlerContext
     *
     * <p></p>
     * Returns the context of the first {@link ChannelHandler} in this pipeline.
     *
     * @return the context of the first handler.  {@code null} if this pipeline is empty.
     */
    ChannelHandlerContext firstContext();

    /**
     * Returns the last {@link ChannelHandler} in this pipeline.
     *
     * @return the last handler.  {@code null} if this pipeline is empty.
     */
    ChannelHandler last();

    /**
     * Returns the context of the last {@link ChannelHandler} in this pipeline.
     *
     * @return the context of the last handler.  {@code null} if this pipeline is empty.
     */
    ChannelHandlerContext lastContext();

    /**
     * Returns the {@link ChannelHandler} with the specified name in this
     * pipeline.
     *
     * @return the handler with the specified name.
     * {@code null} if there's no such handler in this pipeline.
     */
    ChannelHandler get(String name);

    /**
     * Returns the {@link ChannelHandler} of the specified type in this
     * pipeline.
     *
     * @return the handler of the specified handler type.
     * {@code null} if there's no such handler in this pipeline.
     */
    <T extends ChannelHandler> T get(Class<T> handlerType);

    /**
     * Returns the context object of the specified {@link ChannelHandler} in
     * this pipeline.
     *
     * @return the context object of the specified handler.
     * {@code null} if there's no such handler in this pipeline.
     */
    ChannelHandlerContext context(ChannelHandler handler);

    /**
     * 返回和 ChannelHandler 绑定的 ChannelHandlerContext
     *
     * Returns the context object of the {@link ChannelHandler} with the
     * specified name in this pipeline.
     *
     * @return the context object of the handler with the specified name.
     * {@code null} if there's no such handler in this pipeline.
     */
    ChannelHandlerContext context(String name);

    /**
     * 返回和 ChannelHandler 绑定的 ChannelHandlerContext
     *
     * Returns the context object of the {@link ChannelHandler} of the
     * specified type in this pipeline.
     *
     * @return the context object of the handler of the specified type.
     * {@code null} if there's no such handler in this pipeline.
     */
    ChannelHandlerContext context(Class<? extends ChannelHandler> handlerType);

    /**
     * Returns the {@link Channel} that this pipeline is attached to.
     *
     * @return the channel. {@code null} if this pipeline is not attached yet.
     */
    Channel channel();

    /**
     * 返回 ChannelPipeline 中所有 ChannelHandler 的名称
     *
     * Returns the {@link List} of the handler names.
     */
    List<String> names();

    /**
     * Converts this pipeline into an ordered {@link Map} whose keys are
     * handler names and whose values are handlers.
     */
    Map<String, ChannelHandler> toMap();

    /**
     * 调用 ChannelPipeline 中下一个 ChannelInboundHandler 的 channelRegistered(ChannelHandlerContext)方法
     *
     * @return
     */
    @Override
    ChannelPipeline fireChannelRegistered();

    //调用 ChannelPipeline 中下一个 ChannelInboundHandler 的 channelUnregistered(ChannelHandlerContext)方法
    @Override
    ChannelPipeline fireChannelUnregistered();

    //调用 ChannelPipeline 中下一个 ChannelInboundHandler 的 channelActive(ChannelHandlerContext)方法
    @Override
    ChannelPipeline fireChannelActive();

    //调用 ChannelPipeline 中下一个 ChannelInboundHandler 的 channelInactive(ChannelHandlerContext)方法
    @Override
    ChannelPipeline fireChannelInactive();

    //调用 ChannelPipeline 中下一个 ChannelInboundHandler 的 exceptionCaught(ChannelHandlerContext, Throwable)方法
    @Override
    ChannelPipeline fireExceptionCaught(Throwable cause);

    //调用 ChannelPipeline 中下一个 ChannelInboundHandler 的 userEventTriggered(ChannelHandlerContext, Object)方法
    @Override
    ChannelPipeline fireUserEventTriggered(Object event);

    //调用 ChannelPipeline 中下一个 ChannelInboundHandler 的 channelRead(ChannelHandlerContext, Object msg)方法
    @Override
    ChannelPipeline fireChannelRead(Object msg);

    //调用 ChannelPipeline 中下一个 ChannelInboundHandler 的 channelReadComplete(ChannelHandlerContext)方法
    @Override
    ChannelPipeline fireChannelReadComplete();

    /**
     * 调用 ChannelPipeline 中下一个 ChannelInboundHandler 的 channelWritabilityChanged(ChannelHandlerContext)方法
     *
     * @return
     * @see ChannelOutboundBuffer#setUnwritable(boolean)
     * @see ChannelOutboundBuffer#decrementPendingOutboundBytes(long, boolean, boolean)
     */
    @Override
    ChannelPipeline fireChannelWritabilityChanged();

    //冲刷 Channel 所有挂起的写入。这将调用 ChannelPipeline 中的下一个 ChannelOutboundHandler 的 flush(ChannelHandlerContext)方法
    @Override
    ChannelPipeline flush();
}
