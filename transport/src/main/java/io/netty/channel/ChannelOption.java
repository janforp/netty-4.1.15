package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.AbstractConstant;
import io.netty.util.ConstantPool;

import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * 该类型是常量 Constant 的一个具体实现
 *
 * <p></p>
 *
 * Constant <------ AbstractConstant <------- ChannelOption
 *
 * <p></p>
 * 该常量类中的很多方法都是通过 他其中的静态属性 pool 来完成的，如果：valueOf(),exist()等
 *
 *
 * <p></p>
 * A {@link ChannelOption} allows to configure a {@link ChannelConfig} in a type-safe
 * way. Which {@link ChannelOption} is supported depends on the actual implementation
 * of {@link ChannelConfig} and may depend on the nature of the transport it belongs
 * to.
 *
 * @param <T> the type of the value which is valid for the {@link ChannelOption}
 */
public class ChannelOption<T> extends AbstractConstant<ChannelOption<T>> {

    /**
     * 模版方法设计模式，所有的逻辑都由抽象基类 ConstantPool 实现，提供一个方法：newConstant 让用户创建具体的常量
     *
     * <p></p>
     *
     * 对创建的实例类型的唯一要求是：该实例是常量 Constant 的子类型（T extends Constant<T>）
     *
     * <p>
     * 该常量池中维护了一个 ConcurrentMap 用来缓存添加进去的常量
     * </p>
     */
    private static final ConstantPool<ChannelOption<Object>> pool = new ConstantPool<ChannelOption<Object>>() {
        @Override
        protected ChannelOption<Object> newConstant(int id, String name) {
            return new ChannelOption<Object>(id, name);
        }
    };

    /**
     * 常量跟常量池都可以直接创建常量
     * <p>
     * 只是常量类 ChannelOption 创建常量的方式是通过 常量池间接完成
     * </p>
     *
     * <p>代理？</p>
     * <p></p>
     * Returns the {@link ChannelOption} of the specified name.
     */
    @SuppressWarnings("unchecked")
    public static <T> ChannelOption<T> valueOf(String name) {
        return (ChannelOption<T>) pool.valueOf(name);
    }

