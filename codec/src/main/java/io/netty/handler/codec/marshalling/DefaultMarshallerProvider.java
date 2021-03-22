package io.netty.handler.codec.marshalling;

import io.netty.channel.ChannelHandlerContext;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;

/**
 * Default implementation of {@link MarshallerProvider} which just create a new {@link Marshaller}
 * on ever {@link #getMarshaller(ChannelHandlerContext)} call.
 */
public class DefaultMarshallerProvider implements MarshallerProvider {

    private final MarshallerFactory factory;

    private final MarshallingConfiguration config;

    /**
     * Create a new instance
     *
     * @param factory the {@link MarshallerFactory} to use to create {@link Marshaller}
     * @param config the {@link MarshallingConfiguration}
     */
    public DefaultMarshallerProvider(MarshallerFactory factory, MarshallingConfiguration config) {
        this.factory = factory;
        this.config = config;
    }

    @Override
    public Marshaller getMarshaller(ChannelHandlerContext ctx) throws Exception {
        return factory.createMarshaller(config);
    }
}