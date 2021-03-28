package io.netty.channel.nio;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link AbstractNioChannel} base class for {@link Channel}s that operate on messages.
 */
public abstract class AbstractNioMessageChannel extends AbstractNioChannel {

    boolean inputShutdown;

    /**
     * @see NioServerSocketChannel#NioServerSocketChannel(java.nio.channels.ServerSocketChannel) 通过该方法调用
     * @see AbstractNioChannel#AbstractNioChannel(Channel, SelectableChannel, int)
     */
    protected AbstractNioMessageChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        super(parent, ch, readInterestOp);
    }

    @Override
    protected AbstractNioUnsafe newUnsafe() {
        return new NioMessageUnsafe();
    }

    @Override
    protected void doBeginRead() throws Exception {
        if (inputShutdown) {
            return;
        }
        super.doBeginRead();
    }

    /**
     * NioEventLoopGroup就是使用该类型的实例
     */
    private final class NioMessageUnsafe extends AbstractNioUnsafe {

        private final List<Object> readBuf = new ArrayList<>();

        @Override
        public void read() {
            assert eventLoop().inEventLoop();
            final ChannelConfig config = config();
            final ChannelPipeline pipeline = pipeline();
            final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
            allocHandle.reset(config);

            boolean closed = false;
            Throwable exception = null;
            try {
                try {
                    do {
                        int localRead = doReadMessages(readBuf);
                        if (localRead == 0) {
                            break;
                        }
                        if (localRead < 0) {
                            closed = true;
                            break;
                        }

                        allocHandle.incMessagesRead(localRead);
                    } while (allocHandle.continueReading());
                } catch (Throwable t) {
                    exception = t;
                }

                int size = readBuf.size();
                for (int i = 0; i < size; i++) {
                    readPending = false;
                    pipeline.fireChannelRead(readBuf.get(i));
                }
                readBuf.clear();
                allocHandle.readComplete();
                pipeline.fireChannelReadComplete();

                if (exception != null) {
                    closed = closeOnReadError(exception);

                    pipeline.fireExceptionCaught(exception);
                }

                if (closed) {
                    inputShutdown = true;
                    if (isOpen()) {
                        close(voidPromise());
                    }
                }
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

    public static void main(String[] args) {
        System.out.println(Integer.toBinaryString(SelectionKey.OP_WRITE));
        System.out.println(Integer.toBinaryString(SelectionKey.OP_ACCEPT));
        System.out.println(Integer.toBinaryString(SelectionKey.OP_CONNECT));
        System.out.println(Integer.toBinaryString(SelectionKey.OP_READ));
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {

        //拿到当前Channel注册之后的selectionKey
        final SelectionKey key = selectionKey();

        //获取当前的网络事件
        final int interestOps = key.interestOps();
        for (; ; ) {

            //获取一条消息
            Object msg = in.current();
            if (msg == null) {
                // Wrote all messages.
                if ((interestOps & SelectionKey.OP_WRITE) != 0) {
                    /**
                     * 条件成立说明：当前感兴趣的事件中包括写事件
                     *
                     * 于是取消对写事件的关注
                     *
                     *   SelectionKey.OP_WRITE      0       ...                 0100
                     *  ~SelectionKey.OP_WRITE      11111111111111111111111111111011
                     *
                     *  如果当前感兴趣的事件是
                     *   OP_ACCEPT                  0       ...                10000
                     *   OP_CONNECT                 0       ...                01000
                     *   OP_READ                    0       ...                00001
                     *
                     *   他们4个的二进制字符串上只有一个1，并且位置不一样，所有，位运算很方便
                     *
                     *   经过运算之后，该key感兴趣的事件不会包括写事件
                     */
                    key.interestOps(interestOps & ~SelectionKey.OP_WRITE);
                }
                break;
            }
            try {
                boolean done = false;
                for (int i = config().getWriteSpinCount() - 1; i >= 0; i--) {
                    /**
                     * 模版方法
                     */
                    if (doWriteMessage(msg, in)) {
                        done = true;
                        break;
                    }
                }

                if (done) {

                    //如果当前消息被完全发送出去，将该消息移除
                    in.remove();
                } else {
                    //否则就说明还没有完全发送完毕，还有一半的包没有发送

                    // Did not write all messages.
                    if ((interestOps & SelectionKey.OP_WRITE) == 0) {
                        /**
                         * (interestOps & SelectionKey.OP_WRITE) == 0 说明，当前的 interestOps 中肯定不包括 写事件
                         *
                         * 所以需要重新注册感兴趣的写事件
                         */
                        key.interestOps(interestOps | SelectionKey.OP_WRITE);
                    }
                    break;
                }
            } catch (IOException e) {
                if (continueOnWriteError()) {
                    in.remove(e);
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * Returns {@code true} if we should continue the write loop on a write error.
     */
    protected boolean continueOnWriteError() {
        return false;
    }

    protected boolean closeOnReadError(Throwable cause) {
        // ServerChannel should not be closed even on IOException because it can often continue
        // accepting incoming connections. (e.g. too many open files)
        return cause instanceof IOException &&
                !(cause instanceof PortUnreachableException) &&
                !(this instanceof ServerChannel);
    }

    /**
     * Read messages into the given array and return the amount which was read.
     */
    protected abstract int doReadMessages(List<Object> buf) throws Exception;

    /**
     * Write a message to the underlying {@link java.nio.channels.Channel}.
     * -- 将消息写到基础{@link java.nio.channels.Channel}。
     *
     * @return {@code true} if and only if the message has been written -- 当且仅当消息已被写入
     */
    protected abstract boolean doWriteMessage(Object msg, ChannelOutboundBuffer in) throws Exception;
}
