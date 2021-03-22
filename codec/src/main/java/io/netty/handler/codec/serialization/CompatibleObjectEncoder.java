package io.netty.handler.codec.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * An encoder which serializes a Java object into a {@link ByteBuf}
 * (interoperability version).
 * <p>
 * This encoder is interoperable with the standard Java object streams such as
 * {@link ObjectInputStream} and {@link ObjectOutputStream}.
 *
 * Compatible：兼容的
 *
 * 和使用 JDK 序列化的非基于 Netty 的远程节点进行互操作的编码器
 *
 * Marshalling:编组
 */
public class CompatibleObjectEncoder extends MessageToByteEncoder<Serializable> {

    private final int resetInterval;

    private int writtenObjects;

    /**
     * Creates a new instance with the reset interval of {@code 16}.
     */
    public CompatibleObjectEncoder() {
        this(16); // Reset at every sixteen writes
    }

    /**
     * Creates a new instance.
     *
     * @param resetInterval the number of objects between {@link ObjectOutputStream#reset()}.
     * {@code 0} will disable resetting the stream, but the remote
     * peer will be at the risk of getting {@link OutOfMemoryError} in
     * the long term.
     */
    public CompatibleObjectEncoder(int resetInterval) {
        if (resetInterval < 0) {
            throw new IllegalArgumentException("resetInterval: " + resetInterval);
        }
        this.resetInterval = resetInterval;
    }

    /**
     * Creates a new {@link ObjectOutputStream} which wraps the specified
     * {@link OutputStream}.  Override this method to use a subclass of the
     * {@link ObjectOutputStream}.
     */
    protected ObjectOutputStream newObjectOutputStream(OutputStream out) throws Exception {
        return new ObjectOutputStream(out);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Serializable msg, ByteBuf out) throws Exception {
        ObjectOutputStream oos = newObjectOutputStream(new ByteBufOutputStream(out));
        try {
            if (resetInterval != 0) {
                // Resetting will prevent OOM on the receiving side.
                writtenObjects++;
                if (writtenObjects % resetInterval == 0) {
                    oos.reset();
                }
            }

            oos.writeObject(msg);
            oos.flush();
        } finally {
            oos.close();
        }
    }
}
