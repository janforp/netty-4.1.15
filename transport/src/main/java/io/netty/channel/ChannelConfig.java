package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.socket.SocketChannelConfig;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Map;

/**
 * TCP/IP Channel 的一些配置
 *
 * 建议使用更具体的子类是配置
 * 也可以直接使用 setOption(XXX)配置，这样就可以不用向下转型为具体类啦
 * <p></p>
 * A set of configuration properties of a {@link Channel}.
 * <p>
 * Please down-cast to more specific configuration type such as
 * {@link SocketChannelConfig} or use {@link #setOptions(Map)} to set the
 * transport-specific properties:
 * <pre>
 * {@link Channel} ch = ...;
 * {@link SocketChannelConfig} cfg = <strong>({@link SocketChannelConfig}) ch.getConfig();</strong>
 * cfg.setTcpNoDelay(false);
 * </pre>
 *
 * <h3>Option map</h3>
 *
 * An option map property is a dynamic write-only property which allows
 * the configuration of a {@link Channel} without down-casting its associated
 * {@link ChannelConfig}.  To update an option map, please call {@link #setOptions(Map)}.
 * <p>
 * All {@link ChannelConfig} has the following options:
 *
 * <table border="1" cellspacing="0" cellpadding="6">
 * <tr>
 * <th>Name</th><th>Associated setter method</th>
 * </tr><tr>
 * <td>{@link ChannelOption#CONNECT_TIMEOUT_MILLIS}</td><td>{@link #setConnectTimeoutMillis(int)}</td>
 * </tr><tr>
 * <td>{@link ChannelOption#WRITE_SPIN_COUNT}</td><td>{@link #setWriteSpinCount(int)}</td>
 * </tr><tr>
 * <td>{@link ChannelOption#WRITE_BUFFER_WATER_MARK}</td><td>{@link #setWriteBufferWaterMark(WriteBufferWaterMark)}</td>
 * </tr><tr>
 * <td>{@link ChannelOption#ALLOCATOR}</td><td>{@link #setAllocator(ByteBufAllocator)}</td>
 * </tr><tr>
 * <td>{@link ChannelOption#AUTO_READ}</td><td>{@link #setAutoRead(boolean)}</td>
 * </tr>
 * </table>
 * <p>
 * More options are available in the sub-types of {@link ChannelConfig}.  For
 * example, you can configure the parameters which are specific to a TCP/IP
 * socket as explained in {@link SocketChannelConfig}.
 */
public interface ChannelConfig {

    /**
     * Return all set {@link ChannelOption}'s.
     */
    Map<ChannelOption<?>, Object> getOptions();

    /**
     * Sets the configuration properties from the specified {@link Map}.
     */
    boolean setOptions(Map<ChannelOption<?>, ?> options);

    /**
     * Return the value of the given {@link ChannelOption}
     */
    <T> T getOption(ChannelOption<T> option);

    /**
     * Sets a configuration property with the specified name and value.
     * To override this method properly, you must call the super class:
     * <pre>
     * public boolean setOption(ChannelOption&lt;T&gt; option, T value) {
     *     if (super.setOption(option, value)) {
     *         return true;
     *     }
     *
     *     if (option.equals(additionalOption)) {
     *         ....
     *         return true;
     *     }
     *
     *     return false;
     * }
     * </pre>
     *
     * @return {@code true} if and only if the property has been set
     */
    <T> boolean setOption(ChannelOption<T> option, T value);

    /**
     * Returns the connect timeout of the channel in milliseconds.  If the
     * {@link Channel} does not support connect operation, this property is not
     * used at all, and therefore will be ignored.
     *
     * @return the connect timeout in milliseconds.  {@code 0} if disabled.
     */
    int getConnectTimeoutMillis();

