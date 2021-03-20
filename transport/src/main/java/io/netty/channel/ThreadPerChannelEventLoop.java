package io.netty.channel;

/**
 * {@link SingleThreadEventLoop} which is used to handle OIO {@link Channel}'s. So in general there will be
 * one {@link ThreadPerChannelEventLoop} per {@link Channel}.
 */
public class ThreadPerChannelEventLoop extends SingleThreadEventLoop {

    private final ThreadPerChannelEventLoopGroup parent;

    private Channel ch;

    public ThreadPerChannelEventLoop(ThreadPerChannelEventLoopGroup parent) {
        super(parent, parent.executor, true);
        this.parent = parent;
    }

    @Override
    public ChannelFuture register(ChannelPromise promise) {
        return super.register(promise).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    ch = future.channel();
                } else {
                    deregister();
                }
            }
        });
    }

    @Deprecated
    @Override
    public ChannelFuture register(Channel channel, ChannelPromise promise) {
        return super.register(channel, promise).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    ch = future.channel();
                } else {
                    deregister();
                }
            }
        });
    }

    @Override
    protected void run() {
        for (; ; ) {
            Runnable task = takeTask();
            if (task != null) {
                task.run();
                updateLastExecutionTime();
            }

            Channel ch = this.ch;
            if (isShuttingDown()) {
                if (ch != null) {
                    ch.unsafe().close(ch.unsafe().voidPromise());
                }
                if (confirmShutdown()) {
                    break;
                }
            } else {
                if (ch != null) {
                    // Handle deregistration
                    if (!ch.isRegistered()) {
                        runAllTasks();
                        deregister();
                    }
                }
            }
        }
    }

    protected void deregister() {
        ch = null;
        parent.activeChildren.remove(this);
        parent.idleChildren.add(this);
    }
}
