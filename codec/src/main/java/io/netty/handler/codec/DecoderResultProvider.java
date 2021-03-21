package io.netty.handler.codec;

/**
 * Provides the accessor methods for the {@link DecoderResult} property of a decoded message.
 */
public interface DecoderResultProvider {

    /**
     * Returns the result of decoding this object.
     */
    DecoderResult decoderResult();

    /**
     * Updates the result of decoding this object. This method is supposed to be invoked by a decoder.
     * Do not call this method unless you know what you are doing.
     */
    void setDecoderResult(DecoderResult result);
}