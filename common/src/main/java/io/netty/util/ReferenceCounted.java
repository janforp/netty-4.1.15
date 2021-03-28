package io.netty.util;

/**
 * A reference-counted object that requires explicit deallocation.
 * <p>
 * When a new {@link ReferenceCounted} is instantiated, it starts with the reference count of {@code 1}.
 * {@link #retain()} increases the reference count, and {@link #release()} decreases the reference count.
 * If the reference count is decreased to {@code 0}, the object will be deallocated explicitly, and accessing
 * the deallocated object will usually result in an access violation.
 * </p>
 * <p>
 * If an object that implements {@link ReferenceCounted} is a container of other objects that implement
 * {@link ReferenceCounted}, the contained objects will also be released via {@link #release()} when the container's
 * reference count becomes 0.（容器的引用计数为空，则内部对象也会释放）
 * </p>
 *
 * 5.6 引用计数 引用计数是一种通过在某个对象所持有的资源不再被其他对象引用时释放该对象所持有的
 * 资源来优化内存使用和性能的技术。Netty 在第 4 版中为 ByteBuf 和 ByteBufHolder
 * 引入了 引用计数技术，它们都实现了 interface ReferenceCounted。
 *
 * 引用计数背后的想法并不是特别的复杂;它主要涉及跟踪到某个特定对象的活动引用的数 量。
 * 一个 ReferenceCounted 实现的实例将通常以活动的引用计数为 1 作为开始。
 * 只要引用计 数大于 0，就能保证对象不会被释放。
 * 当活动引用的数量减少到 0 时，该实例就会被释放。
 * 注意， 虽然释放的确切语义可能是特定于实现的，但是至少已经释放的对象应该不可再用了。
 *
 * 引用计数对于池化实现(如 PooledByteBufAllocator)来说是至关重要的，它降低了 内存分配的开销.
 *
 * @see com.nettyinaction.codes._14_RefCnt
 * 试图访问一个已经被释放的引用计数的对象，将会导致一个 IllegalReferenceCountException。
 * 注意，一个特定的(ReferenceCounted 的实现)类，可以用它自己的独特方式来定义它 的引用计数规则。
 * 例如，我们可以设想一个类，其 release()方法的实现总是将引用计数设为 零，
 * 而不用关心它的当前值，从而一次性地使所有的活动引用都失效。
 *
 * 谁负责释放 一般来说，是由最后访问(引用计数)对象的那一方来负责将它释放。
 * 在第 6 章中， 我们将会解释这个概念和 ChannelHandler 以及 ChannelPipeline 的相关性。
 */
public interface ReferenceCounted {

    /**
     * deallocated:释放
     * refCnt:reference count
     *
     * Returns the reference count of this object.  If {@code 0}, it means this object has been deallocated.
     */
    int refCnt();

    /**
     * Increases the reference count by {@code 1}.
     */
    ReferenceCounted retain();

    /**
     * Increases the reference count by the specified {@code increment}.
     */
    ReferenceCounted retain(int increment);

    /**
     * Records the current access location of this object for debugging purposes.
     * -- 记录此对象的当前访问位置以进行调试
     *
     * If this object is determined to be leaked, the information recorded by this operation will be provided to you
     * via {@link ResourceLeakDetector}.  This method is a shortcut to {@link #touch(Object) touch(null)}.
     */
    ReferenceCounted touch();

    /**
     * Records the current access location of this object with an additional arbitrary information for debugging
     * purposes.  If this object is determined to be leaked, the information recorded by this operation will be
     * provided to you via {@link ResourceLeakDetector}.
     */
    ReferenceCounted touch(Object hint);

    /**
     * Decreases the reference count by {@code 1} and deallocates this object if the reference count reaches at
     * {@code 0}.
     *
     * @return {@code true} if and only if the reference count became {@code 0} and this object has been deallocated
     */
    boolean release();

    /**
     * Decreases the reference count by the specified {@code decrement} and deallocates this object if the reference
     * count reaches at {@code 0}.
     *
     * @return {@code true} if and only if the reference count became {@code 0} and this object has been deallocated
     */
    boolean release(int decrement);
}
