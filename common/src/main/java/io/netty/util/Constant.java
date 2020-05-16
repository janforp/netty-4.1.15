package io.netty.util;

/**
 * 通过==运算符可以安全比较的单例。由ConstantPool创建和管理。
 *
 * 单例：具体实现终会体现，一个name只能有对应的一个常量
 * <p></p>
 * A singleton which is safe to compare via the {@code ==} operator. Created and managed by {@link ConstantPool}.
 */
public interface Constant<T extends Constant<T>> extends Comparable<T> {

    /**
     * Returns the unique number assigned to this {@link Constant}.
     */
    int id();

    /**
     * Returns the name of this {@link Constant}.
     */
    String name();
}
