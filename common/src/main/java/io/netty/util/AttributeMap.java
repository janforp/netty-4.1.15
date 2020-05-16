package io.netty.util;

/**
 * Holds {@link Attribute}s which can be accessed via {@link AttributeKey}.
 *
 * Implementations must be Thread-safe.
 */
public interface AttributeMap {

    /**
     * Get the {@link Attribute} for the given {@link AttributeKey}. This method will never return null, but may return
     * an {@link Attribute} which does not have a value set yet.
     *
     * 通过一个 AttributeKey 查询对应的 Attribute，不会返回null,但是可能返回一个 Attribute，他里面存的值是null
     */
    <T> Attribute<T> attr(AttributeKey<T> key);

    /**
     * Returns {@code} true if and only if the given {@link Attribute} exists in this {@link AttributeMap}.
     * 判断是否有该 key
     */
    <T> boolean hasAttr(AttributeKey<T> key);
}
