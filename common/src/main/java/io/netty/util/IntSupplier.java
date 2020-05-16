package io.netty.util;

/**
 * Represents a supplier of {@code int}-valued results.
 */
public interface IntSupplier {

    /**
     * Gets a result.
     *
     * @return a result
     */
    int get() throws Exception;
}
