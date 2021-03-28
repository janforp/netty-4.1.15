package io.netty.buffer;

import io.netty.util.ReferenceCounted;

/**
 * A packet which is send or receive.
 * 发送或接收的数据包。
 *
 * ByteBuf 的容器！！！！
 *
 *
 * 例如HTTp协议的请求消息跟应答消息都可以携带消息体，这个消息体在netty中就是一个ByteBuf对象。
 * 由于不同的协议消息体可以包含不同的协议字段和功能，因此，需要对ByteBuf进行包装和抽象，不同的子类有不同的实现
 *
 * @see io.netty.handler.codec.http.websocketx.PingWebSocketFrame
 * @see io.netty.handler.codec.http.websocketx.PongWebSocketFrame
 * @see io.netty.handler.codec.http.DefaultHttpContent
 */
public interface ByteBufHolder extends ReferenceCounted {

    /**
     * Return the data which is held by this {@link ByteBufHolder}.
     */
    ByteBuf content();

    /**
     * Creates a deep copy of this {@link ByteBufHolder}.
     */
    ByteBufHolder copy();

    /**
     * Duplicates this {@link ByteBufHolder}. Be aware that this will not automatically call {@link #retain()}.
     */
    ByteBufHolder duplicate();

    /**
     * Duplicates this {@link ByteBufHolder}. This method returns a retained duplicate unlike {@link #duplicate()}.
     *
     * @see ByteBuf#retainedDuplicate()
     */
    ByteBufHolder retainedDuplicate();

    /**
     * Returns a new {@link ByteBufHolder} which contains the specified {@code content}.
     */
    ByteBufHolder replace(ByteBuf content);

    @Override
    ByteBufHolder retain();//保持:保持

    @Override
    ByteBufHolder retain(int increment);

    @Override
    ByteBufHolder touch();

    @Override
    ByteBufHolder touch(Object hint);
}
