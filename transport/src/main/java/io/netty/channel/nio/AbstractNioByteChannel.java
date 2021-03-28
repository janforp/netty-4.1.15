package io.netty.channel.nio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.FileRegion;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 * 字节操作的基类
 *
 * <p></p>
 * {@link AbstractNioChannel} base class for {@link Channel}s that operate on bytes.
 */
public abstract class AbstractNioByteChannel extends AbstractNioChannel {

    private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);

    private static final String EXPECTED_TYPES =
            " (expected: " + StringUtil.simpleClassName(ByteBuf.class) + ", " +
                    StringUtil.simpleClassName(FileRegion.class) + ')';

    /**
     * @see AbstractNioByteChannel#incompleteWrite(boolean)
     */
    private Runnable flushTask;

    /**
     * Create a new instance
     *
     * @param parent the parent {@link Channel} by which this instance was created. May be {@code null}
     * @param ch the underlying {@link SelectableChannel} on which it operates
     */
    protected AbstractNioByteChannel(Channel parent, SelectableChannel ch) {
        super(parent, ch, SelectionKey.OP_READ);
    }

    /**
     * Shutdown the input side of the channel.
     */
    protected abstract ChannelFuture shutdownInput();

    protected boolean isInputShutdown0() {
        return false;
    }

    @Override
    protected AbstractNioUnsafe newUnsafe() {
        return new NioByteUnsafe();
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    protected class NioByteUnsafe extends AbstractNioUnsafe {

        private void closeOnRead(ChannelPipeline pipeline) {
            if (!isInputShutdown0()) {
                if (Boolean.TRUE.equals(config().getOption(ChannelOption.ALLOW_HALF_CLOSURE))) {
                    shutdownInput();
                    pipeline.fireUserEventTriggered(ChannelInputShutdownEvent.INSTANCE);
                } else {
                    close(voidPromise());
                }
            } else {
                pipeline.fireUserEventTriggered(ChannelInputShutdownReadComplete.INSTANCE);
            }
        }

        private void handleReadException(ChannelPipeline pipeline, ByteBuf byteBuf, Throwable cause, boolean close,
                RecvByteBufAllocator.Handle allocHandle) {
            if (byteBuf != null) {
                if (byteBuf.isReadable()) {
                    readPending = false;
                    pipeline.fireChannelRead(byteBuf);
                } else {
                    byteBuf.release();
                }
            }
            allocHandle.readComplete();
            pipeline.fireChannelReadComplete();
            pipeline.fireExceptionCaught(cause);
            if (close || cause instanceof IOException) {
                closeOnRead(pipeline);
            }
        }

        @Override
        public final void read() {
            final ChannelConfig config = config();
            final ChannelPipeline pipeline = pipeline();
            final ByteBufAllocator allocator = config.getAllocator();
            final RecvByteBufAllocator.Handle allocHandle = recvBufAllocHandle();
            allocHandle.reset(config);

            ByteBuf byteBuf = null;
            boolean close = false;
            try {
                do {
                    byteBuf = allocHandle.allocate(allocator);
                    allocHandle.lastBytesRead(doReadBytes(byteBuf));
                    if (allocHandle.lastBytesRead() <= 0) {
                        // nothing was read. release the buffer.
                        byteBuf.release();
                        byteBuf = null;
                        close = allocHandle.lastBytesRead() < 0;
                        break;
                    }

                    allocHandle.incMessagesRead(1);
                    readPending = false;
                    pipeline.fireChannelRead(byteBuf);
                    byteBuf = null;
                } while (allocHandle.continueReading());

                allocHandle.readComplete();
                pipeline.fireChannelReadComplete();

                if (close) {
                    closeOnRead(pipeline);
                }
            } catch (Throwable t) {
                handleReadException(pipeline, byteBuf, t, close, allocHandle);
            } finally {
                // Check if there is a readPending which was not processed yet.
                // This could be for two reasons:
                // * The user called Channel.read() or ChannelHandlerContext.read() in channelRead(...) method
                // * The user called Channel.read() or ChannelHandlerContext.read() in channelReadComplete(...) method
                //
                // See https://github.com/netty/netty/issues/2254
                if (!readPending && !config.isAutoRead()) {
                    removeReadOp();
                }
            }
        }
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        int writeSpinCount = -1;

        //写半包是否完成标识
        boolean setOpWrite = false;
        for (; ; ) {
            Object msg = in.current();
            if (msg == null) {
                // Wrote all messages.
                //所有消息都写完了
                clearOpWrite();
                // Directly return here so incompleteWrite(...) is not called.
                return;
            }

            //如果需要发送的消息不为空，则继续往下执行

            if (msg instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) msg;
                int readableBytes = buf.readableBytes();
                if (readableBytes == 0) {

                    //如果可读消息为0，则说明该消息不可读，需要从环形发送数组中删除该消息，继续处理其他的消息
                    in.remove();
                    continue;
                }

                //消息是否全部发送完毕
                boolean done = false;

                //发送的总字节数
                long flushedAmount = 0;
                if (writeSpinCount == -1) {

                    //如果为-1，则从 Channel 配置对象中获取循环发送次数
                    writeSpinCount = config().getWriteSpinCount();
                }
                for (int i = writeSpinCount - 1; i >= 0; i--) {

                    /**
                     * 模版方法，子类实现
                     */
                    int localFlushedAmount = doWriteBytes(buf);
                    if (localFlushedAmount == 0) {
                        /**
                         * 本次发送的数量=0，说明TCP缓冲区已经满了，发生了 ZERO_WINDOW
                         * 此时如果继续循环发送则继续可能出现0字节的情况吗，空循环会占用CPU
                         * 资源，导致I/O线程无法处理其他事件，所以将写半包标识设置为 true,
                         * 退出循环,释放I/O线程
                         */

                        //写半包标识完成
                        setOpWrite = true;
                        break;
                    }

                    //如果发送的字节数量>0则计数
                    flushedAmount += localFlushedAmount;
                    if (!buf.isReadable()) {
                        /**
                         * 判断当前消息是否已经发送成功（缓冲区没有可读字节），
                         * 如果发送成功则设置done为true,退出循环
                         */
                        done = true;
                        break;
                    }
                }

                //消息发送完成之后，更新发送进度信息
                in.progress(flushedAmount);
                if (done) {
                    in.remove();
                } else {
                    // Break the loop and so incompleteWrite(...) is called.
                    break;
                }
            } else if (msg instanceof FileRegion) {
                FileRegion region = (FileRegion) msg;
                boolean done = region.transferred() >= region.count();
                if (!done) {
                    long flushedAmount = 0;
                    if (writeSpinCount == -1) {
                        writeSpinCount = config().getWriteSpinCount();
                    }
                    for (int i = writeSpinCount - 1; i >= 0; i--) {
                        long localFlushedAmount = doWriteFileRegion(region);
                        if (localFlushedAmount == 0) {
                            setOpWrite = true;
                            break;
                        }
                        flushedAmount += localFlushedAmount;
                        if (region.transferred() >= region.count()) {
                            done = true;
                            break;
                        }
                    }
                    in.progress(flushedAmount);
                }
                if (done) {
                    in.remove();
                } else {
                    // Break the loop and so incompleteWrite(...) is called.
                    break;
                }
            } else {
                // Should not reach here.
                throw new Error();
            }
        }
        incompleteWrite(setOpWrite);
    }

    @Override
    protected final Object filterOutboundMessage(Object msg) {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            if (buf.isDirect()) {
                return msg;
            }

            return newDirectBuffer(buf);
        }

        if (msg instanceof FileRegion) {
            return msg;
        }

        throw new UnsupportedOperationException(
                "unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
    }

    /**
     * 写还没有完成，说明写了半个包
     * 启动写半包任务线程用于后续继续发送半包消息
     *
     * @param setOpWrite
     */
    protected final void incompleteWrite(boolean setOpWrite) {
        // Did not write completely.
        if (setOpWrite) {
            setOpWrite();
        } else {
            // Schedule flush again later so other tasks can be picked up in the meantime
            Runnable flushTask = this.flushTask;
            if (flushTask == null) {
                flushTask = this.flushTask = new Runnable() {
                    @Override
                    public void run() {
                        flush();
                    }
                };
            }
            eventLoop().execute(flushTask);
        }
    }

    /**
     * Write a {@link FileRegion}
     *
     * @param region the {@link FileRegion} from which the bytes should be written
     * @return amount       the amount of written bytes
     */
    protected abstract long doWriteFileRegion(FileRegion region) throws Exception;

    /**
     * Read bytes into the given {@link ByteBuf} and return the amount.
     */
    protected abstract int doReadBytes(ByteBuf buf) throws Exception;

    /**
     * Write bytes form the given {@link ByteBuf} to the underlying {@link java.nio.channels.Channel}.
     *
     * @param buf the {@link ByteBuf} from which the bytes should be written
     * @return amount       the amount of written bytes
     */
    protected abstract int doWriteBytes(ByteBuf buf) throws Exception;

    protected final void setOpWrite() {
        final SelectionKey key = selectionKey();
        // Check first if the key is still valid as it may be canceled as part of the deregistration
        // from the EventLoop
        // See https://github.com/netty/netty/issues/2104
        if (!key.isValid()) {
            return;
        }
        final int interestOps = key.interestOps();
        if ((interestOps & SelectionKey.OP_WRITE) == 0) {

            //如果没有关注写操作位，就把写操作位设置进去
            key.interestOps(interestOps | SelectionKey.OP_WRITE);
        }
    }

    protected final void clearOpWrite() {
        final SelectionKey key = selectionKey();
        // Check first if the key is still valid as it may be canceled as part of the deregistration
        // from the EventLoop
        // See https://github.com/netty/netty/issues/2104
        if (!key.isValid()) {
            return;
        }
        //清除半包消息

        //从当前 SelectionKey 中获取网络操作位
        final int interestOps = key.interestOps();
        if ((interestOps & SelectionKey.OP_WRITE) != 0) {
            //如果进入该 if 代码块，则说明当前是 可写的，需要清除写操作位

            //清除写操作位
            key.interestOps(interestOps & ~SelectionKey.OP_WRITE);
        }
    }
}
