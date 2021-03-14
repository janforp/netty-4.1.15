package io.netty.buffer;

/**
 * Implementations are responsible to allocate buffers. Implementations of this interface are expected to be
 * thread-safe.
 *
 * 5.5.1 按需分配:ByteBufAllocator 接口
 * 为了降低分配和释放内存的开销，
 * Netty 通过 interface ByteBufAllocator 实现了 (ByteBuf 的)池化，
 * 它可以用来分配我们所描述过的任意类型的 ByteBuf 实例。使用池化是
 * 特定于应用程序的决定，其并不会以任何方式改变 ByteBuf API(的语义)。
 *
 * 可以通过 Channel(每个都可以有一个不同的 ByteBufAllocator 实例)或者绑定到 ChannelHandler 的 ChannelHandlerContext 获取一个到 ByteBufAllocator 的引用。
 */
public interface ByteBufAllocator {

    /**
     * Netty提供了两种ByteBufAllocator的实现:
     * PooledByteBufAllocator和UnpooledByteBufAllocator。
     * 前者池化了ByteBuf的实例以提高性能并最大限度地减少内存碎片。
     * 此实 现 使 用 了 一 种 称 为 j e m a l l o c
     * 的 已 被 大 量 现 代 操 作 系 统 所 采 用 的 高 效 方 法 来 分 配 内 存.
     *
     * 后 者 的 实 现 不池化ByteBuf实例，并且在每次它被调用时都会返回一个新的实例。
     *
     * @see PooledByteBufAllocator
     * @see UnpooledByteBufAllocator
     * 虽然Netty默认使用了PooledByteBufAllocator，但这可以很容易地通过
     * ChannelConfig API或者在引导你的应用程序时指定一个不同的分配器来更改。
     *
     * 5.5.2 Unpooled 缓冲区
     * @see Unpooled
     * 可能某些情况下，你未能获取一个到 ByteBufAllocator 的引用。
     * 对于这种情况，Netty 提 供了一个简单的称为 Unpooled 的工具类，
     * 它提供了静态的辅助方法来创建未池化的 ByteBuf 实例。表 5-8 列举了这些中最重要的方法。
     */

    ByteBufAllocator DEFAULT = ByteBufUtil.DEFAULT_ALLOCATOR;

    /**
     * Allocate a {@link ByteBuf}. If it is a direct or heap buffer
     * depends on the actual implementation.
     * 返回一个基于堆或者直接内存 存储的 ByteBuf
     */
    ByteBuf buffer();

    /**
     * Allocate a {@link ByteBuf} with the given initial capacity.
     * If it is a direct or heap buffer depends on the actual implementation.
     *
     * 返回一个基于堆或者直接内存 存储的 ByteBuf
     */
    ByteBuf buffer(int initialCapacity);

    /**
     * Allocate a {@link ByteBuf} with the given initial capacity and the given
     * maximal capacity. If it is a direct or heap buffer depends on the actual
     * implementation.
     *
     * 返回一个基于堆或者直接内存 存储的 ByteBuf
     */
    ByteBuf buffer(int initialCapacity, int maxCapacity);

    /**
     * Allocate a {@link ByteBuf}, preferably a direct buffer which is suitable for I/O.
     */
    ByteBuf ioBuffer();

    /**
     * Allocate a {@link ByteBuf}, preferably a direct buffer which is suitable for I/O.
     */
    ByteBuf ioBuffer(int initialCapacity);

    /**
     * Allocate a {@link ByteBuf}, preferably a direct buffer which is suitable for I/O.
     */
    ByteBuf ioBuffer(int initialCapacity, int maxCapacity);

    /**
     * Allocate a heap {@link ByteBuf}.
     * 返回一个基于堆内存存储的ByteBuf
     */
    ByteBuf heapBuffer();

    /**
     * Allocate a heap {@link ByteBuf} with the given initial capacity.
     * 返回一个基于堆内存存储的ByteBuf
     */
    ByteBuf heapBuffer(int initialCapacity);

    /**
     * Allocate a heap {@link ByteBuf} with the given initial capacity and the given
     * maximal capacity.
     *
     * 返回一个基于堆内存存储的ByteBuf
     */
    ByteBuf heapBuffer(int initialCapacity, int maxCapacity);

    /**
     * Allocate a direct {@link ByteBuf}.
     *
     * 返回一个基于直接内存存储的ByteBuf
     */
    ByteBuf directBuffer();

    /**
     * Allocate a direct {@link ByteBuf} with the given initial capacity.
     * 返回一个基于直接内存存储的ByteBuf
     */
    ByteBuf directBuffer(int initialCapacity);

    /**
     * Allocate a direct {@link ByteBuf} with the given initial capacity and the given
     * maximal capacity.
     *
     * 返回一个基于直接内存存储的ByteBuf
     */
    ByteBuf directBuffer(int initialCapacity, int maxCapacity);

    /**
     * Allocate a {@link CompositeByteBuf}.
     * If it is a direct or heap buffer depends on the actual implementation.
     *
     * 返回一个可以通过添加最大到 指定数目的基于堆的或者直接
     * 内存存储的缓冲区来扩展的 CompositeByteBuf
     */
    CompositeByteBuf compositeBuffer();

    /**
     * Allocate a {@link CompositeByteBuf} with the given maximum number of components that can be stored in it.
     * If it is a direct or heap buffer depends on the actual implementation.
     *
     * 返回一个可以通过添加最大到 指定数目的基于堆的或者直接
     * 内存存储的缓冲区来扩展的 CompositeByteBuf
     */
    CompositeByteBuf compositeBuffer(int maxNumComponents);

    /**
     * Allocate a heap {@link CompositeByteBuf}.
     */
    CompositeByteBuf compositeHeapBuffer();

    /**
     * Allocate a heap {@link CompositeByteBuf} with the given maximum number of components that can be stored in it.
     */
    CompositeByteBuf compositeHeapBuffer(int maxNumComponents);

    /**
     * Allocate a direct {@link CompositeByteBuf}.
     */
    CompositeByteBuf compositeDirectBuffer();

    /**
     * Allocate a direct {@link CompositeByteBuf} with the given maximum number of components that can be stored in it.
     */
    CompositeByteBuf compositeDirectBuffer(int maxNumComponents);

    /**
     * Returns {@code true} if direct {@link ByteBuf}'s are pooled
     */
    boolean isDirectBufferPooled();

    /**
     * Calculate the new capacity of a {@link ByteBuf} that is used when a {@link ByteBuf} needs to expand by the
     * {@code minNewCapacity} with {@code maxCapacity} as upper-bound.
     */
    int calculateNewCapacity(int minNewCapacity, int maxCapacity);
}
