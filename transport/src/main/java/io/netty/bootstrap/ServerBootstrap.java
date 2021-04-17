package io.netty.bootstrap;

import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * {@link Bootstrap} sub-class which allows easy bootstrap of {@link ServerChannel}
 */
public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ServerBootstrap.class);

    private final Map<ChannelOption<?>, Object> childOptions = new LinkedHashMap<ChannelOption<?>, Object>();

    private final Map<AttributeKey<?>, Object> childAttrs = new LinkedHashMap<AttributeKey<?>, Object>();

    private final ServerBootstrapConfig config = new ServerBootstrapConfig(this);

    /**
     * serverBootstrap.group(parentGroup, childGroup) 的 childGroup
     *
     * 是基于当前Server产生的客户端Channel使用
     */
    private volatile EventLoopGroup childGroup;

    /**
     * serverBootstrap.childHandler(new MyServerInitializer());
     * 处理工作线程的处理器
     *
     * @see com.shengsiyuan.netty.secondexample.server.MyServerInitializer
     *
     *  配置的是当前Server上连接进来的的客户端的Channel的handler，其实也是保存配置的过程
     */
    private volatile ChannelHandler childHandler;

    public ServerBootstrap() {
    }

    private ServerBootstrap(ServerBootstrap bootstrap) {
        super(bootstrap);
        childGroup = bootstrap.childGroup;
        childHandler = bootstrap.childHandler;
        synchronized (bootstrap.childOptions) {
            childOptions.putAll(bootstrap.childOptions);
        }
        synchronized (bootstrap.childAttrs) {
            childAttrs.putAll(bootstrap.childAttrs);
        }
    }

    /**
     * Specify the {@link EventLoopGroup} which is used for the parent (acceptor) and the child (client).
     */
    @Override
    public ServerBootstrap group(EventLoopGroup group) {
        return group(group, group);
    }

    /**
     * Set the {@link EventLoopGroup} for the parent (acceptor) and the child (client). These
     * {@link EventLoopGroup}'s are used to handle all the events and IO for {@link ServerChannel} and
     * {@link Channel}'s.
     */
    public ServerBootstrap group(
            EventLoopGroup parentGroup, // boss 是砸门的 ServerChannel 使用
            EventLoopGroup childGroup   // 是基于当前Server产生的客户端Channel使用
    ) {
        super.group(parentGroup);
        if (childGroup == null) {
            throw new NullPointerException("childGroup");
        }
        if (this.childGroup != null) {
            throw new IllegalStateException("childGroup set already");
        }
        this.childGroup = childGroup;
        return this;
    }

    /**
     * Allow to specify a {@link ChannelOption} which is used for the {@link Channel} instances once they get created
     * (after the acceptor accepted the {@link Channel}). Use a value of {@code null} to remove a previous set
     * {@link ChannelOption}.
     */
    public <T> ServerBootstrap childOption(ChannelOption<T> childOption, T value) {
        if (childOption == null) {
            throw new NullPointerException("childOption");
        }
        if (value == null) {
            synchronized (childOptions) {
                childOptions.remove(childOption);
            }
        } else {
            synchronized (childOptions) {
                childOptions.put(childOption, value);
            }
        }
        return this;
    }

    /**
     * Set the specific {@link AttributeKey} with the given value on every child {@link Channel}. If the value is
     * {@code null} the {@link AttributeKey} is removed
     */
    public <T> ServerBootstrap childAttr(AttributeKey<T> childKey, T value) {
        if (childKey == null) {
            throw new NullPointerException("childKey");
        }
        if (value == null) {
            childAttrs.remove(childKey);
        } else {
            childAttrs.put(childKey, value);
        }
        return this;
    }

    /**
     * Set the {@link ChannelHandler} which is used to serve the request for the {@link Channel}'s.
     *
     * 配置的是当前Server上连接进来的的客户端的Channel的handler，其实也是保存配置的过程
     */
    public ServerBootstrap childHandler(ChannelHandler childHandler) {
        if (childHandler == null) {
            throw new NullPointerException("childHandler");
        }
        this.childHandler = childHandler;
        return this;
    }

    @Override
    void init(Channel channel) throws Exception {
        //用户传入的
        final Map<ChannelOption<?>, Object> options = options0();
        synchronized (options) {
            setChannelOptions(channel, options, logger);
        }
        //用户传入的
        final Map<AttributeKey<?>, Object> attrs = attrs0();
        synchronized (attrs) {
            //便利map: Set<Map.Entry<K, V>> entrySet();
            for (Entry<AttributeKey<?>, Object> e : attrs.entrySet()) {
                @SuppressWarnings("unchecked")
                AttributeKey<Object> key = (AttributeKey<Object>) e.getKey();
                //因为 AbstractChannel 实现了 AttributeMap 接口
                Attribute<Object> attribute = channel.attr(key);
                attribute.set(e.getValue());
            }
        }

        /**
         * 该 Channel 对应的 ChannelPipeline
         *
         * 实例化 Channel 的时候也实例化了该 ChannelPipeline，是一个 DefaultChannelPipeline 实例
         * @see AbstractChannel#AbstractChannel(io.netty.channel.Channel)
         * @see AbstractChannel#newChannelPipeline()
         */
        ChannelPipeline p = channel.pipeline();

        //serverBootstrap.group(parentGroup, childGroup) 的 childGroup
        final EventLoopGroup currentChildGroup = childGroup;
        //serverBootstrap.childHandler(new MyServerInitializer());
        final ChannelHandler currentChildHandler = childHandler;

        //用户指定的一些配置
        final Entry<ChannelOption<?>, Object>[] currentChildOptions;

        //用户传的一些值
        final Entry<AttributeKey<?>, Object>[] currentChildAttrs;
        synchronized (childOptions) {
            currentChildOptions = childOptions.entrySet().toArray(newOptionArray(childOptions.size()));
        }
        synchronized (childAttrs) {
            currentChildAttrs = childAttrs.entrySet().toArray(newAttrArray(childAttrs.size()));
        }

        //添加处理器到 pipeline
        p.addLast(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(final Channel ch) throws Exception {
                //每一个 Channel 都有一个 ChannelPipeline
                final ChannelPipeline pipeline = ch.pipeline();
                ChannelHandler handler = config.handler();//parent的处理器
                if (handler != null) {
                    //往pipeline中添加parent的处理器
                    pipeline.addLast(handler);
                }

                EventLoop eventLoop = ch.eventLoop();
                eventLoop.execute(new Runnable() {
                    @Override
                    public void run() {
                        ServerBootstrapAcceptor bootstrapAcceptor = new ServerBootstrapAcceptor(ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs);
                        pipeline.addLast(bootstrapAcceptor);
                    }
                });
            }
        });
    }

    @Override
    public ServerBootstrap validate() {
        super.validate();
        if (childHandler == null) {
            throw new IllegalStateException("childHandler not set");
        }
        if (childGroup == null) {
            logger.warn("childGroup is not set. Using parentGroup instead.");
            childGroup = config.group();
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private static Entry<AttributeKey<?>, Object>[] newAttrArray(int size) {
        return new Entry[size];
    }

    @SuppressWarnings("unchecked")
    private static Map.Entry<ChannelOption<?>, Object>[] newOptionArray(int size) {
        return new Map.Entry[size];
    }

    private static class ServerBootstrapAcceptor extends ChannelInboundHandlerAdapter {

        /**
         * 工作线程
         */
        private final EventLoopGroup childGroup;

        /**
         * 工作线程处理器：用户自定义处理器也在其中
         */
        private final ChannelHandler childHandler;

        /**
         * 用户配置
         */
        private final Entry<ChannelOption<?>, Object>[] childOptions;

        /**
         * 用户配置
         */
        private final Entry<AttributeKey<?>, Object>[] childAttrs;

        /**
         * 构造器中实例化：把 Channel 配置成自动读
         */
        private final Runnable enableAutoReadTask;

        ServerBootstrapAcceptor(final Channel channel, EventLoopGroup childGroup, ChannelHandler childHandler,
                Entry<ChannelOption<?>, Object>[] childOptions, Entry<AttributeKey<?>, Object>[] childAttrs) {

            this.childGroup = childGroup;
            this.childHandler = childHandler;
            this.childOptions = childOptions;
            this.childAttrs = childAttrs;

            // Task which is scheduled to re-enable auto-read.
            // It's important to create this Runnable before we try to submit it as otherwise the URLClassLoader may
            // not be able to load the class because of the file limit it already reached.
            //
            // See https://github.com/netty/netty/issues/1328
            enableAutoReadTask = new Runnable() {
                @Override
                public void run() {
                    //把 Channel 配置成自动读
                    channel.config().setAutoRead(true);
                }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            //TODO ? 为什么这里会是 Channel 呢？
            final Channel childChannel = (Channel) msg;

            ChannelPipeline pipeline = childChannel.pipeline();
            pipeline.addLast(childHandler);

            setChannelOptions(childChannel, childOptions, logger);

            for (Entry<AttributeKey<?>, Object> e : childAttrs) {
                childChannel.attr((AttributeKey<Object>) e.getKey()).set(e.getValue());
            }

            try {
                //把该 childChannel Channel 注册到工作线程，那么该子线程的所有io操作都交给了工作线程
                ChannelFuture channelFuture = childGroup.register(childChannel);
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        //参数中的 future 其实就是外面的 channelFuture
                        if (!future.isSuccess()) {
                            forceClose(childChannel, future.cause());
                        }
                    }
                });
            } catch (Throwable t) {
                forceClose(childChannel, t);
            }
        }

        private static void forceClose(Channel childChannel, Throwable t) {
            Channel.Unsafe unsafe = childChannel.unsafe();
            unsafe.closeForcibly();
            logger.warn("Failed to register an accepted channel: {}", childChannel, t);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final ChannelConfig config = ctx.channel().config();
            if (config.isAutoRead()) {
                // stop accept new connections for 1 second to allow the channel to recover
                // See https://github.com/netty/netty/issues/1328
                config.setAutoRead(false);
                ctx.channel().eventLoop().schedule(enableAutoReadTask, 1, TimeUnit.SECONDS);
            }
            // still let the exceptionCaught event flow through the pipeline to give the user
            // a chance to do something with it
            ctx.fireExceptionCaught(cause);
        }
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public ServerBootstrap clone() {
        return new ServerBootstrap(this);
    }

    /**
     * Return the configured {@link EventLoopGroup} which will be used for the child channels or {@code null}
     * if non is configured yet.
     *
     * @deprecated Use {@link #config()} instead.
     */
    @Deprecated
    public EventLoopGroup childGroup() {
        return childGroup;
    }

    final ChannelHandler childHandler() {
        return childHandler;
    }

    final Map<ChannelOption<?>, Object> childOptions() {
        return copiedMap(childOptions);
    }

    final Map<AttributeKey<?>, Object> childAttrs() {
        return copiedMap(childAttrs);
    }

    @Override
    public final ServerBootstrapConfig config() {
        return config;
    }
}
