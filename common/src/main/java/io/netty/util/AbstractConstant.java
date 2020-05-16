package io.netty.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 常量的一个抽象基类
 *
 * <p></p>
 * 他是一个常量类型，与常量池无关
 *
 *
 * <p></p>
 * Base implementation of {@link Constant}.
 */
public abstract class AbstractConstant<T extends AbstractConstant<T>> implements Constant<T> {

    /**
     * 全局唯一
     */
    private static final AtomicLong uniqueIdGenerator = new AtomicLong();

    /**
     * 常量的唯一id
     */
    private final int id;

    /**
     * 常量名称
     */
    private final String name;

    /**
     * 唯一化器
     *
     * <p></p>
     * 构造器中执行：
     * this.uniquifier = uniqueIdGenerator.getAndIncrement();
     *
     * <p>
     * 因为 uniqueIdGenerator 是全局唯一的(JVM)，所以 uniquifier 也能保证全局唯一
     * </p>
     *
     * <p>
     * 作用：因为常量需要实现比较器接口，一般情况下比较直接通过hashCode即可，但是
     * 当哈希值一样的时候就使用该值进行比较，由于该值每一个常量对象都不同相同，所以肯定会达到比较器不返回0的目的
     * </p>
     */
    private final long uniquifier;

    /**
     * Creates a new instance.
     */
    protected AbstractConstant(int id, String name) {
        this.id = id;
        this.name = name;
        this.uniquifier = uniqueIdGenerator.getAndIncrement();
    }

    @Override
    public final String name() {
        return name;
    }

    @Override
    public final int id() {
        return id;
    }

    @Override
    public final String toString() {
        return name();
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public final int compareTo(T o) {
        if (this == o) {
            return 0;
        }

        //T extends AbstractConstant<T>
        @SuppressWarnings("UnnecessaryLocalVariable")
        AbstractConstant<T> other = o;
        int returnCode;

        returnCode = hashCode() - other.hashCode();
        if (returnCode != 0) {
            return returnCode;
        }

        if (uniquifier < other.uniquifier) {
            return -1;
        }
        if (uniquifier > other.uniquifier) {
            return 1;
        }

        throw new Error("failed to compare two different constants");
    }
}
