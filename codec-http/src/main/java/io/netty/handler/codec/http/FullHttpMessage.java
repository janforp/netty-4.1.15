package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;

/**
 * Combines {@link HttpMessage} and {@link LastHttpContent} into one
 * message. So it represent a <i>complete</i> http message.
 */
public interface FullHttpMessage extends HttpMessage, LastHttpContent {

    @Override
    FullHttpMessage copy();

    @Override
    FullHttpMessage duplicate();

    @Override
    FullHttpMessage retainedDuplicate();

    @Override
    FullHttpMessage replace(ByteBuf content);

    @Override
    FullHttpMessage retain(int increment);

    @Override
    FullHttpMessage retain();

    @Override
    FullHttpMessage touch();

    @Override
    FullHttpMessage touch(Object hint);
}
