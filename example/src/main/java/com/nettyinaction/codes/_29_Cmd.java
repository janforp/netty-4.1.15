package com.nettyinaction.codes;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.LineBasedFrameDecoder;

/**
 * 作为示例，我们将使用下面的协议 规范:
 * 传入数据流是一系列的帧，每个帧都由换行符(\n)分隔;
 * 每个帧都由一系列的元素组成，每个元素都由单个空格字符分隔;
 * 一个帧的内容代表一个命令，定义为一个命令名称后跟着数目可变的参数。 我们用于这个协议的自定义解码器将定义以下类:
 * Cmd— 将帧(命令)的内容存储在ByteBuf中，一个ByteBuf用于名称，另一个 用于参数;
 * CmdDecoder— 从被重写了的 decode()方法中获取一行字符串，并从它的内容构建 一个 Cmd 的实例;
 * CmdHandler— 从CmdDecoder获取解码的Cmd对象，并对它进行一些处理;
 * CmdHandlerInitializer— 为了简便起见，我们将会把前面的这些类定义为专门 的 ChannelInitializer 的嵌套类，其将会把这些 ChannelInboundHandler 安装
 * 到 ChannelPipeline 中。
 *
 * @author zhucj
 * @since 20210325
 */
@SuppressWarnings("all")
public class _29_Cmd {

    /**
     * _29_Cmd:这是一个接近真实开发的例子！！！！
     */

    static final byte SPACE = (byte) ' ';

    static final class CmdHandlerInitializer extends ChannelInitializer<Channel> {

        @Override
        protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();

            /**
             *  添加 CmdDecoder 以提取 Cmd 对象，并将它转发给下 一个 ChannelInboundHandler
             */
            pipeline.addLast(new CmdDecoder(60 * 1024));

            /**
             *  添加 CmdHandler 以接收 和处理 Cmd 对象
             */
            pipeline.addLast(new CmdHandler());

            /**
             * 1.远端建立了一个连接，此处创建该连接
             * 2.远程连接传过来一些命令以及参数
             * 3.CmdDecoder 会处理字节，并且解码成一个 Cmd 对象，然后传给下一个处理器
             * 4.下一个处理器正好是 CmdHandler，这个处理器就会针对每个cmd对象进行自己的处理！！！！
             */
        }
    }

    static final class Cmd {

        private final ByteBuf name;

        private final ByteBuf args;

        public Cmd(ByteBuf name, ByteBuf args) {
            this.name = name;
            this.args = args;
        }

        public ByteBuf args() {
            return args;
        }

        public ByteBuf name() {
            return name;
        }
    }

    /**
     * CmdDecoder 会处理字节，并且解码成一个 Cmd 对象，然后传给下一个处理器
     */
    static final class CmdDecoder extends LineBasedFrameDecoder {

        public CmdDecoder(int maxLength) {
            super(maxLength);
        }

        @Override
        protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
            ByteBuf frame = (ByteBuf) super.decode(ctx, buffer);
            //从 ByteBuf 中提取由 行尾符序列分隔的帧
            if (frame == null) {
                //如果输入中没有 帧，则返回 null
                return null;
            }

            /**
             * 查找第一个空格字符的索引。 前面是命令名称，接着是参数
             */
            int index = frame.indexOf(frame.readerIndex(), frame.writerIndex(), SPACE);

            /**
             * 使用包含有命令名 称和参数的切片创 建新的 Cmd 对象
             */
            return new Cmd(
                    frame.slice(frame.readerIndex(), index), // ByteBuf name
                    frame.slice(index + 1, frame.writerIndex()) // ByteBuf args
            );
        }
    }

    /**
     * 下一个处理器正好是 CmdHandler，这个处理器就会针对每个cmd对象进行自己的处理！！！！
     */
    static final class CmdHandler extends SimpleChannelInboundHandler<Cmd> {

        /**
         * 处理传经 ChannelPipeline 的 Cmd 对象
         */
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Cmd msg) throws Exception {
            //针对该命令做一些事情
            //处理传经 ChannelPipeline 的 Cmd 对象
            System.out.println(msg.name + " : " + msg.args);
        }
    }
}