    /**
     * Shortcut of {@link #valueOf(String) valueOf(firstNameComponent.getName() + "#" + secondNameComponent)}.
     */
    @SuppressWarnings("unchecked")
    public static <T> ChannelOption<T> valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        return (ChannelOption<T>) pool.valueOf(firstNameComponent, secondNameComponent);
    }

    /**
     * Returns {@code true} if a {@link ChannelOption} exists for the given {@code name}.
     */
    public static boolean exists(String name) {
        return pool.exists(name);
    }

    /**
     * Creates a new {@link ChannelOption} for the given {@code name} or fail with an
     * {@link IllegalArgumentException} if a {@link ChannelOption} for the given {@code name} exists.
     */
    @SuppressWarnings("unchecked")
    public static <T> ChannelOption<T> newInstance(String name) {
        return (ChannelOption<T>) pool.newInstance(name);
    }

    /**
     * Netty参数，ByteBuf的分配器，默认值为ByteBufAllocator.DEFAULT，4.0版本为UnpooledByteBufAllocator，4.1版本为PooledByteBufAllocator。
     * 该值也可以使用系统参数io.netty.allocator.type配置，使用字符串值："unpooled"，"pooled"。
     */
    public static final ChannelOption<ByteBufAllocator> ALLOCATOR = valueOf("ALLOCATOR");

    /**
     * Netty参数，用于Channel分配接受Buffer的分配器，默认值为 AdaptiveRecvByteBufAllocator.DEFAULT，
     * 是一个自适应的接受缓冲区分配器，能根据接受到的数据自动调节大小。
     * 可选值为FixedRecvByteBufAllocator，固定大小的接受缓冲区分配器。
     */
    public static final ChannelOption<RecvByteBufAllocator> RCVBUF_ALLOCATOR = valueOf("RCVBUF_ALLOCATOR");

    /**
     * Netty参数，消息大小估算器，默认为 DefaultMessageSizeEstimator.DEFAULT。
     * 估算ByteBuf、ByteBufHolder和FileRegion的大小，其中ByteBuf和ByteBufHolder为实际大小，FileRegion估算值为0。
     * 该值估算的字节数在计算水位时使用，FileRegion为0可知FileRegion不影响高低水位。
     */
    public static final ChannelOption<MessageSizeEstimator> MESSAGE_SIZE_ESTIMATOR = valueOf("MESSAGE_SIZE_ESTIMATOR");

    /**
     * 通用参数：
     * 连接超时毫秒数，默认值30000毫秒即30秒
     */
    public static final ChannelOption<Integer> CONNECT_TIMEOUT_MILLIS = valueOf("CONNECT_TIMEOUT_MILLIS");

    /**
     * 通用参数：
     *
     * Netty参数，一次Loop读取的最大消息数，对于ServerChannel或者NioByteChannel，默认值为16，其他Channel默认值为1。
     * 默认值这样设置，是因为：ServerChannel需要接受足够多的连接，保证大吞吐量，NioByteChannel可以减少不必要的系统调用select。
     * <p></p>
     *
     * @deprecated Use {@link MaxMessagesRecvByteBufAllocator}
     */
    @Deprecated
    public static final ChannelOption<Integer> MAX_MESSAGES_PER_READ = valueOf("MAX_MESSAGES_PER_READ");

    /**
     * Netty参数，一个Loop写操作执行的最大次数，默认值为16。
     * 也就是说，对于大数据量的写操作至多进行16次，如果16次仍没有全部写完数据，此时会提交一个新的写任务给EventLoop，任务将在下次调度继续执行。
     * 这样，其他的写请求才能被响应不会因为单个大数据量写请求而耽误。
     */
    public static final ChannelOption<Integer> WRITE_SPIN_COUNT = valueOf("WRITE_SPIN_COUNT");

    /**
     * Netty参数，写高水位标记，默认值64KB。如果Netty的写缓冲区中的字节超过该值，Channel的isWritable()返回False。
     * <p></p>
     *
     * @deprecated Use {@link #WRITE_BUFFER_WATER_MARK}
     */
    @Deprecated
    public static final ChannelOption<Integer> WRITE_BUFFER_HIGH_WATER_MARK = valueOf("WRITE_BUFFER_HIGH_WATER_MARK");

    /**
     * Netty参数，写低水位标记，默认值32KB。当Netty的写缓冲区中的字节超过高水位之后若下降到低水位，则Channel的isWritable()返回True。
     * 写高低水位标记使用户可以控制写入数据速度，从而实现流量控制。推荐做法是：每次调用channel.write(msg)方法首先调用channel.isWritable()判断是否可写。
     * <p></p>
     *
     * @deprecated Use {@link #WRITE_BUFFER_WATER_MARK}
     */
    @Deprecated
    public static final ChannelOption<Integer> WRITE_BUFFER_LOW_WATER_MARK = valueOf("WRITE_BUFFER_LOW_WATER_MARK");

    public static final ChannelOption<WriteBufferWaterMark> WRITE_BUFFER_WATER_MARK =
            valueOf("WRITE_BUFFER_WATER_MARK");

    /**
     * Netty参数，一个连接的远端关闭时本地端是否关闭，默认值为False。
     * 值为False时，连接自动关闭；
     * 为True时，触发ChannelInboundHandler的userEventTriggered()方法，事件为ChannelInputShutdownEvent。
     */
    public static final ChannelOption<Boolean> ALLOW_HALF_CLOSURE = valueOf("ALLOW_HALF_CLOSURE");

    /**
     * Netty参数，自动读取，默认值为True。
     * Netty只在必要的时候才设置关心相应的I/O事件。对于读操作，需要调用channel.read()设置关心的I/O事件为OP_READ，这样若有数据到达才能读取以供用户处理。
     * 该值为True时，每次读操作完毕后会自动调用channel.read()，从而有数据到达便能读取；否则，需要用户手动调用channel.read()。
     * 需要注意的是：当调用config.setAutoRead(boolean)方法时，如果状态由false变为true，将会调用channel.read()方法读取数据；由true变为false，将调用config.autoReadCleared()方法终止数据读取。
     */
    public static final ChannelOption<Boolean> AUTO_READ = valueOf("AUTO_READ");

    /**
     * @deprecated Auto close will be removed in a future release.
     *
     * If {@code true} then the {@link Channel} is closed automatically and immediately on write failure.
     * The default value is {@code true}.
     */
    @Deprecated
    public static final ChannelOption<Boolean> AUTO_CLOSE = valueOf("AUTO_CLOSE");

    /**
     * Socket参数，设置广播模式。
     */
    public static final ChannelOption<Boolean> SO_BROADCAST = valueOf("SO_BROADCAST");

    /**
     * Socket参数，连接保活，默认值为False。
     * 启用该功能时，TCP会主动探测空闲连接的有效性。可以将此功能视为TCP的心跳机制，需要注意的是：默认的心跳间隔是7200s即2小时。
     * Netty默认关闭该功能。
     */
    public static final ChannelOption<Boolean> SO_KEEPALIVE = valueOf("SO_KEEPALIVE");

    /**
     * Socket参数，TCP数据发送缓冲区大小。该缓冲区即TCP发送滑动窗口，linux操作系统可使用命令：cat /proc/sys/net/ipv4/tcp_smem查询其大小。
     *
     * 套接字使用的发送缓冲区大小
     */
    public static final ChannelOption<Integer> SO_SNDBUF = valueOf("SO_SNDBUF");

    /**
     * Socket参数，TCP数据接收缓冲区大小。
     * 该缓冲区即TCP接收滑动窗口，linux操作系统可使用命令：cat /proc/sys/net/ipv4/tcp_rmem查询其大小。
     * 一般情况下，该值可由用户在任意时刻设置，但当设置值超过64KB时，需要在连接到远端之前设置。
     *
     * 套接字使用的接收缓冲区大小
     */
    public static final ChannelOption<Integer> SO_RCVBUF = valueOf("SO_RCVBUF");

    /**
     * Socket参数，地址复用，默认值False。
     * 有四种情况可以使用：
     * (1).当有一个有相同本地地址和端口的socket1处于TIME_WAIT状态时，而你希望启动的程序的socket2要占用该地址和端口，比如重启服务且保持先前端口。
     * (2).有多块网卡或用IP Alias技术的机器在同一端口启动多个进程，但每个进程绑定的本地IP地址不能相同。
     * (3).单个进程绑定相同的端口到多个socket上，但每个socket绑定的ip地址不同。
     * (4).完全相同的地址和端口的重复绑定。但这只用于UDP的多播，不用于TCP。
     */
    public static final ChannelOption<Boolean> SO_REUSEADDR = valueOf("SO_REUSEADDR");

    /**
     * Socket参数，关闭Socket的延迟时间，默认值为-1，表示禁用该功能。
     * -1表示socket.close()方法立即返回，但OS底层会将发送缓冲区全部发送到对端。
     * 0表示socket.close()方法立即返回，OS放弃发送缓冲区的数据直接向对端发送RST包，对端收到复位错误。
     * 非0整数值表示调用socket.close()方法的线程被阻塞直到延迟时间到或发送缓冲区中的数据发送完毕，若超时，则对端会收到复位错误。
     */
    public static final ChannelOption<Integer> SO_LINGER = valueOf("SO_LINGER");

    /**
     * Socket参数，服务端接受连接的队列长度，如果队列已满，客户端连接将被拒绝。
     * 默认值，Windows为200，其他为128。
     */
    public static final ChannelOption<Integer> SO_BACKLOG = valueOf("SO_BACKLOG");

    /**
     * 控制读取操作将阻塞多少浩渺，如果设置为0，则无限阻塞
     */
    public static final ChannelOption<Integer> SO_TIMEOUT = valueOf("SO_TIMEOUT");

    /**
     * IP参数，设置IP头部的Type-of-Service字段，用于描述IP包的优先级和QoS选项。
     */
    public static final ChannelOption<Integer> IP_TOS = valueOf("IP_TOS");

    /**
     * 对应IP参数IP_MULTICAST_IF，设置对应地址的网卡为多播模式。
     */
    public static final ChannelOption<InetAddress> IP_MULTICAST_ADDR = valueOf("IP_MULTICAST_ADDR");

    /**
     * 对应IP参数IP_MULTICAST_IF2，同上但支持IPV6。
     */
    public static final ChannelOption<NetworkInterface> IP_MULTICAST_IF = valueOf("IP_MULTICAST_IF");

    /**
     * IP参数，多播数据报的time-to-live即存活跳数。
     */
    public static final ChannelOption<Integer> IP_MULTICAST_TTL = valueOf("IP_MULTICAST_TTL");

    /**
     * 对应IP参数IP_MULTICAST_LOOP，设置本地回环接口的多播功能。由于IP_MULTICAST_LOOP返回True表示关闭，所以Netty加上后缀_DISABLED防止歧义。
     */
    public static final ChannelOption<Boolean> IP_MULTICAST_LOOP_DISABLED = valueOf("IP_MULTICAST_LOOP_DISABLED");

    /**
     * TCP参数，立即发送数据，默认值为True（Netty默认为True而操作系统默认为False）。该值设置Nagle算法的启用，该算法将小的碎片数据连接成更大的报文来最小化所发送的报文的数量，如果需要发送一些较小的报文，则需要禁用该算法。
     * Netty默认禁用该算法，从而最小化报文传输延时。
     */
    public static final ChannelOption<Boolean> TCP_NODELAY = valueOf("TCP_NODELAY");

    /**
     * Netty参数，DatagramChannel注册的EventLoop即表示已激活。
     */
    @Deprecated
    public static final ChannelOption<Boolean> DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION =
            valueOf("DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION");

    /**
     * Netty参数，单线程执行ChannelPipeline中的事件，默认值为True。
     * 该值控制执行ChannelPipeline中执行ChannelHandler的线程。如果为True，整个pipeline由一个线程执行，这样不需要进行线程切换以及线程同步，是Netty4的推荐做法；
     * 如果为False，ChannelHandler中的处理过程会由Group中的不同线程执行。
     */
    public static final ChannelOption<Boolean> SINGLE_EVENTEXECUTOR_PER_GROUP = valueOf("SINGLE_EVENTEXECUTOR_PER_GROUP");

    /**
     * Creates a new {@link ChannelOption} with the specified unique {@code name}.
     */
    private ChannelOption(int id, String name) {
        super(id, name);
    }

    @Deprecated
    protected ChannelOption(String name) {
        this(pool.nextId(), name);
    }

    /**
     * Validate the value which is set for the {@link ChannelOption}. Sub-classes
     * may override this for special checks.
     */
    public void validate(T value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
    }
}
