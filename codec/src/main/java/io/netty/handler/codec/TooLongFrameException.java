package io.netty.handler.codec;

/**
 * An {@link DecoderException} which is thrown when the length of the frame
 * decoded is greater than the allowed maximum.
 */
public class TooLongFrameException extends DecoderException {

    /**
     * 由于 Netty 是一个异步框架，所以需要在字节可以解码之前在内存中缓冲它们。因此，不能 让解码器缓冲大量的数据以至于耗尽可用的内存。
     *
     * 为了解除这个常见的顾虑，Netty 提供了 TooLongFrameException 类，其将由解码器在帧超出指定的大小限制时抛出。
     *
     * 为了避免这种情况，你可以设置一个最大字节数的阈值，如果超出该阈值，则会导致抛出一 个 TooLongFrameException
     * (随后会被 ChannelHandler.exceptionCaught()方法捕 获)。
     *
     * 然后，如何处理该异常则完全取决于该解码器的用户。
     */

    private static final long serialVersionUID = -1995801950698951640L;

    /**
     * Creates a new instance.
     */
    public TooLongFrameException() {
    }

    /**
     * Creates a new instance.
     */
    public TooLongFrameException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new instance.
     */
    public TooLongFrameException(String message) {
        super(message);
    }

    /**
     * Creates a new instance.
     */
    public TooLongFrameException(Throwable cause) {
        super(cause);
    }
}