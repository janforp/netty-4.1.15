package com.nettyinaction.codes;

/**
 * SimpleChannelInboundHandler 与 ChannelInboundHandler
 *
 * @author zhucj
 * @since 20210325
 */
public class _4_SimpleChannelInboundHandler_And_ChannelInboundHandler {

    /**
     * 你可能会想:为什么我们在客户端{@link _3_EchoClientHandler}使用的是 SimpleChannelInboundHandler，
     * 而不是在 {@link _1_EchoServerHandler} 中所使用的 ChannelInboundHandlerAdapter 呢?
     *
     * 这和两个因素的相互作用有 关:
     *
     * 业务逻辑如何处理消息以及 Netty 如何管理资源。
     *
     * 在客户端，当 channelRead0()方法完成时，你已经有了传入消息，并且已经处理完它了。当该方 法返回时，SimpleChannelInboundHandler 负责释放指向保存该消息的 ByteBuf 的内存引用。
     * 在 {@link _1_EchoServerHandler} 中，你仍然需要将传入消息回送给发送者，而 write()操作是异步的，直 到 channelRead()方法返回后可能仍然没有完成(如代码清单 2-1 所示)。
     * 为此，{@link _1_EchoServerHandler}  扩展了 ChannelInboundHandlerAdapter，其在这个时间点上不会释放消息。
     * 消息在 {@link _1_EchoServerHandler}  的 channelReadComplete()方法中，当 writeAndFlush()方 法被调用时被释放(见代码清单 2-1)。
     * 第 5 章和第 6 章将对消息的资源管理进行详细的介绍。
     */
}
