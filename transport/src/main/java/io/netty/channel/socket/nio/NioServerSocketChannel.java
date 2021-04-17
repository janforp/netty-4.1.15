package io.netty.channel.socket.nio;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.nio.AbstractNioChannel;
import io.netty.channel.nio.AbstractNioMessageChannel;
import io.netty.channel.socket.DefaultServerSocketChannelConfig;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;

/**
 * A {@link io.netty.channel.socket.ServerSocketChannel} implementation which uses
 * NIO selector based implementation to accept new connections.
 */
public class NioServerSocketChannel extends AbstractNioMessageChannel
        implements io.netty.channel.socket.ServerSocketChannel {

    private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);

    /**
     * 全局唯一哦
     */
    private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioServerSocketChannel.class);

    /**
     * 获取java原生的服务端的：ServerSocketChannel
     *
     * 通过该方法在每次建立连接的时候创建 ServerSocketChannel ！！！
     *
     * @param provider 系统的提供器
     * @return
     * @see ServerSocketChannel#open() 如果使用java.nio写，则使用该方法
     * @see AbstractNioChannel#ch 每次生成java原生的socket之后都会保存到该字段
     */
    private static ServerSocketChannel newSocket(SelectorProvider provider) {
        try {
            /**
             *  Use the {@link SelectorProvider} to open {@link SocketChannel} and so remove condition in
             *  {@link SelectorProvider#provider()} which is called by each ServerSocketChannel.open() otherwise.
             *
             *  See <a href="https://github.com/netty/netty/issues/2308">#2308</a>.
             */
            return provider.openServerSocketChannel();
        } catch (IOException e) {
            throw new ChannelException(
                    "Failed to open a server socket.", e);
        }
    }

    /**
     * config = new NioServerSocketChannelConfig(this, serverSocket);
     *
     * 用于配置 TCP 连接的 参数！！！！！
     *
     * @see NioServerSocketChannel#NioServerSocketChannel(java.nio.channels.ServerSocketChannel)
     * @see NioServerSocketChannelConfig#NioServerSocketChannelConfig(io.netty.channel.socket.nio.NioServerSocketChannel, java.net.ServerSocket)
     */
    private final ServerSocketChannelConfig config;

    /**
     * Create a new instance
     */
    public NioServerSocketChannel() {
        //ServerSocketChannel serverSocketChannel = newSocket(DEFAULT_SELECTOR_PROVIDER);
        //传入java原生的服务端的：ServerSocketChannel
        this(newSocket(DEFAULT_SELECTOR_PROVIDER));
    }

    /**
     * Create a new instance using the given {@link SelectorProvider}.
     */
    public NioServerSocketChannel(SelectorProvider provider) {
        this(newSocket(provider));
    }

    /**
     * Create a new instance using the given {@link ServerSocketChannel}.
     */
    public NioServerSocketChannel(ServerSocketChannel channel) {
        //注册感兴趣的事件
        super(null, channel, SelectionKey.OP_ACCEPT);
        //java nio
        ServerSocketChannel serverSocketChannel = javaChannel();
        //java nio
        ServerSocket serverSocket = serverSocketChannel.socket();
        //这是一个私有内部类
        config = new NioServerSocketChannelConfig(this, serverSocket);
    }

    @Override
    public InetSocketAddress localAddress() {
        return (InetSocketAddress) super.localAddress();
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public ServerSocketChannelConfig config() {
        return config;
    }

    @Override
    public boolean isActive() {

        //拿到java原生的 socket
        ServerSocket serverSocket = javaChannel().socket();
        boolean bound = serverSocket.isBound();
        /**
         * 如果还在绑定中就表示还是激活状态
         */
        return bound;
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return null;
    }

    @Override
    protected ServerSocketChannel javaChannel() {
        return (ServerSocketChannel) super.javaChannel();
    }

    @Override
    protected SocketAddress localAddress0() {
        return SocketUtils.localSocketAddress(javaChannel().socket());
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        /**
         * localAddress:本地发起的可以指定端口也可以不指定端口
         */

        if (PlatformDependent.javaVersion() >= 7) {
            //如果是1.7则使用新的api绑定
            javaChannel().bind(localAddress, config.getBacklog());
        } else {
            //否则就通过socket的api进行绑定
            javaChannel().socket().bind(localAddress, config.getBacklog());
        }
    }

    @Override
    protected void doClose() throws Exception {
        javaChannel().close();
    }

    /**
     * 对于NioServerSocketChannel，他的读取操作就是接收客户端的连接，创建 NioSocketChannel 对象！！！！！
     *
     * @param buf
     * @return
     * @throws Exception
     */
    @Override
    protected int doReadMessages(List<Object> buf) throws Exception {
        /**
         * 将消息读取到给定的数组中，并返回已读取的数量。
         *
         * serverSocketChannel.accept() 其实就是调用原生的 accept() 进行连接的建立
         *
         * 接收新的客户端连接
         */
        SocketChannel ch = SocketUtils.accept(javaChannel());

        try {
            if (ch != null) {

                //TODO 这是干嘛？
                buf.add(new NioSocketChannel(this, ch));
                return 1;
            }
        } catch (Throwable t) {
            logger.warn("Failed to create a new channel from an accepted socket.", t);

            try {
                ch.close();
            } catch (Throwable t2) {
                logger.warn("Failed to close a socket.", t2);
            }
        }

        return 0;
    }

    // Unnecessary stuff ： 不必要的东西 因为 Channel 是公共的，下面的这些接口 NioSocketChannel 实现就可以了
    @Override
    protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        //只有客户端才需要实现该方法
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doFinishConnect() throws Exception {

        //只有客户端才需要实现该方法
        throw new UnsupportedOperationException();
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return null;
    }

    @Override
    protected void doDisconnect() throws Exception {

        //只有客户端才需要实现该方法

        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean doWriteMessage(Object msg, ChannelOutboundBuffer in) throws Exception {

        //只有客户端才需要实现该方法

        throw new UnsupportedOperationException();
    }

    @Override
    protected final Object filterOutboundMessage(Object msg) throws Exception {
        throw new UnsupportedOperationException();
    }

    private final class NioServerSocketChannelConfig extends DefaultServerSocketChannelConfig {

        private NioServerSocketChannelConfig(NioServerSocketChannel channel, ServerSocket javaSocket) {
            super(channel, javaSocket);
        }

        @Override
        protected void autoReadCleared() {
            clearReadPending();
        }
    }
}
