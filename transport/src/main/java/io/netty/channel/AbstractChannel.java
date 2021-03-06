package io.netty.channel;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.nio.AbstractNioChannel;
import io.netty.channel.socket.ChannelOutputShutdownEvent;
import io.netty.channel.socket.ChannelOutputShutdownException;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.DefaultAttributeMap;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ThrowableUtil;
import io.netty.util.internal.UnstableApi;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * A skeletal {@link Channel} implementation.
 *
 * 骨架{@link Channel}实现。
 */
@SuppressWarnings("all")
public abstract class AbstractChannel extends DefaultAttributeMap implements Channel {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractChannel.class);

    private static final ClosedChannelException FLUSH0_CLOSED_CHANNEL_EXCEPTION = ThrowableUtil.unknownStackTrace(
            new ClosedChannelException(), AbstractUnsafe.class, "flush0()");

    /**
     * ensureOpen 的时候是不的时候塞入的异常
     */
    private static final ClosedChannelException ENSURE_OPEN_CLOSED_CHANNEL_EXCEPTION = ThrowableUtil.unknownStackTrace(
            new ClosedChannelException(), AbstractUnsafe.class, "ensureOpen(...)");

    private static final ClosedChannelException CLOSE_CLOSED_CHANNEL_EXCEPTION = ThrowableUtil.unknownStackTrace(
            new ClosedChannelException(), AbstractUnsafe.class, "close(...)");

    private static final ClosedChannelException WRITE_CLOSED_CHANNEL_EXCEPTION = ThrowableUtil.unknownStackTrace(
            new ClosedChannelException(), AbstractUnsafe.class, "write(...)");

    private static final NotYetConnectedException FLUSH0_NOT_YET_CONNECTED_EXCEPTION = ThrowableUtil.unknownStackTrace(
            new NotYetConnectedException(), AbstractUnsafe.class, "flush0()");

    private final Channel parent;

    /**
     * 每个channel实例都有一个id对象
     */
    private final ChannelId id;

    /**
     * @see AbstractUnsafe
     * 服务端的时候：NioServerSocketChannel，她的unsafe实例是谁？{@link io.netty.channel.nio.AbstractNioMessageChannel.NioMessageUnsafe}
     * 客户端的时候：NioSocketChannel，她的unsafe实例是谁？
     */
    private final Unsafe unsafe;

    // 创建出来当前channel内部的 pipeline 管道
    private final DefaultChannelPipeline pipeline;

    private final VoidChannelPromise unsafeVoidPromise = new VoidChannelPromise(this, false);

    /**
     * 其中的很多方法都是直接抛出异常
     */
    private final CloseFuture closeFuture = new CloseFuture(this);

    private volatile SocketAddress localAddress;

    private volatile SocketAddress remoteAddress;

    /**
     * 当前Channel注册的时候传进来，然后注册到该 EventLoop上
     *
     * @see AbstractUnsafe#register(io.netty.channel.EventLoop, io.netty.channel.ChannelPromise)
     */
    private volatile EventLoop eventLoop;

    /**
     * 该 Channel 是否已经注册
     *
     * @see AbstractUnsafe#register0(io.netty.channel.ChannelPromise)
     */
    private volatile boolean registered;

    /**
     *
     */
    private boolean closeInitiated;

    /**
     * Cache for the string representation of this channel
     *
     * 缓存此通道的字符串表示形式
     */
    private boolean strValActive;

    private String strVal;

    /**
     * Creates a new instance.
     *
     * @param parent the parent of this channel. {@code null} if there's no parent.
     */
    protected AbstractChannel(Channel parent) {
        this.parent = parent;
        // 每个channel实例都有一个id对象
        id = newId();

        /**
         * 服务端的时候：NioServerSocketChannel，她的unsafe实例是谁？{@link io.netty.channel.nio.AbstractNioMessageChannel.NioMessageUnsafe}
         * 客户端的时候：NioSocketChannel，她的unsafe实例是谁？
         */
        unsafe = newUnsafe();
        // 创建出来当前channel内部的 pipeline 管道, 创建出来的 pipeline 内部有2个默认的处理器，分别是 HeadContext 和 TailContext，如： head <--> tail
        pipeline = newChannelPipeline();
    }

    /**
     * Creates a new instance.
     *
     * @param parent the parent of this channel. {@code null} if there's no parent.
     */
    protected AbstractChannel(Channel parent, ChannelId id) {
        this.parent = parent;
        this.id = id;
        unsafe = newUnsafe();
        pipeline = newChannelPipeline();
    }

    @Override
    public final ChannelId id() {
        return id;
    }

    /**
     * Returns a new {@link DefaultChannelId} instance. Subclasses may override this method to assign custom
     * {@link ChannelId}s to {@link Channel}s that use the {@link AbstractChannel#AbstractChannel(Channel)} constructor.
     */
    protected ChannelId newId() {
        return DefaultChannelId.newInstance();
    }

    /**
     * Returns a new {@link DefaultChannelPipeline} instance.
     */
    protected DefaultChannelPipeline newChannelPipeline() {

        // 创建出来当前channel内部的 pipeline 管道, 创建出来的 pipeline 内部有2个默认的处理器，分别是 HeadContext 和 TailContext，如： head <--> tail
        return new DefaultChannelPipeline(this);
    }

    @Override
    public boolean isWritable() {
        ChannelOutboundBuffer buf = unsafe.outboundBuffer();
        return buf != null && buf.isWritable();
    }

    @Override
    public long bytesBeforeUnwritable() {
        ChannelOutboundBuffer buf = unsafe.outboundBuffer();
        // isWritable() is currently assuming if there is no outboundBuffer then the channel is not writable.
        // We should be consistent with that here.
        return buf != null ? buf.bytesBeforeUnwritable() : 0;
    }

    @Override
    public long bytesBeforeWritable() {
        ChannelOutboundBuffer buf = unsafe.outboundBuffer();
        // isWritable() is currently assuming if there is no outboundBuffer then the channel is not writable.
        // We should be consistent with that here.
        return buf != null ? buf.bytesBeforeWritable() : Long.MAX_VALUE;
    }

    @Override
    public Channel parent() {
        return parent;
    }

    @Override
    public ChannelPipeline pipeline() {
        return pipeline;
    }

    @Override
    public ByteBufAllocator alloc() {
        return config().getAllocator();
    }

    @Override
    public EventLoop eventLoop() {
        EventLoop eventLoop = this.eventLoop;
        if (eventLoop == null) {
            throw new IllegalStateException("channel not registered to an event loop");
        }
        return eventLoop;
    }

    @Override
    public SocketAddress localAddress() {
        SocketAddress localAddress = this.localAddress;
        if (localAddress == null) {
            try {
                this.localAddress = localAddress = unsafe().localAddress();
            } catch (Throwable t) {
                // Sometimes fails on a closed socket in Windows.
                return null;
            }
        }
        return localAddress;
    }

    /**
     * @deprecated no use-case for this.
     */
    @Deprecated
    protected void invalidateLocalAddress() {
        localAddress = null;
    }

    @Override
    public SocketAddress remoteAddress() {
        SocketAddress remoteAddress = this.remoteAddress;
        if (remoteAddress == null) {
            try {
                this.remoteAddress = remoteAddress = unsafe().remoteAddress();
            } catch (Throwable t) {
                // Sometimes fails on a closed socket in Windows.
                return null;
            }
        }
        return remoteAddress;
    }

    /**
     * @deprecated no use-case for this.
     */
    @Deprecated
    protected void invalidateRemoteAddress() {
        remoteAddress = null;
    }

    @Override
    public boolean isRegistered() {
        return registered;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return pipeline.bind(localAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return pipeline.connect(remoteAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        /**
         * 网络I/O操作直接调用 pipeline 的相关方法，由 pipeline 中的具体的 ChannelHandler 进行具体的逻辑处理
         */
        return pipeline.connect(remoteAddress, localAddress);
    }

    @Override
    public ChannelFuture disconnect() {
        return pipeline.disconnect();
    }

    @Override
    public ChannelFuture close() {
        return pipeline.close();
    }

    @Override
    public ChannelFuture deregister() {
        return pipeline.deregister();
    }

    @Override
    public Channel flush() {
        pipeline.flush();
        return this;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return pipeline.bind(localAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return pipeline.connect(remoteAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        return pipeline.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return pipeline.disconnect(promise);
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        return pipeline.close(promise);
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return pipeline.deregister(promise);
    }

    @Override
    public Channel read() {
        pipeline.read();
        return this;
    }

    @Override
    public ChannelFuture write(Object msg) {
        return pipeline.write(msg);
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return pipeline.write(msg, promise);
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return pipeline.writeAndFlush(msg);
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return pipeline.writeAndFlush(msg, promise);
    }

    @Override
    public ChannelPromise newPromise() {
        return pipeline.newPromise();
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
        return pipeline.newProgressivePromise();
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return pipeline.newSucceededFuture();
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return pipeline.newFailedFuture(cause);
    }

    @Override
    public ChannelFuture closeFuture() {
        return closeFuture;
    }

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    /**
     * Create a new {@link AbstractUnsafe} instance which will be used for the life-time of the {@link Channel}
     */
    protected abstract AbstractUnsafe newUnsafe();

    /**
     * Returns the ID of this channel.
     */
    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    /**
     * Returns {@code true} if and only if the specified object is identical
     * with this channel (i.e: {@code this == o}).
     */
    @Override
    public final boolean equals(Object o) {
        return this == o;
    }

    @Override
    public final int compareTo(Channel o) {
        if (this == o) {
            return 0;
        }

        return id().compareTo(o.id());
    }

    /**
     * Returns the {@link String} representation of this channel.  The returned
     * string contains the {@linkplain #hashCode() ID}, {@linkplain #localAddress() local address},
     * and {@linkplain #remoteAddress() remote address} of this channel for
     * easier identification.
     */
    @Override
    public String toString() {
        boolean active = isActive();
        if (strValActive == active && strVal != null) {
            return strVal;
        }

        SocketAddress remoteAddr = remoteAddress();
        SocketAddress localAddr = localAddress();
        if (remoteAddr != null) {
            StringBuilder buf = new StringBuilder(96)
                    .append("[id: 0x")
                    .append(id.asShortText())
                    .append(", L:")
                    .append(localAddr)
                    .append(active ? " - " : " ! ")
                    .append("R:")
                    .append(remoteAddr)
                    .append(']');
            strVal = buf.toString();
        } else if (localAddr != null) {
            StringBuilder buf = new StringBuilder(64)
                    .append("[id: 0x")
                    .append(id.asShortText())
                    .append(", L:")
                    .append(localAddr)
                    .append(']');
            strVal = buf.toString();
        } else {
            StringBuilder buf = new StringBuilder(16)
                    .append("[id: 0x")
                    .append(id.asShortText())
                    .append(']');
            strVal = buf.toString();
        }

        strValActive = active;
        return strVal;
    }

    @Override
    public final ChannelPromise voidPromise() {
        return pipeline.voidPromise();
    }

    /**
     * {@link Unsafe} implementation which sub-classes must extend and use.
     */
    protected abstract class AbstractUnsafe implements Unsafe {

        /**
         * 缓冲区
         */
        private volatile ChannelOutboundBuffer outboundBuffer = new ChannelOutboundBuffer(AbstractChannel.this);

        private RecvByteBufAllocator.Handle recvHandle;

        /**
         * 是否正在刷新
         */
        private boolean inFlush0;

        /**
         * 如果从未注册过，则为true，否则为false
         *
         *
         * true if the channel has never been registered, false otherwise
         *
         * @see AbstractUnsafe#register0(io.netty.channel.ChannelPromise)
         */
        private boolean neverRegistered = true;

        private void assertEventLoop() {
            //所在的channel未注册，并且在当前事件循环
            assert !registered || eventLoop.inEventLoop();
        }

        @Override
        public RecvByteBufAllocator.Handle recvBufAllocHandle() {
            if (recvHandle == null) {
                //如果是首次调用，则初始化
                recvHandle = config().getRecvByteBufAllocator().newHandle();
            }
            return recvHandle;
        }

        @Override
        public final ChannelOutboundBuffer outboundBuffer() {
            return outboundBuffer;
        }

        @Override
        public final SocketAddress localAddress() {
            return localAddress0();
        }

        @Override
        public final SocketAddress remoteAddress() {
            return remoteAddress0();
        }

        /**
         * 不可复写的方法
         *
         * @param eventLoop 事件循环，NioEventLoop 单线程线程池
         * @param promise 回调对象，结果封装，外部可以注册监听，进行异步操作
         */
        @Override
        public final void register(EventLoop eventLoop, final ChannelPromise promise) {
            if (eventLoop == null) {
                throw new NullPointerException("eventLoop");
            }

            // 防止channel重复注册
            if (isRegistered()) {
                //如果当前 Channel 已经注册过了，则无法重复注册
                /**
                 * 1.设置当前promise结果是失败
                 * 2.回调监听者执行失败的逻辑
                 */
                promise.setFailure(new IllegalStateException("registered to an event loop already"));
                return;
            }
            if (!isCompatible(eventLoop)) {
                //如果给定的EventLoop与此实例兼容，则返回true。如果不兼容返回false，是否兼容由具体的 Channel 实例决定
                promise.setFailure(new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName()));
                return;
            }

            //初始化该成员变量,
            // 获取到 Channel 作用域，这个 channel 就是unsafe的外层对象，如果是服务端，则channel就是 NioServersocketChannel对象,并且保存 eventLoop 到该 channel
            // 绑定关系，后续该 channel上的事件 或者 任务 都会依赖当前 eventLoop 线程去处理！！！！！
            AbstractChannel.this.eventLoop = eventLoop;

            /**
             * 封装的单独的线程
             *
             * 1.一个EventLoop（SingleThreadEventExecutor）在他的整个生命周期中都只会与唯一的一个Thread进行绑定
             * 2.所有由 EventLoop 所处理的各种I/O事件都将在它（该EventLoop）所关联的那个Thread上进行处理
             * 3.一个 Channel 在它的整个生命周期中只会注册在一个 EventLoop 上
             * 4.一个 EventLoop 在运行过程当中会被分配给一个或者多个 Channel（也就是说一个线程可以处理多个连接）更直白的说一个 EventLoop 需要处理一个或者多个 Channel,但是每一个 Channel 只属于唯一的一个 EventLoop
             * 5.所有同一个 Channel 的操作，他们的任务提交顺序跟任务的执行顺序是一样的，这一点netty是可以保证的（通过一个queue实现的FIFO）
             *
             *  ===》1：在netty当中，Channel 的实现一定是线程安全的；基于此，我们可以存储一个Channel的引用，并且在需要向远程端点发送数据时，通过这个引用来调用Channel相应的方法；
             *  即便当时有很多线程都在使用它也不会出现多线程问题，而且消息一定会按照顺序发送出去。
             *
             *  ===》2：我们在业务开发中，不要将长时间的耗时任务放入到 EventLoop 的执行队列中，因为它将会一直阻塞该线程所对应的Channel上的其他执行任务，
             *  如果我们需要进行阻塞调用或者耗时操作（实际开发中很常见），那么我们需要使用一个专门的 EventExecutor(业务线程池)。
             *
             *  业务线程池通常有2种实现方式"
             *  1.在 ChannelHandler 的回调方法中使用自定义的业务线程池，这样就可以实现异步调用。
             *  2.借助于netty提供的向 ChannelPipeline 添加 ChannelHandler 时调用的如 addLast 方法来传递 EventExecutor。
             *
             * @see SingleThreadEventExecutor#inEventLoop(java.lang.Thread) 判断当前线程是不是 当前 eventLoop 自己这个线程！！！！
             */
            if (eventLoop.inEventLoop()) {
                // 当前线程是不是 当前 eventLoop 自己这个线程！！！！
                // 真正注册的逻辑
                // 这样设计得到目的就是为了线程安全,因为咱们的 channel 支持 unregistor......？？？？TODO
                register0(promise);
            } else {
                try {
                    /**
                     * 将注册的任务提交到了 eventLoop 工作队列中了，带着 promise 过去的,注册的结果都通过带入的 promise 处理！！！
                     * 提交一个任务
                     *
                     * 并且在该方法中真正的创建线程
                     *
                     * @see SingleThreadEventExecutor#execute(java.lang.Runnable)
                     */
                    eventLoop.execute(new Runnable() {
                        /**
                         * 保证是但线程执行，避免多线程并发的问题
                         */
                        @Override
                        public void run() {
                            // 封装成一个任务！！！
                            register0(promise);
                        }
                    });
                } catch (Throwable t) {
                    logger.warn(
                            "Force-closing a channel whose registration task was not accepted by an event loop: {}",
                            AbstractChannel.this, t);
                    closeForcibly();
                    closeFuture.setClosed();
                    safeSetFailure(promise, t);
                }
            }
        }

        /**
         * 该方法由 当前 Channel 关联的 NioEventLoop 执行，跟主线程没关系
         * 经过之前的一些操作，使用该方法注册 Channel
         *
         * @param promise 其中包含了 Channel，表示注册结果的，外部可以向它注册监听器来完成注册后的逻辑
         */
        private void register0(ChannelPromise promise) {
            try {
                // check if the channel is still open as it could be closed in the mean time when the register
                // call was outside of the eventLoop
                if (!promise.setUncancellable() || !ensureOpen(promise)) {
                    return;
                }
                // 是否还没有注册过
                boolean firstRegistration = neverRegistered;

                /**
                 * 具体逻辑：selectionKey = nioSelectableChannel.register(nioSelector, 0, this);
                 *
                 * @see AbstractNioChannel#doRegister() 调用java.nio 注册
                 *
                 * jdk nio 原生的注册
                 */
                doRegister();
                // 表示已经注册过了
                neverRegistered = false;
                // 表示已经注册过了
                registered = true;

                // Ensure we call handlerAdded(...) before we actually notify the promise. This is needed as the
                // user may already fire events through the pipeline in the ChannelFutureListener.
                //回调 Channel 添加函数
                /**
                 * 其实就是把 程序员添加到 ChannelInitializer 中的 handler 添加到
                 * 该 channel 对于的 pipeline 中，然后把 ChannelInitializer 本身从 pipeline 中移出去
                 * @see DefaultChannelPipeline#callHandlerAddedForAllHandlers()
                 * @see DefaultChannelPipeline.PendingHandlerAddedTask#execute()
                 * @see DefaultChannelPipeline#callHandlerAdded0(io.netty.channel.AbstractChannelHandlerContext)
                 * @see ChannelInitializer#handlerAdded(io.netty.channel.ChannelHandlerContext) 调用CI 的 handlerAdded 方法
                 * @see ChannelInitializer#initChannel(io.netty.channel.ChannelHandlerContext) 在CI的该方法中调用下面的抽象方法
                 * @see ChannelInitializer#initChannel(io.netty.channel.Channel) 最终调用 CI 的抽象方法，这个方法就是程序员自己实现的方法，一般在这个方法中会向 pipeline 中添加自定义的 handler
                 * @see ChannelInitializer#initChannel(io.netty.channel.ChannelHandlerContext) 在上面的方法执行完成之后，此时 程序员自定义的 handler 已经添加到了 channel对应的pipeline中，
                 * 此时在 finally 代码块中会把 CI 本身从 pipeline 中移出去!!!!
                 */
                pipeline.invokeHandlerAddedIfNeeded();

                /**
                 * @param promise 其中包含了 Channel，表示注册结果的，外部可以向它注册监听器来完成注册后的逻辑
                 *
                 * 这一步会去回调 注册相关的 promise 上注册的哪些 listener，比如 主线程在 regFuture 上注册的 监听器
                 * @see AbstractBootstrap#doBind(java.net.SocketAddress) 这里添加了任务 {@link AbstractBootstrap#doBind0(io.netty.channel.ChannelFuture, io.netty.channel.Channel, java.net.SocketAddress, io.netty.channel.ChannelPromise)}
                 * @see AbstractBootstrap#doBind0(io.netty.channel.ChannelFuture, io.netty.channel.Channel, java.net.SocketAddress, io.netty.channel.ChannelPromise)
                 */
                safeSetSuccess(promise);

                /**
                 * 触发 ChannelHandler 的事件(Channel注册事件)传播
                 * 向当前 channel 的 pipeline 发起注册完成事件，关注该事件的 handler 可以做一些事情，比如打印日志
                 */
                pipeline.fireChannelRegistered();
                // Only fire a channelActive if the channel has never been registered. This prevents firing
                // multiple channel actives if the channel is deregistered and re-registered.
                //如果从未注册过频道，则仅触发channelActive。如果通道已注销并重新注册，这可以防止触发多个通道活动对象。

                /**
                 * @see NioServerSocketChannel#isActive() 这个方法其实就是判断是否绑定
                 * 那么咱们在这一步的时候完成绑定了吗？绑定操作一定是当前 eventLoop 线程去做的，当前 eventLoop 线程在干嘛呢？它还在执行register0也就是当前方法呢
                 * 所以，在这里的时候绑定操作一定是没有完成的，所以此时 isActive(0 == false
                 */
                if (isActive()) {
                    if (firstRegistration) {
                        pipeline.fireChannelActive();
                    } else if (config().isAutoRead()) {
                        // This channel was registered before and autoRead() is set. This means we need to begin read
                        // again so that we process inbound data.
                        //
                        // See https://github.com/netty/netty/issues/4805
                        //开始接收客户端的数据了
                        beginRead();
                    }
                }
            } catch (Throwable t) {
                // Close the channel directly to avoid FD leak.
                closeForcibly();
                closeFuture.setClosed();
                safeSetFailure(promise, t);
            }
        }

        @Override
        public final void bind(final SocketAddress localAddress, final ChannelPromise promise) {
            assertEventLoop();

            if (!promise.setUncancellable() || !ensureOpen(promise)) {
                return;
            }

            // See: https://github.com/netty/netty/issues/576
            if (Boolean.TRUE.equals(config().getOption(ChannelOption.SO_BROADCAST)) &&
                    localAddress instanceof InetSocketAddress &&
                    !((InetSocketAddress) localAddress).getAddress().isAnyLocalAddress() &&
                    !PlatformDependent.isWindows() && !PlatformDependent.maybeSuperUser()) {
                // Warn a user about the fact that a non-root user can't receive a
                // broadcast packet on *nix if the socket is bound on non-wildcard address.
                logger.warn(
                        "A non-root user can't receive a broadcast packet if the socket " +
                                "is not bound to a wildcard address; binding to a non-wildcard " +
                                "address (" + localAddress + ") anyway as requested.");
            }

            // 一般是 false
            boolean wasActive = isActive();
            try {
                /**
                 * 真正的使用 java.nio 绑定函数
                 *
                 * 模版方法
                 * 客户端跟服务端不同的绑定方法
                 */
                doBind(localAddress);
            } catch (Throwable t) {
                safeSetFailure(promise, t);
                closeIfClosed();
                return;
            }

            // 此时肯定完成了绑定
            if (!wasActive && isActive()) {

                // 向 eventLoop 提交任务4
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // 触发 Channel 激活事件传播
                        // headContext 会响应 active 事件，如何处理呢？
                        // 再次向当前Channel的pipeline发起read事件
                        // read 事件，就会修改 Channel 在 selector 上注册的感兴趣的事件为 accept

                        pipeline.fireChannelActive();
                    }
                });
            }

            // promise 表示的是绑定结果，谁在等待绑定结果呢？
            // 是咱们的启动线程，在该 promise 上执行的 wait 操作,所以,这一步绑定完成之后,会将其唤醒
            safeSetSuccess(promise);
        }

        @Override
        public final void disconnect(final ChannelPromise promise) {
            assertEventLoop();

            if (!promise.setUncancellable()) {
                return;
            }

            boolean wasActive = isActive();
            try {
                //java.nio.channels.spi.AbstractInterruptibleChannel.close
                doDisconnect();
            } catch (Throwable t) {
                safeSetFailure(promise, t);
                closeIfClosed();
                return;
            }

            if (wasActive && !isActive()) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        //失活事件传播
                        pipeline.fireChannelInactive();
                    }
                });
            }

            safeSetSuccess(promise);
            closeIfClosed(); // doDisconnect() might have closed the channel
        }

        @Override
        public final void close(final ChannelPromise promise) {
            assertEventLoop();

            close(promise, CLOSE_CLOSED_CHANNEL_EXCEPTION, CLOSE_CLOSED_CHANNEL_EXCEPTION, false);
        }

        @UnstableApi
        public void shutdownOutput() {
            final ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            if (outboundBuffer == null) {
                return;
            }
            this.outboundBuffer = null; // Disallow adding any messages and flushes to outboundBuffer.
            ChannelOutputShutdownException e = new ChannelOutputShutdownException("Channel output explicitly shutdown");
            outboundBuffer.failFlushed(e, false);
            outboundBuffer.close(e, true);
            pipeline.fireUserEventTriggered(ChannelOutputShutdownEvent.INSTANCE);
        }

        private void close(final ChannelPromise promise, final Throwable cause, final ClosedChannelException closeCause, final boolean notify) {

            if (!promise.setUncancellable()) {
                return;
            }

            if (closeInitiated) {
                if (closeFuture.isDone()) {
                    // Closed already.
                    safeSetSuccess(promise);
                } else if (!(promise instanceof VoidChannelPromise)) { // Only needed if no VoidChannelPromise.
                    // This means close() was called before so we just register a listener and return
                    closeFuture.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            promise.setSuccess();
                        }
                    });
                }
                return;
            }

            closeInitiated = true;

            final boolean wasActive = isActive();
            final ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            this.outboundBuffer = null; // Disallow adding any messages and flushes to outboundBuffer.
            Executor closeExecutor = prepareToClose();
            if (closeExecutor != null) {
                closeExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Execute the close.
                            doClose0(promise);
                        } finally {
                            // Call invokeLater so closeAndDeregister is executed in the EventLoop again!
                            invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    if (outboundBuffer != null) {
                                        // Fail all the queued messages
                                        outboundBuffer.failFlushed(cause, notify);
                                        outboundBuffer.close(closeCause);
                                    }
                                    fireChannelInactiveAndDeregister(wasActive);
                                }
                            });
                        }
                    }
                });
            } else {
                try {
                    // Close the channel and fail the queued messages in all cases.
                    doClose0(promise);
                } finally {
                    if (outboundBuffer != null) {
                        // Fail all the queued messages.
                        outboundBuffer.failFlushed(cause, notify);
                        outboundBuffer.close(closeCause);
                    }
                }
                if (inFlush0) {
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            fireChannelInactiveAndDeregister(wasActive);
                        }
                    });
                } else {
                    fireChannelInactiveAndDeregister(wasActive);
                }
            }
        }

        private void doClose0(ChannelPromise promise) {
            try {
                doClose();
                closeFuture.setClosed();
                safeSetSuccess(promise);
            } catch (Throwable t) {
                closeFuture.setClosed();
                safeSetFailure(promise, t);
            }
        }

        private void fireChannelInactiveAndDeregister(final boolean wasActive) {
            deregister(voidPromise(), wasActive && !isActive());
        }

        @Override
        public final void closeForcibly() {
            assertEventLoop();

            try {
                doClose();
            } catch (Exception e) {
                logger.warn("Failed to close a channel.", e);
            }
        }

        @Override
        public final void deregister(final ChannelPromise promise) {
            assertEventLoop();

            deregister(promise, false);
        }

        private void deregister(final ChannelPromise promise, final boolean fireChannelInactive) {
            if (!promise.setUncancellable()) {
                return;
            }

            if (!registered) {
                safeSetSuccess(promise);
                return;
            }

            // As a user may call deregister() from within any method while doing processing in the ChannelPipeline,
            // we need to ensure we do the actual deregister operation later. This is needed as for example,
            // we may be in the ByteToMessageDecoder.callDecode(...) method and so still try to do processing in
            // the old EventLoop while the user already registered the Channel to a new EventLoop. Without delay,
            // the deregister operation this could lead to have a handler invoked by different EventLoop and so
            // threads.
            //
            // See:
            // https://github.com/netty/netty/issues/4435
            invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        doDeregister();
                    } catch (Throwable t) {
                        logger.warn("Unexpected exception occurred while deregistering a channel.", t);
                    } finally {
                        if (fireChannelInactive) {
                            pipeline.fireChannelInactive();
                        }
                        // Some transports like local and AIO does not allow the deregistration of
                        // an open channel.  Their doDeregister() calls close(). Consequently,
                        // close() calls deregister() again - no need to fire channelUnregistered, so check
                        // if it was registered.
                        if (registered) {
                            registered = false;
                            pipeline.fireChannelUnregistered();
                        }
                        safeSetSuccess(promise);
                    }
                }
            });
        }

        @Override
        public final void beginRead() {
            assertEventLoop();

            if (!isActive()) {
                return;
            }

            try {
                doBeginRead();
            } catch (final Exception e) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.fireExceptionCaught(e);
                    }
                });
                close(voidPromise());
            }
        }

        @Override
        public final void write(Object msg, ChannelPromise promise) {
            assertEventLoop();

            ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            if (outboundBuffer == null) {
                // If the outboundBuffer is null we know the channel was closed and so
                // need to fail the future right away. If it is not null the handling of the rest
                // will be done in flush0()
                // See https://github.com/netty/netty/issues/2362
                safeSetFailure(promise, WRITE_CLOSED_CHANNEL_EXCEPTION);
                // release message now to prevent resource-leak
                ReferenceCountUtil.release(msg);
                return;
            }

            int size;
            try {

                //拿到消息
                msg = filterOutboundMessage(msg);
                size = pipeline.estimatorHandle().size(msg);
                if (size < 0) {
                    size = 0;
                }
            } catch (Throwable t) {
                safeSetFailure(promise, t);
                ReferenceCountUtil.release(msg);
                return;
            }

            outboundBuffer.addMessage(msg, size, promise);
        }

        @Override
        public final void flush() {
            assertEventLoop();

            ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            if (outboundBuffer == null) {
                return;
            }

            outboundBuffer.addFlush();
            flush0();
        }

        @SuppressWarnings("deprecation")
        protected void flush0() {
            if (inFlush0) {
                // Avoid re-entrance
                return;
            }

            final ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            if (outboundBuffer == null || outboundBuffer.isEmpty()) {
                return;
            }

            inFlush0 = true;

            // Mark all pending write requests as failure if the channel is inactive.
            if (!isActive()) {
                try {
                    if (isOpen()) {
                        outboundBuffer.failFlushed(FLUSH0_NOT_YET_CONNECTED_EXCEPTION, true);
                    } else {
                        // Do not trigger channelWritabilityChanged because the channel is closed already.
                        outboundBuffer.failFlushed(FLUSH0_CLOSED_CHANNEL_EXCEPTION, false);
                    }
                } finally {
                    inFlush0 = false;
                }
                return;
            }

            try {
                doWrite(outboundBuffer);
            } catch (Throwable t) {
                if (t instanceof IOException && config().isAutoClose()) {
                    /**
                     * Just call {@link #close(ChannelPromise, Throwable, boolean)} here which will take care of
                     * failing all flushed messages and also ensure the actual close of the underlying transport
                     * will happen before the promises are notified.
                     *
                     * This is needed as otherwise {@link #isActive()} , {@link #isOpen()} and {@link #isWritable()}
                     * may still return {@code true} even if the channel should be closed as result of the exception.
                     */
                    close(voidPromise(), t, FLUSH0_CLOSED_CHANNEL_EXCEPTION, false);
                } else {
                    outboundBuffer.failFlushed(t, true);
                }
            } finally {
                inFlush0 = false;
            }
        }

        @Override
        public final ChannelPromise voidPromise() {
            assertEventLoop();

            return unsafeVoidPromise;
        }

        protected final boolean ensureOpen(ChannelPromise promise) {
            if (isOpen()) {
                return true;
            }

            safeSetFailure(promise, ENSURE_OPEN_CLOSED_CHANNEL_EXCEPTION);
            return false;
        }

        /**
         * Marks the specified {@code promise} as success.  If the {@code promise} is done already, log a message.
         */
        protected final void safeSetSuccess(ChannelPromise promise) {
            if (!(promise instanceof VoidChannelPromise) && !promise.trySuccess()) {
                logger.warn("Failed to mark a promise as success because it is done already: {}", promise);
            }
        }

        /**
         * Marks the specified {@code promise} as failure.  If the {@code promise} is done already, log a message.
         */
        protected final void safeSetFailure(ChannelPromise promise, Throwable cause) {
            if (!(promise instanceof VoidChannelPromise) && !promise.tryFailure(cause)) {
                logger.warn("Failed to mark a promise as failure because it's done already: {}", promise, cause);
            }
        }

        protected final void closeIfClosed() {
            if (isOpen()) {
                return;
            }
            close(voidPromise());
        }

        private void invokeLater(Runnable task) {
            try {
                // This method is used by outbound operation implementations to trigger an inbound event later.
                // They do not trigger an inbound event immediately because an outbound operation might have been
                // triggered by another inbound event handler method.  If fired immediately, the call stack
                // will look like this for example:
                //
                //   handlerA.inboundBufferUpdated() - (1) an inbound handler method closes a connection.
                //   -> handlerA.ctx.close()
                //      -> channel.unsafe.close()
                //         -> handlerA.channelInactive() - (2) another inbound handler method called while in (1) yet
                //
                // which means the execution of two inbound handler methods of the same handler overlap undesirably.
                eventLoop().execute(task);
            } catch (RejectedExecutionException e) {
                logger.warn("Can't invoke task later as EventLoop rejected it", e);
            }
        }

        /**
         * Appends the remote address to the message of the exceptions caused by connection attempt failure.
         */
        protected final Throwable annotateConnectException(Throwable cause, SocketAddress remoteAddress) {
            if (cause instanceof ConnectException) {
                return new AnnotatedConnectException((ConnectException) cause, remoteAddress);
            }
            if (cause instanceof NoRouteToHostException) {
                return new AnnotatedNoRouteToHostException((NoRouteToHostException) cause, remoteAddress);
            }
            if (cause instanceof SocketException) {
                return new AnnotatedSocketException((SocketException) cause, remoteAddress);
            }

            return cause;
        }

        /**
         * Prepares to close the {@link Channel}. If this method returns an {@link Executor}, the
         * caller must call the {@link Executor#execute(Runnable)} method with a task that calls
         * {@link #doClose()} on the returned {@link Executor}. If this method returns {@code null},
         * {@link #doClose()} must be called from the caller thread. (i.e. {@link EventLoop})
         */
        protected Executor prepareToClose() {
            return null;
        }
    }

    //AbstractUnsafe end

    /**
     * 如果给定的EventLoop与此实例兼容，则返回true。
     *
     * @Override protected boolean isCompatible(EventLoop loop) {
     * return loop instanceof NioEventLoop;
     * }
     * @see AbstractNioChannel#isCompatible(io.netty.channel.EventLoop)
     *
     * <p></p>
     * Return {@code true} if the given {@link EventLoop} is compatible with this instance.
     */
    protected abstract boolean isCompatible(EventLoop loop);

    /**
     * Returns the {@link SocketAddress} which is bound locally.
     */
    protected abstract SocketAddress localAddress0();

    /**
     * Return the {@link SocketAddress} which the {@link Channel} is connected to.
     */
    protected abstract SocketAddress remoteAddress0();

    /**
     * Is called after the {@link Channel} is registered with its {@link EventLoop} as part of the register process.
     *
     * Sub-classes may override this method
     */
    protected void doRegister() throws Exception {
        // NOOP
    }

    /**
     * Bind the {@link Channel} to the {@link SocketAddress}
     */
    protected abstract void doBind(SocketAddress localAddress) throws Exception;

    /**
     * Disconnect this {@link Channel} from its remote peer
     */
    protected abstract void doDisconnect() throws Exception;

    /**
     * Close the {@link Channel}
     */
    protected abstract void doClose() throws Exception;

    /**
     * +
     * Deregister the {@link Channel} from its {@link EventLoop}.
     *
     * Sub-classes may override this method
     */
    protected void doDeregister() throws Exception {
        // NOOP
    }

    /**
     * Schedule a read operation.
     */
    protected abstract void doBeginRead() throws Exception;

    /**
     * Flush the content of the given buffer to the remote peer.
     */
    protected abstract void doWrite(ChannelOutboundBuffer in) throws Exception;

    /**
     * Invoked when a new message is added to a {@link ChannelOutboundBuffer} of this {@link AbstractChannel}, so that
     * the {@link Channel} implementation converts the message to another. (e.g. heap buffer -> direct buffer)
     */
    protected Object filterOutboundMessage(Object msg) throws Exception {
        return msg;
    }

    static final class CloseFuture extends DefaultChannelPromise {

        CloseFuture(AbstractChannel ch) {
            super(ch);
        }

        @Override
        public ChannelPromise setSuccess() {
            throw new IllegalStateException();
        }

        @Override
        public ChannelPromise setFailure(Throwable cause) {
            throw new IllegalStateException();
        }

        @Override
        public boolean trySuccess() {
            throw new IllegalStateException();
        }

        @Override
        public boolean tryFailure(Throwable cause) {
            throw new IllegalStateException();
        }

        boolean setClosed() {
            return super.trySuccess();
        }
    }

    private static final class AnnotatedConnectException extends ConnectException {

        private static final long serialVersionUID = 3901958112696433556L;

        AnnotatedConnectException(ConnectException exception, SocketAddress remoteAddress) {
            super(exception.getMessage() + ": " + remoteAddress);
            initCause(exception);
            setStackTrace(exception.getStackTrace());
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    private static final class AnnotatedNoRouteToHostException extends NoRouteToHostException {

        private static final long serialVersionUID = -6801433937592080623L;

        AnnotatedNoRouteToHostException(NoRouteToHostException exception, SocketAddress remoteAddress) {
            super(exception.getMessage() + ": " + remoteAddress);
            initCause(exception);
            setStackTrace(exception.getStackTrace());
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    private static final class AnnotatedSocketException extends SocketException {

        private static final long serialVersionUID = 3896743275010454039L;

        AnnotatedSocketException(SocketException exception, SocketAddress remoteAddress) {
            super(exception.getMessage() + ": " + remoteAddress);
            initCause(exception);
            setStackTrace(exception.getStackTrace());
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }
}
