package io.netty.handler.codec.http.websocketx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.util.internal.StringUtil;

/**
 * Base class for web socket frames
 */
public abstract class WebSocketFrame extends DefaultByteBufHolder {

    /**
     * Flag to indicate if this frame is the final fragment in a message. The first fragment (frame) may also be the
     * final fragment.
     */
    private final boolean finalFragment;

    /**
     * RSV1, RSV2, RSV3 used for extensions
     */
    private final int rsv;

    protected WebSocketFrame(ByteBuf binaryData) {
        this(true, 0, binaryData);
    }

    protected WebSocketFrame(boolean finalFragment, int rsv, ByteBuf binaryData) {
        super(binaryData);
        this.finalFragment = finalFragment;
        this.rsv = rsv;
    }

    /**
     * Flag to indicate if this frame is the final fragment in a message. The first fragment (frame) may also be the
     * final fragment.
     */
    public boolean isFinalFragment() {
        return finalFragment;
    }

    /**
     * Bits used for extensions to the standard.
     */
    public int rsv() {
        return rsv;
    }

    @Override
    public WebSocketFrame copy() {
        return (WebSocketFrame) super.copy();
    }

    @Override
    public WebSocketFrame duplicate() {
        return (WebSocketFrame) super.duplicate();
    }

    @Override
    public WebSocketFrame retainedDuplicate() {
        return (WebSocketFrame) super.retainedDuplicate();
    }

    @Override
    public abstract WebSocketFrame replace(ByteBuf content);

    @Override
    public String toString() {
        return StringUtil.simpleClassName(this) + "(data: " + contentToString() + ')';
    }

    @Override
    public WebSocketFrame retain() {
        super.retain();
        return this;
    }

    @Override
    public WebSocketFrame retain(int increment) {
        super.retain(increment);
        return this;
    }

    @Override
    public WebSocketFrame touch() {
        super.touch();
        return this;
    }

    @Override
    public WebSocketFrame touch(Object hint) {
        super.touch(hint);
        return this;
    }
}
