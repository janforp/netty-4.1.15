package io.netty.channel;

/**
 * {@link ChannelHandler} which adds callbacks for state changes.
 *
 * This allows the user to hook in to state changes easily.
 * -- 这使用户可以轻松加入状态更改。
 *
 * 处理入站数据以及各种状态变化
 */
public interface ChannelInboundHandler extends ChannelHandler {
    /**
     * 表 6-3 列出了 interface ChannelInboundHandler 的生命周期方法。
     * 这些方法将会在 数据被接收时 或者 与其对应的 Channel 状态发生改变时被调用。
     * 正如我们前面所提到的，这些 方法和 Channel 的生命周期密切相关。
     *
     * * @Sharable
     * * public class DiscardHandler extends ChannelInboundHandlerAdapter {
     * *     @Override
     * *     public void channelRead(ChannelHandlerContext ctx, Object msg) {
     *            //释放消息资源
     * *          ReferenceCountUtil.release(msg);
     * *     }
     * * }
     *
     * @see SimpleChannelInboundHandler
     */

    // ************************************************************************************************************

    /**
     * The {@link Channel} of the {@link ChannelHandlerContext} was registered with its {@link EventLoop}
     *
     * 当 Channel 已经注册到它的 EventLoop 并且能够处理 I/O 时被调用
     */
    void channelRegistered(ChannelHandlerContext ctx) throws Exception;

    /**
     * The {@link Channel} of the {@link ChannelHandlerContext} was unregistered from its {@link EventLoop}
     *
     * 当 Channel 从它的 EventLoop 注销并且无法处理任何 I/O 时被调用
     */
    void channelUnregistered(ChannelHandlerContext ctx) throws Exception;

    /**
     * The {@link Channel} of the {@link ChannelHandlerContext} is now active
     *
     * 当 Channel 处于活动状态时被调用;Channel 已经连接/绑定并且已经就绪
     */
    void channelActive(ChannelHandlerContext ctx) throws Exception;

    /**
     * The {@link Channel} of the {@link ChannelHandlerContext} was registered is now inactive and reached its
     * end of lifetime.
     *
     * 当 Channel 离开活动状态并且不再连接它的远程节点时被调用
     */
    void channelInactive(ChannelHandlerContext ctx) throws Exception;

    /**
     * 对于每个传入的消息都要调用
     *
     * Invoked when the current {@link Channel} has read a message from the peer.
     *
     * 当从 Channel 读取数据时被调用
     */
    void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception;

    /**
     * Invoked when the last message read by the current read operation has been consumed by
     * {@link #channelRead(ChannelHandlerContext, Object)}.  If {@link ChannelOption#AUTO_READ} is off, no further
     * attempt to read an inbound data from the current {@link Channel} will be made until
     * {@link ChannelHandlerContext#read()} is called.
     *
     * 当Channel上的一个读操作完成时被调用
     */
    void channelReadComplete(ChannelHandlerContext ctx) throws Exception;

    /**
     * Gets called if an user event was triggered.
     *
     * 当 ChannelInboundHandler.fireUserEventTriggered()方法被调 用时被调用，
     * 因为一个 POJO 被传经了 ChannelPipeline
     *
     * Triggered：已触发
     *
     * @param evt 一个 POJO 被传经了 ChannelPipeline
     */
    void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception;

    /**
     * Gets called once the writable state of a {@link Channel} changed. You can check the state with
     * {@link Channel#isWritable()}.
     *
     * 当Channel的可写状态发生改变时被调用。
     * 用户可以确保写操作不会完成 得太快(以避免发生 OutOfMemoryError)
     * 或者可以在 Channel 变为再
     * 次可写时恢复写入。
     *
     * 可以通过调用Channel的isWritable()方法来检测 Channel 的可写性。
     * 与可写性相关的阈值可以通过 Channel.config().setWriteHighWaterMark()
     * 和 Channel.config().setWriteLowWaterMark()方法来设置
     */
    void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception;

    /**
     * Gets called if a {@link Throwable} was thrown.
     */
    @Override
    @SuppressWarnings("deprecation")
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;
}
