package io.netty.channel;

import java.net.SocketAddress;

/**
 * {@link ChannelHandler} which will get notified for IO-outbound-operations.
 * 入站处理器
 *
 * 处理出站数据并且允许拦截所有的操作
 *
 * 出站操作和数据将由 ChannelOutboundHandler 处理。
 * 它的方法将被 Channel、Channel- Pipeline 以及 ChannelHandlerContext 调用。
 *
 * ChannelOutboundHandler 的一个强大的功能是可以按需推迟操作或者事件，
 * 这使得可 以通过一些复杂的方法来处理请求。
 * 例如，如果到远程节点的写入被暂停了，那么你可以推迟冲 刷操作并在稍后继续。
 *
 * ChannelPromise与ChannelFuture ：
 * ChannelOutboundHandler中的大部分方法都需要一个 ChannelPromise参数，
 * 以便在操作完成时得到通知。
 * ChannelPromise是ChannelFuture的一个 子类，
 * 其定义了一些可写的方法，如setSuccess()和setFailure()，从而使ChannelFuture不 可变。
 */
public interface ChannelOutboundHandler extends ChannelHandler {

    /**
     * Called once a bind operation is made.
     *
     * @param ctx the {@link ChannelHandlerContext} for which the bind operation is made
     * @param localAddress the {@link SocketAddress} to which it should bound
     * @param promise the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception thrown if an error occurs
     *
     * 当请求将 Channel 绑定到本地地址时被调用
     */
    void bind(
            ChannelHandlerContext ctx,
            SocketAddress localAddress,
            ChannelPromise promise
    ) throws Exception;

    /**
     * Called once a connect operation is made.
     *
     * @param ctx the {@link ChannelHandlerContext} for which the connect operation is made
     * @param remoteAddress the {@link SocketAddress} to which it should connect
     * @param localAddress the {@link SocketAddress} which is used as source on connect
     * @param promise the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception thrown if an error occurs
     *
     * 当请求将 Channel 连接到远程节点时被调用
     */
    void connect(
            ChannelHandlerContext ctx,
            SocketAddress remoteAddress,
            SocketAddress localAddress,
            ChannelPromise promise
    ) throws Exception;

    /**
     * Called once a disconnect operation is made.
     *
     * @param ctx the {@link ChannelHandlerContext} for which the disconnect operation is made
     * @param promise the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception thrown if an error occurs
     *
     * 当请求将 Channel 从远程节点断开时被调用
     */
    void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;

    /**
     * Called once a close operation is made.
     *
     * @param ctx the {@link ChannelHandlerContext} for which the close operation is made
     * @param promise the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception thrown if an error occurs
     *
     * 当请求关闭 Channel 时被调用
     */
    void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;

    /**
     * Called once a deregister operation is made from the current registered {@link EventLoop}.
     *
     * @param ctx the {@link ChannelHandlerContext} for which the close operation is made
     * @param promise the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception thrown if an error occurs
     *
     * 当请求将 Channel 从它的 EventLoop 注销 时被调用
     */
    void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;

    /**
     * Intercepts {@link ChannelHandlerContext#read()}.
     *
     * 当请求从 Channel 读取更多的数据时被调用
     *
     * 每当通过调用 ChannelInboundHandler.channelRead()或者 ChannelOutbound- Handler.write()方法来处理数据时，
     * 你都需要确保没有任何的资源泄漏。你可能还记得在前面的章节中所提到的，Netty 使用引用计数来处理池化的 ByteBuf。
     * 所以在完全使用完某个 ByteBuf 后，调整其引用计数是很重要的。
     * 为了帮助你诊断潜在的(资源泄漏)问题，Netty提供了class ResourceLeakDetector1， 它将对你应用程序的缓冲区分配做大约 1%的采样来检测内存泄露。相关的开销是非常小的。
     */
    void read(ChannelHandlerContext ctx) throws Exception;

    /**
     * Called once a write operation is made. The write operation will write the messages through the
     * {@link ChannelPipeline}. Those are then ready to be flushed to the actual {@link Channel} once
     * {@link Channel#flush()} is called
     *
     * @param ctx the {@link ChannelHandlerContext} for which the write operation is made
     * @param msg the message to write
     * @param promise the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception thrown if an error occurs
     *
     * 当请求通过 Channel 将数据写到远程节点时 被调用
     */
    void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception;

    /**
     * Called once a flush operation is made. The flush operation will try to flush out all previous written messages
     * that are pending.
     *
     * @param ctx the {@link ChannelHandlerContext} for which the flush operation is made
     * @throws Exception thrown if an error occurs
     *
     * 当请求通过 Channel 将入队数据冲刷到远程
     * 节点时被调用
     */
    void flush(ChannelHandlerContext ctx) throws Exception;
}