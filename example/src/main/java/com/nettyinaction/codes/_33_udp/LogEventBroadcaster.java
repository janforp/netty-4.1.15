package com.nettyinaction.codes._33_udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;

/**
 * LogEventBroadcaster
 *
 * @author zhucj
 * @since 20210325
 */
public class LogEventBroadcaster {

    private final EventLoopGroup group;

    private final Bootstrap bootstrap;

    private final File file;

    public LogEventBroadcaster(InetSocketAddress address, File file) {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new LogEventEncoder(address));
        this.file = file;
    }

    public void run() throws Exception {
        Channel ch = bootstrap.bind(0).sync().channel();
        long pointer = 0;
        for (; ; ) {//启动主处理循环
            long len = file.length();
            if (len < pointer) {
                pointer = len;//如果有毕业，将文件的指针设置到该文件的最后一个字节
            } else if (len > pointer) {
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                raf.seek(pointer);//设置当前文件的指针，以确保没有任何的日志被发送
                String line;
                while ((line = raf.readLine()) != null) {
                    //
                    ch.writeAndFlush(new LogEvent(null, -1, file.getAbsolutePath(), line));
                }
                pointer = raf.getFilePointer();
                raf.close();
            }
        }
    }
}
