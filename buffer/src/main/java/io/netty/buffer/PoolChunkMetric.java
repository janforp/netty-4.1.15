package io.netty.buffer;

/**
 * Metrics for a chunk.
 *
 * 池块公制
 */
public interface PoolChunkMetric {

    /**
     * Return the percentage of the current usage of the chunk.
     *
     * 返回该块当前使用量的百分比。
     */
    int usage();

    /**
     * Return the size of the chunk in bytes, this is the maximum of bytes that can be served out of the chunk.
     *
     * 返回块的大小（以字节为单位），这是可以从块中提供的最大字节数。
     */
    int chunkSize();

    /**
     * Return the number of free bytes in the chunk.
     *
     * 返回块中的空闲字节数。
     */
    int freeBytes();
}
