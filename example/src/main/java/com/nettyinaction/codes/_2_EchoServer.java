package com.nettyinaction.codes;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * 与此同时，让我们回顾一下你刚完成的服务器实现中的重要步骤。下面这些是服务器的主要 代码组件:
 * EchoServerHandler 实现了业务逻辑;
 * main()方法引导了服务器;
 *
 * 引导过程中所需要的步骤如下:
 * 创建一个 ServerBootstrap 的实例以引导和绑定服务器;
 * 创建并分配一个 NioEventLoopGroup 实例以进行事件的处理，如接受新连接以及读/写数据;
 * 指定服务器绑定的本地的 InetSocketAddress;
 * 使用一个 EchoServerHandler 的实例初始化每一个新的 Channel;
 * 调用 ServerBootstrap.bind()方法以绑定服务器。
 *
 * @author zhucj
 * @since 20210325
 */
public class _2_EchoServer {

    public static void main(String[] args) throws Exception {
        new _2_EchoServer().start();
    }

    public void start() throws Exception {
        _1_EchoServerHandler serverHandler = new _1_EchoServerHandler();

        /**
         * 1.创建 Event- LoopGroup
         *
         * 这个示例使用了 NIO，因为得益于它的可扩展性和彻底的异步性，它是目前使用最广泛的传 输。
         * 但是也可以使用一个不同的传输实现。如果你想要在自己的服务器中使用 OIO 传输，将需 要指定 OioServerSocketChannel 和 OioEventLoopGroup
         * @see io.netty.channel.oio.OioEventLoopGroup
         */
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            // 2.创建 ServerBootstrap(服务器引导)
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(group)

                    /**
                     * 3.指定所使用的 NIO 传输 Channel
                     * 因为你正在使用的是 NIO 传输，所以你指定 了NioEventLoopGroup 来接受和处理新的连接，并且将Channel的类型指定为NioServerSocketChannel
                     * @see io.netty.channel.socket.oio.OioServerSocketChannel 这个示例使用了 NIO，因为得益于它的可扩展性和彻底的异步性，它是目前使用最广泛的传 输。
                     * 但是也可以使用一个不同的传输实现。如果你想要在自己的服务器中使用 OIO 传输，将需 要指定 OioServerSocketChannel 和 OioEventLoopGroup
                     */
                    .channel(NioServerSocketChannel.class)
                    // 4.使用指定的 端口设置套 接字地址
                    //在此之后，你将本地地址设置为一个具有选定端口的 InetSocketAddress 。服务器将绑定到这个地址以监听新的连接请求。
                    .localAddress(new InetSocketAddress(8888))
                    // 5. 添加一个 EchoServerHandler 到子Channel(childHandler) 的 ChannelPipeline
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        /**
                         * 你使用了一个特殊的类—— {@link ChannelInitializer}。这是关键。
                         * 当一个新的连接 被接受时，一个新的子 Channel 将会被创建，
                         * 而 ChannelInitializer 将会把一个你的 EchoServerHandler 的实例添加到该 Channel 的 {@link io.netty.channel.ChannelPipeline} 中。
                         * 正如我们之前所 解释的，这个 ChannelHandler 将会收到有关入站消息的通知
                         * @param ch the {@link io.netty.channel.Channel} which was registered.
                         */
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // EchoServerHandler 被 标注为@Shareable，所 以我们可以总是使用 同样的实例
                            ch.pipeline().addLast(serverHandler);
                        }
                    });

            //6.异步地绑定服务器; 调用 sync()方法阻塞 等待直到绑定完成
            ChannelFuture future = bootstrap
                    //接下来你绑定了服务器
                    .bind()
                    //并等待绑定完成
                    //对 sync()方法的调用将导致当前 Thread 阻塞，一直到绑定操作完成为止
                    .sync();

            //7.获取 Channel 的 CloseFuture，并 且阻塞当前线 程直到它完成
            //该应用程序将会阻塞等待直到服务器的 Channel 关闭(因为你在 Channel 的 CloseFuture 上调用了 sync()方法)
            future.channel()
                    .closeFuture()
                    .sync();
        } finally {
            //8. 关闭 EventLoopGroup， 释放所有的资源
            group.shutdownGracefully();
        }
    }
}
