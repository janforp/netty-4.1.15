package io.netty.channel;

/**
 * 负责估计 数据 的大小。大小表示 数据 将占用多少内存。保留在内存中。
 * <P></P>
 * Responsible to estimate size of a message. The size represent how much memory the message will ca. reserve in memory.
 */
public interface MessageSizeEstimator {

    /**
     * 创建一个新的 Handle。Handle 提供实际操作。
     * <P></P>
     * Creates a new handle. The handle provides the actual operations.
     */
    Handle newHandle();

    interface Handle {

        /**
         * Calculate the size of the given message.
         *
         * @param msg The message for which the size should be calculated
         * @return size     The size in bytes. The returned size must be >= 0
         */
        int size(Object msg);
    }
}
