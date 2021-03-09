package com.nettyinaction.codes;

/**
 * hannel、EventLoop 和 ChannelFuture
 *
 * @author zhucj
 * @since 20210325
 */
public class _6_Channel_EventLoop_ChannelFuture {
    /**
     * 接下来的各节将会为我们对于 {@link io.netty.channel.Channel }、{@link io.netty.channel.EventLoop} 和 {@link io.netty.channel.ChannelFuture} 类进行的讨论
     * 增添更多的细节，这些类合在一起，可以被认为是 Netty 网络抽象的代表:
     * Channel— Socket;
     * EventLoop — 控制流、多线程处理、并发;
     * ChannelFuture — 异步通知。
     */

    /**
     * 3.1.1 Channel 接口
     * 基本的 I/O 操作(bind()、connect()、read()和 write())依赖于底层网络传输所提 供的原语。
     * 在基于 Java 的网络编程中，其基本的构造是 类 Socket。
     * Netty 的 Channel 接 口所提供的 API，大大地降低了直接使用 Socket 类的复杂性。
     * 此外，Channel 也是拥有许多 预定义的、专门化实现的广泛类层次结构的根，下面是一个简短的部分清单:
     *
     * {@link io.netty.channel.embedded.EmbeddedChannel};
     * {@link io.netty.channel.local.LocalServerChannel};
     * {@link io.netty.channel.socket.nio.NioDatagramChannel};
     * {@link io.netty.channel.sctp.nio.NioSctpChannel};
     * {@link io.netty.channel.socket.nio.NioSocketChannel}。
     */

    /**
     * 3.1.2 EventLoop 接口
     *
     * 一个 EventLoopGroup 包含一个或者多个 EventLoop;
     * 一个 EventLoop 在它的生命周期内只和一个 Thread 绑定;
     * 所有由 EventLoop 处理的 I/O 事件都将在它专有的 Thread 上被处理;
     * 一个 Channel 在它的生命周期内只注册于一个 EventLoop;
     * 一个 EventLoop 可能会被分配给一个或多个 Channel。
     *
     * @see _8_EventLoop接口.png
     *
     * 注意，在这种设计中，一个给定 Channel 的 I/O 操作都是由相同的 Thread 执行的，实际上消除了对于同步的需要。
     */

    /**
     * 3.1.3 ChannelFuture 接口
     * 正如我们已经解释过的那样，Netty 中所有的 I/O 操作都是异步的。
     * 因为一个操作可能不会 立即返回，所以我们需要一种用于在之后的某个时间点确定其结果的方法。
     * 为此，Netty 提供了 ChannelFuture接口，其addListener()方法注册了一个ChannelFutureListener，以 便在某个操作完成时(无论是否成功)得到通知。
     *
     * 关于 ChannelFuture 的更多讨论 可以将 ChannelFuture 看作是将来要执行的操作的结果的 占位符。
     * 它究竟什么时候被执行则可能取决于若干的因素，因此不可能准确地预测，但是可以肯 定的是它将会被执行。
     * 此外，所有属于同一个 Channel 的操作都被保证其将以它们被调用的顺序 被执行
     */
}