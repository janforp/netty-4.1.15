package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.StringUtil;

/**
 * The default {@link HttpContent} implementation.
 */
public class DefaultHttpContent extends DefaultHttpObject implements HttpContent {

    private final ByteBuf content;

    /**
     * Creates a new instance with the specified chunk content.
     */
    public DefaultHttpContent(ByteBuf content) {
        if (content == null) {
            throw new NullPointerException("content");
        }
        this.content = content;
    }

    @Override
    public ByteBuf content() {
        return content;
    }

    @Override
    public HttpContent copy() {
        return replace(content.copy());
    }

    @Override
    public HttpContent duplicate() {
        return replace(content.duplicate());
    }

    @Override
    public HttpContent retainedDuplicate() {
        return replace(content.retainedDuplicate());
    }

    @Override
    public HttpContent replace(ByteBuf content) {
        return new DefaultHttpContent(content);
    }

    @Override
    public int refCnt() {
        return content.refCnt();
    }

    @Override
    public HttpContent retain() {
        content.retain();
        return this;
    }

    @Override
    public HttpContent retain(int increment) {
        content.retain(increment);
        return this;
    }

    @Override
    public HttpContent touch() {
        content.touch();
        return this;
    }

    @Override
    public HttpContent touch(Object hint) {
        content.touch(hint);
        return this;
    }

    @Override
    public boolean release() {
        return content.release();
    }

    @Override
    public boolean release(int decrement) {
        return content.release(decrement);
    }

    @Override
    public String toString() {
        return StringUtil.simpleClassName(this) +
                "(data: " + content() + ", decoderResult: " + decoderResult() + ')';
    }
}
