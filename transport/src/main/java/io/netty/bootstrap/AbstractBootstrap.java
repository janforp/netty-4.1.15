package io.netty.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AbstractBootstrap是一个帮助程序类，可以轻松地引导Channel。
 *
 * 它支持方法链接，以提供一种简便的方法来配置AbstractBootstrap
 *
 * 如果未在ServerBootstrap上下文中使用，bind（）方法对于无连接传输（例如数据报（UDP））很有用。
 *
 * <p></p>
 * {@link AbstractBootstrap} is a helper class that makes it easy to bootstrap a {@link Channel}. It support
 * method-chaining to provide an easy way to configure the {@link AbstractBootstrap}.
 *
 * <p>When not used in a {@link ServerBootstrap} context, the {@link #bind()} methods are useful for connectionless
 * transports such as datagram (UDP).</p>
 */
public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> implements Cloneable {

    /**
     * 老板，父线程组
     * serverBootstrap.group(parentGroup, childGroup) 的 parentGroup
     */
    volatile EventLoopGroup group;

    /**
     * ReflectiveChannelFactory，传入的 Channel 类型为：NioServerSocketChannel/NioSocketChannel
     *
     * 就是为了实例化一个 Channel
     *
     * @see ReflectiveChannelFactory
     */
    @SuppressWarnings("deprecation")
    private volatile ChannelFactory<? extends C> channelFactory;

    private volatile SocketAddress localAddress;

    /**
     * serverBootstrap.option(ChannelOption.SO_RCVBUF, 123)
     * 实例化 BootStrap 的时候，用户指定的一些配置
     *
     * 设置TCP参数
     * 无论是异步NIO还是同步NIO，创建套接字的时候通常都会设置连接参数
     * 例如接收和发送缓冲区大小，连接超时时间等
     */
    private final Map<ChannelOption<?>, Object> options = new LinkedHashMap<ChannelOption<?>, Object>();

    /**
     * serverBootstrap.attr(AttributeKey.valueOf("login"), "123")
     * 实例化 BootStrap 的时候，用户指定的一些配置
     */
    private final Map<AttributeKey<?>, Object> attrs = new LinkedHashMap<AttributeKey<?>, Object>();

    /**
     * .handler(new LoggingHandler(LogLevel.WARN)) 用于处理 parentGroup 的处理器
     *
     * 配置用户自定义的 Server 端 pipeline 处理器,后续创建出来的 NioServerChannel 实例以后，会将用户自定义的handler加到该channel的pipeline中
     */
    private volatile ChannelHandler handler;

    AbstractBootstrap() {
        // Disallow extending from a different package.
    }

    AbstractBootstrap(AbstractBootstrap<B, C> bootstrap) {
        group = bootstrap.group;
        channelFactory = bootstrap.channelFactory;
        handler = bootstrap.handler;
        localAddress = bootstrap.localAddress;
        synchronized (bootstrap.options) {
            options.putAll(bootstrap.options);
        }
        synchronized (bootstrap.attrs) {
            attrs.putAll(bootstrap.attrs);
        }
    }

    /**
     * The {@link EventLoopGroup} which is used to handle all the events for the to-be-created
     * {@link Channel}
     */
    @SuppressWarnings("unchecked")
    public B group(EventLoopGroup group) {
        if (group == null) {
            throw new NullPointerException("group");
        }
        if (this.group != null) {
            throw new IllegalStateException("group set already");
        }
        this.group = group;
        return (B) this;
    }

    /**
     * The {@link Class} which is used to create {@link Channel} instances from.
     * You either use this or {@link #channelFactory(io.netty.channel.ChannelFactory)} if your
     * {@link Channel} implementation has no no-args constructor.
     *
     * 设置 Channel 类型
     */
    public B channel(Class<? extends C> channelClass) {
        if (channelClass == null) {
            throw new NullPointerException("channelClass");
        }
        return channelFactory(new ReflectiveChannelFactory<C>(channelClass));
    }

    /**
     * @deprecated Use {@link #channelFactory(io.netty.channel.ChannelFactory)} instead.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public B channelFactory(ChannelFactory<? extends C> channelFactory) {
        if (channelFactory == null) {
            throw new NullPointerException("channelFactory");
        }
        if (this.channelFactory != null) {
            throw new IllegalStateException("channelFactory set already");
        }

        this.channelFactory = channelFactory;
        return (B) this;
    }

    /**
     * {@link io.netty.channel.ChannelFactory} which is used to create {@link Channel} instances from
     * when calling {@link #bind()}. This method is usually only used if {@link #channel(Class)}
     * is not working for you because of some more complex needs. If your {@link Channel} implementation
     * has a no-args constructor, its highly recommend to just use {@link #channel(Class)} for
     * simplify your code.
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    public B channelFactory(io.netty.channel.ChannelFactory<? extends C> channelFactory) {
        return channelFactory((ChannelFactory<C>) channelFactory);
    }

    /**
     * The {@link SocketAddress} which is used to bind the local "end" to.
     */
    @SuppressWarnings("unchecked")
    public B localAddress(SocketAddress localAddress) {
        this.localAddress = localAddress;
        return (B) this;
    }

    /**
     * @see #localAddress(SocketAddress)
     */
    public B localAddress(int inetPort) {
        return localAddress(new InetSocketAddress(inetPort));
    }

    /**
     * @see #localAddress(SocketAddress)
     */
    public B localAddress(String inetHost, int inetPort) {
        return localAddress(SocketUtils.socketAddress(inetHost, inetPort));
    }

    /**
     * @see #localAddress(SocketAddress)
     */
    public B localAddress(InetAddress inetHost, int inetPort) {
        return localAddress(new InetSocketAddress(inetHost, inetPort));
    }

    /**
     * Allow to specify a {@link ChannelOption} which is used for the {@link Channel} instances once they got
     * created. Use a value of {@code null} to remove a previous set {@link ChannelOption}.
     *
     * 设置TCP参数
     * 无论是异步NIO还是同步NIO，创建套接字的时候通常都会设置连接参数
     * 例如接收和发送缓冲区大小，连接超时时间等
     *
     * 保存一些 Server 端自定义选项
     */
    @SuppressWarnings("unchecked")
    public <T> B option(ChannelOption<T> option, T value) {
        if (option == null) {
            throw new NullPointerException("option");
        }
        if (value == null) {
            synchronized (options) {
                options.remove(option);
            }
        } else {
            synchronized (options) {
                options.put(option, value);
            }
        }
        return (B) this;
    }

    /**
     * Allow to specify an initial attribute of the newly created {@link Channel}.  If the {@code value} is
     * {@code null}, the attribute of the specified {@code key} is removed.
     */
    @SuppressWarnings("unchecked")
    public <T> B attr(AttributeKey<T> key, T value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            synchronized (attrs) {
                attrs.remove(key);
            }
        } else {
            synchronized (attrs) {
                attrs.put(key, value);
            }
        }
        return (B) this;
    }

    /**
     * Validate all the parameters. Sub-classes may override this, but should
     * call the super method in that case.
     */
    @SuppressWarnings("unchecked")
    public B validate() {
        if (group == null) {
            throw new IllegalStateException("group not set");
        }
        if (channelFactory == null) {
            throw new IllegalStateException("channel or channelFactory not set");
        }
        return (B) this;
    }

    /**
     * Returns a deep clone of this bootstrap which has the identical configuration.  This method is useful when making
     * multiple {@link Channel}s with similar settings.  Please note that this method does not clone the
     * {@link EventLoopGroup} deeply but shallowly, making the group a shared resource.
     */
    @Override
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    public abstract B clone();

    /**
     * Create a new {@link Channel} and register it with an {@link EventLoop}.
     */
    public ChannelFuture register() {
        validate();
        return initAndRegister();
    }

    /**
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind() {
        validate();
        SocketAddress localAddress = this.localAddress;
        if (localAddress == null) {
            throw new IllegalStateException("localAddress not set");
        }
        return doBind(localAddress);
    }

    /**
     * Create a new {@link Channel} and bind it.
     *
     * @param inetPort 服务端要绑定的端口号
     */
    public ChannelFuture bind(int inetPort) {

        // 把端口封装了一下
        return bind(new InetSocketAddress(inetPort));
    }

    /**
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind(String inetHost, int inetPort) {
        return bind(SocketUtils.socketAddress(inetHost, inetPort));
    }

    /**
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind(InetAddress inetHost, int inetPort) {
        return bind(new InetSocketAddress(inetHost, inetPort));
    }

    /**
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind(SocketAddress localAddress) {
        validate();
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }
        return doBind(localAddress);
    }

    /**
     * 真正完成bind 工作的方法，非常关键
     *
     * @param localAddress 封装了端口
     * @return
     */
    private ChannelFuture doBind(final SocketAddress localAddress) {
        //初始化并注册，返回一个注册回调Future
        //其实这一步就是调用 java 原生的方法进行注册了，返回了promise对象，其实就是一个注册结果
        final ChannelFuture regFuture = initAndRegister();
        //拿到被注册的Channel
        final Channel channel = regFuture.channel();
        if (regFuture.cause() != null) {
            //发生了异常
            return regFuture;
        }

        /**
         * 如果此任务完成，则返回true。完成可能是由于正常终止，异常或取消引起的，在所有这些情况下，此方法都将返回true。
         */
        if (regFuture.isDone()) {
            //ChannelFuture 注册已经完成
            // 当 register0 已经被执行完后， regFuture 就是 done 状态

            // At this point we know that the registration was complete and successful.
            ChannelPromise promise = channel.newPromise();
            doBind0(regFuture, channel, localAddress, promise);
            return promise;
        } else {//ChannelFuture 注册还没有完成
            // Registration future is almost always fulfilled already, but just in case it's not.
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);

            // 给 register0 任务的promise对象添加了一个监听器，register0任务成功或者失败的事情，监听器回调线程就是 eventLoop 线程，并不是当前主线程
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    // 当 register0 任务执行完成之后，就会回调 operationComplete 该方法
                    Throwable cause = future.cause();
                    if (cause != null) {
                        // Registration on the EventLoop failed so fail the ChannelPromise directly to not cause an
                        // IllegalStateException once we try to access the EventLoop of the Channel.
                        promise.setFailure(cause);
                    } else {
                        // Registration was successful, so set the correct executor to use.
                        // See https://github.com/netty/netty/issues/2586
                        promise.registered();

                        // TODO？？？？
                        doBind0(regFuture, channel, localAddress, promise);
                    }
                }
            });
            // 主线程返回一个与 bind 操作相关的 promise 对象
            return promise;
        }
    }

    /**
     * 初始化一个 Channel，然后注册，真正的使用 java nio api 注册
     *
     * @return 返回注册完成后的通知回调
     */
    final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
            /**
             * 通过反射实例化一个 Channel(NioServerSocketChannel/NioSocketChannel)
             *
             * 调用 Channel 的无参数构造器实例化
             *
             * @see NioServerSocketChannel#NioServerSocketChannel()
             *
             * 会分别注册感兴趣的事件到操作系统上
             *
             * 会做一些列初始化的赋值，默认的也有
             *
             * 这个 channelFactory 就是创建 bootstrap 的时候穿进去的{bootstrap.channel(NioServerSocketChannel.class)}
             *
             * 调用的是 Channel 的无参数构造器,如果是服务端，这是{@link NioServerSocketChannel#NioServerSocketChannel()}
             *
             */
            channel = channelFactory.newChannel();
            //模版方法模式，该逻辑由具体的子类实现
            //向该 Channel 的 pipeline 添加了 用户指定的处理器，并且把parent线程的处理器添加到parent
            //把工作线程的处理器添加到工作线程

            //记住，最主要的：这一步会给当前服务端 Channel 的 Pipeline 添加一个 CI!!!!!!
            init(channel);
        } catch (Throwable t) {
            if (channel != null) {
                // channel can be null if newChannel crashed (eg SocketException("too many open files"))
                channel.unsafe().closeForcibly();
            }
            // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
            //由于 Channel 尚未  注册，因此我们需要强制使用GlobalEventExecutor，我们这里不能使用 Channel 自身的 EventLoop
            return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
        }

        //如果初始化成功，则执行后面的注册逻辑
        /**** 开始注册 ********/

        // new ServerBootstrapConfig(this);
        AbstractBootstrapConfig<B, C> config = config();

        /**
         * 返回 boss 线程组
         * config()返回的是什么呢？返回 boss 线程组
         * 其实就是 NioEventLoopGroup
         */
        EventLoopGroup eventLoopGroup = config.group();

        //真正的使用java.nio 进行注册
        /**
         * @see io.netty.channel.nio.NioEventLoopGroup#register(Channel)
         *
         * 返回一个注册的结果
         */
        ChannelFuture regFuture = eventLoopGroup.register(channel);

        //返回一个注册完成后的回调

        //如果注册的时候发生了异常
        if (regFuture.cause() != null) {
            if (channel.isRegistered()) {
                channel.close();
            } else {
                channel.unsafe().closeForcibly();
            }
        }

        // If we are here and the promise is not failed, it's one of the following cases:
        // 1) If we attempted registration from the event loop, the registration has been completed at this point.
        //    i.e. It's safe to attempt bind() or connect() now because the channel has been registered.
        // 2) If we attempted registration from the other thread, the registration request has been successfully
        //    added to the event loop's task queue for later execution.
        //    i.e. It's safe to attempt bind() or connect() now:
        //         because bind() or connect() will be executed *after* the scheduled registration task is executed
        //         because register(), bind(), and connect() are all bound to the same thread.

        return regFuture;
    }

    /**
     * NioServerSocketChannel/NioSocketChannel
     *
     * 模版方法模式
     *
     * @param channel
     * @throws Exception
     */
    abstract void init(Channel channel) throws Exception;

    /**
     * 这里只是在真正注册之后，触发注册绑定事件传播而已，真正的注册绑定已经执行了
     *
     * @param regFuture 注册的未来通知
     * @param channel 当前被注册的通道
     * @param localAddress 地址
     * @param promise channel.newPromise()返回的实例
     */
    private static void doBind0(final ChannelFuture regFuture, final Channel channel,
            final SocketAddress localAddress, final ChannelPromise promise) {

        //在触发channelRegistered（）之前调用此方法。使用户处理程序有机会在其channelRegistered（）实现中设置管道。
        // This method is invoked before channelRegistered() is triggered.  Give user handlers a chance to set up
        // the pipeline in its channelRegistered() implementation.
        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                if (regFuture.isSuccess()) {
                    //触发pipeline的绑定事件传播，执行相应handler中的逻辑而已
                    ChannelFuture channelFuture = channel.bind(localAddress, promise);
                    channelFuture.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                } else {
                    promise.setFailure(regFuture.cause());
                }
            }
        });
    }

    /**
     * the {@link ChannelHandler} to use for serving the requests.
     *
     * 配置用户自定义的 Server 端 pipeline 处理器,后续创建出来的 NioServerChannel 实例以后，会将用户自定义的handler加到该channel的pipeline中
     */
    @SuppressWarnings("unchecked")
    public B handler(ChannelHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        this.handler = handler;
        return (B) this;
    }

    /**
     * Returns the configured {@link EventLoopGroup} or {@code null} if non is configured yet.
     *
     * @deprecated Use {@link #config()} instead.
     */
    @Deprecated
    public final EventLoopGroup group() {
        return group;
    }

    /**
     * Returns the {@link AbstractBootstrapConfig} object that can be used to obtain the current config
     * of the bootstrap.
     */
    public abstract AbstractBootstrapConfig<B, C> config();

    static <K, V> Map<K, V> copiedMap(Map<K, V> map) {
        final Map<K, V> copied;
        synchronized (map) {
            if (map.isEmpty()) {
                return Collections.emptyMap();
            }
            copied = new LinkedHashMap<K, V>(map);
        }
        return Collections.unmodifiableMap(copied);
    }

    final Map<ChannelOption<?>, Object> options0() {
        return options;
    }

    final Map<AttributeKey<?>, Object> attrs0() {
        return attrs;
    }

    final SocketAddress localAddress() {
        return localAddress;
    }

    @SuppressWarnings("deprecation")
    final ChannelFactory<? extends C> channelFactory() {
        return channelFactory;
    }

    final ChannelHandler handler() {
        return handler;
    }

    final Map<ChannelOption<?>, Object> options() {
        return copiedMap(options);
    }

    final Map<AttributeKey<?>, Object> attrs() {
        return copiedMap(attrs);
    }

    /**
     * 为channel设置配置
     *
     * @param channel 当前 Channel
     * @param options 用户传入的
     * @param logger 日志
     */
    static void setChannelOptions(Channel channel, Map<ChannelOption<?>, Object> options, InternalLogger logger) {
        //便利 Map
        for (Map.Entry<ChannelOption<?>, Object> e : options.entrySet()) {
            setChannelOption(channel, e.getKey(), e.getValue(), logger);
        }
    }

    static void setChannelOptions(
            Channel channel, Map.Entry<ChannelOption<?>, Object>[] options, InternalLogger logger) {
        for (Map.Entry<ChannelOption<?>, Object> e : options) {
            setChannelOption(channel, e.getKey(), e.getValue(), logger);
        }
    }

    /**
     * 为channel设置配置
     *
     * @param channel 当前 Channel
     * @param option 用户传入的
     * @param value 配置对应的值
     * @param logger 日志
     * @see ChannelOption#MESSAGE_SIZE_ESTIMATOR
     */
    @SuppressWarnings("unchecked")
    private static void setChannelOption(Channel channel, ChannelOption<?> option, Object value, InternalLogger logger) {
        try {
            if (!channel.config().setOption((ChannelOption<Object>) option, value)) {
                logger.warn("Unknown channel option '{}' for channel '{}'", option, channel);
            }
        } catch (Throwable t) {
            logger.warn(
                    "Failed to set channel option '{}' with value '{}' for channel '{}'", option, value, channel, t);
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder()
                .append(StringUtil.simpleClassName(this))
                .append('(').append(config()).append(')');
        return buf.toString();
    }

    /**
     * 待注册
     *
     * At the moment AbstractBoostrap.bind(...) will always use the GlobalEventExecutor to
     * notify the returned ChannelFuture if the registration is not done yet.
     *
     * This should only be done if the registration fails later.
     * If it completes successful we should just notify with the EventLoop of the Channel.
     */
    static final class PendingRegistrationPromise extends DefaultChannelPromise {

        //注册成功后，将其设置为正确的EventExecutor。否则它将保持为null，因此GlobalEventExecutor.INSTANCE将用于通知。
        // Is set to the correct EventExecutor once the registration was successful. Otherwise it will
        // stay null and so the GlobalEventExecutor.INSTANCE will be used for notifications.
        private volatile boolean registered;

        PendingRegistrationPromise(Channel channel) {
            super(channel);
        }

        void registered() {
            registered = true;
        }

        @Override
        protected EventExecutor executor() {
            if (registered) {
                // If the registration was a success executor is set.
                //
                // See https://github.com/netty/netty/issues/2586
                return super.executor();
            }
            // The registration failed so we can only use the GlobalEventExecutor as last resort to notify.
            return GlobalEventExecutor.INSTANCE;
        }
    }
}
