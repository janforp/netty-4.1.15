package com.netty.qwzn.codes.customerprotocal.client;

import com.netty.qwzn.codes.customerprotocal.Header;
import com.netty.qwzn.codes.customerprotocal.MessageType;
import com.netty.qwzn.codes.customerprotocal.NettyMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author Lilinfeng
 * @version 1.0
 * @date 2014年3月15日
 */
public class LoginAuthReqHandler extends SimpleChannelInboundHandler<NettyMessage> {

    /**
     * Calls {@link ChannelHandlerContext#fireChannelActive()} to forward to the
     * next {@link ChannelHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(buildLoginReq());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NettyMessage message) throws Exception {

        // 如果是握手应答消息，需要判断是否认证成功
        if (message.getHeader() != null && message.getHeader().getType() == MessageType.LOGIN_RESP.value()) {
            byte loginResult = (byte) message.getBody();
            if (loginResult != (byte) 0) {
                // 握手失败，关闭连接
                ctx.close();
            } else {
                System.out.println("Login is ok : " + message);
                //登陆成功之后该消息继续往下传递
                ctx.fireChannelRead(message);
            }
        } else {
            ctx.fireChannelRead(message);
        }
    }

    private NettyMessage buildLoginReq() {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(MessageType.LOGIN_REQ.value());
        message.setHeader(header);
        return message;
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.fireExceptionCaught(cause);
    }
}
