package io.netty.channel.socket.nio;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.util.internal.SocketUtils;
import io.netty.channel.nio.AbstractNioMessageChannel;
import io.netty.channel.socket.DefaultServerSocketChannelConfig;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.util.internal.PlatformDependent;
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
     * @param provider 系统的提供器
     * @return
     * @see ServerSocketChannel#open() 如果使用java.nio写，则使用该方法
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
     * @see NioServerSocketChannel#NioServerSocketChannel(java.nio.channels.ServerSocketChannel)
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
        ServerSocket serverSocket = javaChannel().socket();
        boolean bound = serverSocket.isBound();
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
        if (PlatformDependent.javaVersion() >= 7) {
            javaChannel().bind(localAddress, config.getBacklog());
        } else {
            javaChannel().socket().bind(localAddress, config.getBacklog());
        }
    }

    @Override
    protected void doClose() throws Exception {
        javaChannel().close();
    }

    @Override
    protected int doReadMessages(List<Object> buf) throws Exception {
        SocketChannel ch = SocketUtils.accept(javaChannel());

        try {
            if (ch != null) {
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

    // Unnecessary stuff
    @Override
    protected boolean doConnect(
            SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doFinishConnect() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return null;
    }

    @Override
    protected void doDisconnect() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean doWriteMessage(Object msg, ChannelOutboundBuffer in) throws Exception {
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
