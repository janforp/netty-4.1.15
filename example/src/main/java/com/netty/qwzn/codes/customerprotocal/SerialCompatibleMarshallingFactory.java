package com.netty.qwzn.codes.customerprotocal;

import io.netty.handler.codec.marshalling.DefaultMarshallerProvider;
import io.netty.handler.codec.marshalling.MarshallerProvider;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;

/**
 * SerialCompatibleMarshallingFactory
 *
 * @author zhucj
 * @since 20210325
 */
public class SerialCompatibleMarshallingFactory {

    static MarshallerFactory createMarshallerFactory() {
        return Marshalling.getProvidedMarshallerFactory("serial");
    }

    static MarshallingConfiguration createMarshallingConfig() {
        // Create a configuration
        final MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setVersion(5);
        return configuration;
    }

    static MarshallerProvider createProvider() {
        return new DefaultMarshallerProvider(createMarshallerFactory(), createMarshallingConfig());
    }
}
