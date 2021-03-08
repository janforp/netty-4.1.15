package com.nettyinaction.codes;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

/**
 * Echo 客户端将会:
 * (1)连接到服务器;
 * (2)发送一个或者多个消息;
 * (3)对于每个消息，等待并接收从服务器发回的相同的消息;
 * (4)关闭连接。
 *
 * @author zhucj
 * @since 20210325
 */
public class _5_EchoClient {

    public static void main(String[] args) throws InterruptedException {
        new _5_EchoClient().start();
    }

    /**
     * 让我们回顾一下这一节中所介绍的要点:
     * 为初始化客户端，创建了一个 Bootstrap 实例;
     * 为进行事件处理分配了一个 NioEventLoopGroup 实例，其中事件处理包括创建新的 连接以及处理入站和出站数据;
     * 为服务器连接创建了一个 InetSocketAddress 实例;
     * 当连接被建立时，一个 _3_EchoClientHandler 实例会被安装到(该 Channel 的)ChannelPipeline 中;
     * 在一切都设置完成后，调用 Bootstrap.connect()方法连接到远程节点; 完成了客户端，你便可以着手构建并测试该系统了
     *
     * @throws InterruptedException
     */
    public void start() throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            // 创建 Bootstrap
            Bootstrap bootstrap = new Bootstrap();
            bootstrap
                    // 指定 EventLoopGroup 以 处理客户端事件;需要适 用于 NIO 的实现
                    .group(group)
                    //适用于 NIO 传输的 Channel 类型
                    //和之前一样，使用了 NIO 传输。注意，你可以在客户端和服务器上分别使用不同的传输。 例如，在服务器端使用 NIO 传输，而在客户端使用 OIO 传输
                    .channel(NioSocketChannel.class)
                    //设置服务器的InetSocketAddress
                    .remoteAddress(new InetSocketAddress("127.0.0.1", 8888))
                    //在创建 Channel 时 向 ChannelPipeline 中添加一个 Echo- ClientHandler 实例
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new _3_EchoClientHandler());
                        }
                    });
            ChannelFuture future = bootstrap
                    //连接到远程节点
                    .connect()
                    //阻塞等待直到连接完成
                    .sync();
            // 阻塞，直到 Channel 关闭
            future.channel().closeFuture().sync();
        } finally {
            //关闭线程池并且
            //释放所有的资源
            group.shutdownGracefully();
        }
    }
}
