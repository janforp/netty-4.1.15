package io.netty.channel;

/**
 * 封装了一个高低液位的报警阀值
 * <p></p>
 * 并且提供了一个默认值。
 * <p></p>
 * WriteBufferWaterMark用于设置写缓冲区的低水位标记和高水位标记。
 * <P>如果写入缓冲区中排队的字节数超过了高水位线，则Channel.isWritable（）将开始返回false。如果在写缓冲区中排队的字节数超过了高水位线，然后降到了低水位线以下，则Channel.isWritable（）将再次开始返回true。</P>
 * <p></p>
 * WriteBufferWaterMark is used to set low water mark and high water mark for the write buffer.
 * <p>
 * If the number of bytes queued in the write buffer exceeds the
 * {@linkplain #high high water mark}, {@link Channel#isWritable()}
 * will start to return {@code false}.
 * <p>
 * If the number of bytes queued in the write buffer exceeds the
 * {@linkplain #high high water mark} and then
 * dropped down below the {@linkplain #low low water mark},
 * {@link Channel#isWritable()} will start to return
 * {@code true} again.
 */
public final class WriteBufferWaterMark {

    /**
     * 写缓冲区的低液位报警阀值
     */
    private static final int DEFAULT_LOW_WATER_MARK = 32 * 1024;

    /**
     * 写缓冲区的高液位报警阀值
     */
    private static final int DEFAULT_HIGH_WATER_MARK = 64 * 1024;

    public static final WriteBufferWaterMark DEFAULT =
            new WriteBufferWaterMark(DEFAULT_LOW_WATER_MARK, DEFAULT_HIGH_WATER_MARK, false);

    private final int low;

    private final int high;

    /**
     * Create a new instance.
     *
     * @param low low water mark for write buffer.
     * @param high high water mark for write buffer
     */
    public WriteBufferWaterMark(int low, int high) {
        this(low, high, true);
    }

    /**
     * This constructor is needed to keep backward-compatibility.
     */
    WriteBufferWaterMark(int low, int high, boolean validate) {
        if (validate) {
            if (low < 0) {
                throw new IllegalArgumentException("write buffer's low water mark must be >= 0");
            }
            if (high < low) {
                throw new IllegalArgumentException(
                        "write buffer's high water mark cannot be less than " +
                                " low water mark (" + low + "): " +
                                high);
            }
        }
        this.low = low;
        this.high = high;
    }

    /**
     * Returns the low water mark for the write buffer.
     */
    public int low() {
        return low;
    }

    /**
     * Returns the high water mark for the write buffer.
     */
    public int high() {
        return high;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(55)
                .append("WriteBufferWaterMark(low: ")
                .append(low)
                .append(", high: ")
                .append(high)
                .append(")");
        return builder.toString();
    }

}
