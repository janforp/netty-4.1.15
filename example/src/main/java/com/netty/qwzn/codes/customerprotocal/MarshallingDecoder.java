package com.netty.qwzn.codes.customerprotocal;

import com.netty.qwzn.codes.customerprotocal.codec.ChannelBufferByteInput;
import io.netty.buffer.ByteBuf;
import org.jboss.marshalling.Unmarshaller;

import java.io.IOException;

/**
 * MarshallingDecoder
 *
 * @author zhucj
 * @since 20210325
 */
public class MarshallingDecoder {

    private final Unmarshaller unmarshaller;

    public MarshallingDecoder() throws IOException {
        unmarshaller = MarshallingCodecFactory.buildUnmarshalling();
    }

    public Object decode(ByteBuf in) throws IOException, ClassNotFoundException {
        int objectSize = in.readInt();
        ByteBuf buf = in.slice(in.readerIndex(), objectSize);
        ChannelBufferByteInput input = new ChannelBufferByteInput(buf);
        try {
            unmarshaller.start(input);
            Object obj = unmarshaller.readObject();
            unmarshaller.finish();

            in.readerIndex(in.readerIndex() + objectSize);
            return obj;

        } finally {
            unmarshaller.close();
        }
    }
}
