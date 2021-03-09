package com.nettyinaction.codes;

/**
 * ChannelHandler 和 ChannelPipeline
 *
 * @author zhucj
 * @since 20210325
 */
public class _9_ChannelHandler_ChannelPipeline {

    /**
     * 3.2.1 {@link io.netty.channel.ChannelHandler} 接口
     *
     * 从应用程序开发人员的角度来看，Netty 的主要组件是 ChannelHandler，它充当了所有 处理入站和出站数据的应用程序逻辑的容器。
     * 这是可行的，因为 ChannelHandler 的方法是 由网络事件(其中术语“事件”的使用非常广泛)触发的。
     * 事实上，ChannelHandler 可专 门用于几乎任何类型的动作，例如将数据从一种格式转换为另外一种格式，或者处理转换过程 中所抛出的异常。
     * 举例来说，ChannelInboundHandler 是一个你将会经常实现的子接口。
     * 这种类型的 ChannelHandler 接收入站事件和数据，这些数据随后将会被你的应用程序的业务逻辑所处 理。
     * 当你要给连接的客户端发送响应时，也可以从 ChannelInboundHandler 冲刷数据。你 的应用程序的业务逻辑通常驻留在一个或者多个 ChannelInboundHandler 中。
     */

    /**
     * 3.2.2 {@link io.netty.channel.ChannelPipeline} 接口
     *
     * ChannelPipeline 提供了 ChannelHandler 链的容器，并定义了用于在该链上传播入站 和出站事件流的 API。
     * 当 Channel 被创建时，它会被自动地分配到它专属的 ChannelPipeline。
     * ChannelHandler 安装到 ChannelPipeline 中的过程如下所示:
     * 一个ChannelInitializer的实现被注册到了ServerBootstrap中 ;
     * 当 ChannelInitializer.initChannel()方法被调用时，ChannelInitializer将在 ChannelPipeline 中安装一组自定义的 ChannelHandler;
     * ChannelInitializer 将它自己从 ChannelPipeline 中移除。
     * 为了审查发送或者接收数据时将会发生什么，让我们来更加深入地研究 ChannelPipeline 和 ChannelHandler 之间的共生关系吧。
     * ChannelHandler 是专为支持广泛的用途而设计的，可以将它看作是处理往来 ChannelPipeline 事件(包括数据)的任何代码的通用容器。
     * 图 3-2 说明了这一点，其展示了从 ChannelHandler 派生的 ChannelInboundHandler 和 ChannelOutboundHandler 接口。
     */
}