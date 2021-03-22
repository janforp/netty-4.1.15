package com.nettyinaction.codes._32_chat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;

/**
 * SecureChatServer
 *
 * @author zhucj
 * @since 20210325
 */
public class SecureChatServer extends ChatServer {

    private final SslContext context;

    public SecureChatServer(SslContext context) {
        this.context = context;
    }

    @Override
    public ChannelInitializer<Channel> createInitializer(ChannelGroup channelGroup) {
        return new SecureChatServerInitializer(channelGroup, context);
    }

    public static void main(String[] args) throws CertificateException, SSLException {
        int port = 8888;

        SelfSignedCertificate certificate = new SelfSignedCertificate();
        SslContext context = SslContext.newServerContext(certificate.certificate(), certificate.privateKey());

        SecureChatServer endPoint = new SecureChatServer(context);
        ChannelFuture future = endPoint.start(new InetSocketAddress(port));

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                endPoint.destroy();
            }
        });

        future.channel().closeFuture().syncUninterruptibly();
    }
}