    /**
     * Sets the connect timeout of the channel in milliseconds.  If the
     * {@link Channel} does not support connect operation, this property is not
     * used at all, and therefore will be ignored.
     *
     * @param connectTimeoutMillis the connect timeout in milliseconds.
     * {@code 0} to disable.
     */
    ChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);

    /**
     * @deprecated Use {@link MaxMessagesRecvByteBufAllocator}
     * <p>
     * Returns the maximum number of messages to read per read loop.
     * a {@link ChannelInboundHandler#channelRead(ChannelHandlerContext, Object) channelRead()} event.
     * If this value is greater than 1, an event loop might attempt to read multiple times to procure multiple messages.
     */
    @Deprecated
    int getMaxMessagesPerRead();

    /**
     * @deprecated Use {@link MaxMessagesRecvByteBufAllocator}
     * <p>
     * Sets the maximum number of messages to read per read loop.
     * If this value is greater than 1, an event loop might attempt to read multiple times to procure multiple messages.
     */
    @Deprecated
    ChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead);

    /**
     * 返回写操作的最大循环计数，直到WritableByteChannel.write（ByteBuffer）返回非零值为止
     * <p>
     * 它类似于并发编程中使用的自旋锁
     * </p>
     * <p>
     * 根据JVM运行的平台，它可以提高内存利用率和写入吞吐量。预设值为16。
     * </p>
     * Returns the maximum loop count for a write operation until
     * {@link WritableByteChannel#write(ByteBuffer)} returns a non-zero value.
     * It is similar to what a spin lock is used for in concurrency programming.
     * It improves memory utilization and write throughput depending on
     * the platform that JVM runs on.  The default value is {@code 16}.
     */
    int getWriteSpinCount();

    /**
     * Sets the maximum loop count for a write operation until
     * {@link WritableByteChannel#write(ByteBuffer)} returns a non-zero value.
     * It is similar to what a spin lock is used for in concurrency programming.
     * It improves memory utilization and write throughput depending on
     * the platform that JVM runs on.  The default value is {@code 16}.
     *
     * @throws IllegalArgumentException if the specified value is {@code 0} or less than {@code 0}
     */
    ChannelConfig setWriteSpinCount(int writeSpinCount);

    /**
     * Returns {@link ByteBufAllocator} which is used for the channel
     * to allocate buffers.
     */
    ByteBufAllocator getAllocator();

    /**
     * Set the {@link ByteBufAllocator} which is used for the channel
     * to allocate buffers.
     */
    ChannelConfig setAllocator(ByteBufAllocator allocator);

    /**
     * <p>返回RecvByteBufAllocator，用于给 channel 分配接收缓冲区。</p>
     * Returns {@link RecvByteBufAllocator} which is used for the channel to allocate receive buffers.
     */
    <T extends RecvByteBufAllocator> T getRecvByteBufAllocator();

    /**
     * Set the {@link RecvByteBufAllocator} which is used for the channel to allocate receive buffers.
     */
    ChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator);

    /**
     * <p>
     * 当且仅当ChannelHandlerContext.read（）会被自动调用，用户应用程序根本不需要主动调用它的时时候，才返回true。默认值是true。
     * </p>
     * Returns {@code true} if and only if {@link ChannelHandlerContext#read()} will be invoked automatically so that
     * a user application doesn't need to call it at all. The default value is {@code true}.
     */
    boolean isAutoRead();

    /**
     * Sets if {@link ChannelHandlerContext#read()} will be invoked automatically so that a user application doesn't
     * need to call it at all. The default value is {@code true}.
     */
    ChannelConfig setAutoRead(boolean autoRead);

    /**
     * @deprecated Auto close will be removed in a future release.
     *
     * Returns {@code true} if and only if the {@link Channel} will be closed automatically and immediately on
     * write failure.  The default is {@code false}.
     */
    @Deprecated
    boolean isAutoClose();

    /**
     * @deprecated Auto close will be removed in a future release.
     *
     * Sets whether the {@link Channel} should be closed automatically and immediately on write failure.
     * The default is {@code false}.
     */
    @Deprecated
    ChannelConfig setAutoClose(boolean autoClose);

    /**
     * <p>名字直译：获取写缓冲区高水位线</p>
     * <p>返回写缓冲区的高水位线(也就是高位报警)。如果写入缓冲区中排队的字节数超过此值，则Channel.isWritable（）将开始返回false。</p>
     * <p>当写入缓冲区的字节数达到报警值的时候，就会变为不可写</p>
     *
     * Returns the high water mark of the write buffer.  If the number of bytes
     * queued in the write buffer exceeds this value, {@link Channel#isWritable()}
     * will start to return {@code false}.
     */
    int getWriteBufferHighWaterMark();

    /**
     * <p>
     * Sets the high water mark of the write buffer.  If the number of bytes
     * queued in the write buffer exceeds this value, {@link Channel#isWritable()}
     * will start to return {@code false}.
     */
    ChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark);

    /**
     * <p>名称直译：获取写缓冲区低水位线</p>
     * <p></p>
     * <p>返回写缓冲区的低水位线(也就是低液位报警)。一旦在写缓冲区中排队的字节数超过了高水位线，然后降至该值以下，Channel.isWritable（）将再次开始返回true。</p>
     * <p></p>
     * <p>从高液位降至低液位以下的时候就重新变为可写</p>
     *
     *
     * <p></p>
     * Returns the low water mark of the write buffer.  Once the number of bytes
     * queued in the write buffer exceeded the
     * {@linkplain #setWriteBufferHighWaterMark(int) high water mark} and then
     * dropped down below this value, {@link Channel#isWritable()} will start to return
     * {@code true} again.
     */
    int getWriteBufferLowWaterMark();

    /**
     * <p>
     * Sets the low water mark of the write buffer.  Once the number of bytes
     * queued in the write buffer exceeded the
     * {@linkplain #setWriteBufferHighWaterMark(int) high water mark} and then
     * dropped down below this value, {@link Channel#isWritable()} will start to return
     * {@code true} again.
     */
    ChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark);

    /**
     * 返回MessageSizeEstimator，该消息用于通道检测消息的大小。
     * <p></p>
     * Returns {@link MessageSizeEstimator} which is used for the channel
     * to detect the size of a message.
     */
    MessageSizeEstimator getMessageSizeEstimator();

    /**
     * Set the {@link MessageSizeEstimator} which is used for the channel
     * to detect the size of a message.
     */
    ChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator);

    /**
     * Returns the {@link WriteBufferWaterMark} which is used for setting the high and low
     * water mark of the write buffer.
     */
    WriteBufferWaterMark getWriteBufferWaterMark();

    /**
     * Set the {@link WriteBufferWaterMark} which is used for setting the high and low
     * water mark of the write buffer.
     */
    ChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark);
}
