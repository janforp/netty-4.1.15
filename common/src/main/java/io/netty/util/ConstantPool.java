package io.netty.util;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 常量池的基类
 * A pool of {@link Constant}s.
 *
 * @param <T> the type of the constant
 */
public abstract class ConstantPool<T extends Constant<T>> {

    /**
     * 该常量池使用一个 ConcurrentMap 来维护一系列的常量
     */
    private final ConcurrentMap<String, T> constants = PlatformDependent.newConcurrentHashMap();

    /**
     * 生成常量的id
     *
     * @see Constant#id()
     */
    private final AtomicInteger nextId = new AtomicInteger(1);

    /**
     * 构造一个常量(Constant 或者 Constant 的子类),内部其实调用了 valueOf(String name)
     * <p></p>
     * Shortcut of {@link #valueOf(String) valueOf(firstNameComponent.getName() + "#" + secondNameComponent)}.
     */
    public T valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        if (firstNameComponent == null) {
            throw new NullPointerException("firstNameComponent");
        }
        if (secondNameComponent == null) {
            throw new NullPointerException("secondNameComponent");
        }

        return valueOf(firstNameComponent.getName() + '#' + secondNameComponent);
    }

    /**
     * 构造一个常量(Constant 或者 Constant 的子类)
     * 该常量的名称是传入的 name
     *
     * 如果该名次的常量不存在，则新建一个随后返回，后续的该名称的常量就只有这一个实例
     *
     * @param name the name of the {@link Constant}
     * @see Constant#name()
     *
     *
     *
     * <p></p>
     * Returns the {@link Constant} which is assigned to the specified {@code name}.
     * If there's no such {@link Constant}, a new one will be created and returned.
     * Once created, the subsequent(随后的) calls with the same {@code name} will always return the previously created one
     * (i.e. singleton.)
     */
    public T valueOf(String name) {
        checkNotNullAndNotEmpty(name);
        return getOrCreate(name);
    }

    /**
     * Get existing constant by name or creates new one if not exists. Threadsafe
     *
     * @param name the name of the {@link Constant}
     */
    private T getOrCreate(String name) {
        //先去缓存中去拿
        T constant = constants.get(name);
        if (constant == null) {
            //实例化常量的逻辑交给实现，模版方法设计模式
            final T tempConstant = newConstant(nextId(), name);
            //此处二次检查，保证线程安全
            //如果存在，则直接返回存在的值，新值不会塞进去，如果不存在，则放入新值，返回之前的值(null)
            constant = constants.putIfAbsent(name, tempConstant);
            if (constant == null) {
                //说明之前不存在
                return tempConstant;
            }
        }

        return constant;
    }

    /**
     * Returns {@code true} if a {@link AttributeKey} exists for the given {@code name}.
     */
    public boolean exists(String name) {
        checkNotNullAndNotEmpty(name);
        return constants.containsKey(name);
    }

    /**
     * 为给定名称创建一个新的 Constant，如果给定名称的 Constant 存在，则失败，并抛出IllegalArgumentException。
     * <p></p>
     *
     *
     *
     * Creates a new {@link Constant} for the given {@code name} or fail with an
     * {@link IllegalArgumentException} if a {@link Constant} for the given {@code name} exists.
     */
    public T newInstance(String name) {
        checkNotNullAndNotEmpty(name);
        return createOrThrow(name);
    }

    /**
     * Creates constant by name or throws exception. Threadsafe
     *
     * @param name the name of the {@link Constant}
     */
    private T createOrThrow(String name) {
        T constant = constants.get(name);
        if (constant == null) {
            final T tempConstant = newConstant(nextId(), name);
            constant = constants.putIfAbsent(name, tempConstant);
            if (constant == null) {
                return tempConstant;
            }
        }
        //给的名称的 Constant 存在，抛出异常
        throw new IllegalArgumentException(String.format("'%s' is already in use", name));
    }

    private static String checkNotNullAndNotEmpty(String name) {
        ObjectUtil.checkNotNull(name, "name");

        if (name.isEmpty()) {
            throw new IllegalArgumentException("empty name");
        }

        return name;
    }

    /**
     * 实例化常量的逻辑交给实现，模版方法设计模式
     *
     * @param id
     * @param name
     * @return
     */
    protected abstract T newConstant(int id, String name);

    @Deprecated
    public final int nextId() {
        return nextId.getAndIncrement();
    }
}
