package io.netty.handler.codec.http.websocketx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.util.internal.StringUtil;

/**
 * Base class for web socket frames
 */
public abstract class WebSocketFrame extends DefaultByteBufHolder {

    /**
     * 11.2.5 WebSocket
     *
     * WebSocket解决了一个长期存在的问题:既然底层的协议(HTTP)是一个请求/响应模式的 交互序列，
     * 那么如何实时地发布信息呢?AJAX提供了一定程度上的改善，但是数据流仍然是由 客户端所发送的请求驱动的。
     * 还有其他的一些或多或少的取巧方式，但是最终它们仍然属于扩 展性受限的变通之法。
     *
     *
     * WebSocket规范以及它的实现代表了对一种更加有效的解决方案的尝试。
     * 简单地说， WebSocket提供了“在一个单个的TCP连接上提供双向的通信......
     * 结合WebSocket API......它为网 页和远程服务器之间的双向通信提供了一种替代HTTP轮询的方案。
     *
     * 也就是说，WebSocket 在客户端和服务器之间提供了真正的双向数据交换。
     * 我们不会深入地 描述太多的内部细节，但是我们还是应该提到，尽管最早的实现仅限于文本数据，
     * 但是现在已经 不是问题了;WebSocket 现在可以用于传输任意类型的数据，很像普通的套接字。
     */

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
