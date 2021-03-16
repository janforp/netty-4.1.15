package com.nettyinaction.codes;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

/**
 * 总之，如果一个消息被消费或者丢弃了，并且没有传递给 ChannelPipeline 中的下一个 ChannelOutboundHandler，
 * 那么用户就有责任调用 ReferenceCountUtil.release()。
 * 如果消息到达了实际的传输层，那么当它被写入时或者 Channel 关闭时，都将被自动释放。
 *
 * @author zhucj
 * @since 20210325
 */
public class _15_DiscardOutboundHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        //释放资源
        ReferenceCountUtil.release(msg);

        /**
         * 重要的是，不仅要释放资源，还要通知 ChannelPromise。否则可能会出现 Channel- FutureListener 收不到某个消息已经被处理了的通知的情况。
         */
        //通知他数据已经处理成功了
        promise.setSuccess();
    }
}
