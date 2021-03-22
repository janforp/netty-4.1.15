package io.netty.handler.codec.marshalling;

import io.netty.channel.ChannelHandlerContext;
import org.jboss.marshalling.Marshaller;

/**
 * This provider is responsible to get a {@link Marshaller} for the given {@link ChannelHandlerContext}.
 */
public interface MarshallerProvider {

    /**
     * Get a {@link Marshaller} for the given {@link ChannelHandlerContext}
     */
    Marshaller getMarshaller(ChannelHandlerContext ctx) throws Exception;
}