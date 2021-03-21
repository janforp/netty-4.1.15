package io.netty.handler.ssl;

import io.netty.buffer.ByteBufAllocator;

import javax.net.ssl.SSLEngine;

/**
 * Implements a {@link SSLEngine} using
 * <a href="https://www.openssl.org/docs/crypto/BIO_s_bio.html#EXAMPLE">OpenSSL BIO abstractions</a>.
 * <p>
 * This class will use a finalizer to ensure native resources are automatically cleaned up. To avoid finalizers
 * and manually release the native memory see {@link ReferenceCountedOpenSslEngine}.
 */
public final class OpenSslEngine extends ReferenceCountedOpenSslEngine {

    /**
     * Netty 的 OpenSSL/SSLEngine 实现
     * Netty 还提供了使用 OpenSSL 工具包(www.openssl.org)的 SSLEngine 实现。这个 OpenSsl-
     * Engine 类提供了比 JDK 提供的 SSLEngine 实现更好的性能。
     * 如果 OpenSSL 库可用，可以将 Netty 应用程序(客户端和服务器)配置为默认使用 OpenSslEngine。
     * 如果不可用，Netty 将会回退到 JDK 实现。有关配置 OpenSSL 支持的详细说明，
     * 参见 Netty 文档: http://netty.io/wiki/forked-tomcat-native.html#wikih2-1。
     * 注意，无论你使用 JDK 的 SSLEngine 还是使用 Netty 的 OpenSslEngine，SSL API 和数据流都 是一致的。
     */

    OpenSslEngine(OpenSslContext context, ByteBufAllocator alloc, String peerHost, int peerPort,
            boolean jdkCompatibilityMode) {
        super(context, alloc, peerHost, peerPort, jdkCompatibilityMode, false);
    }

    @Override
    @SuppressWarnings("FinalizeDeclaration")
    protected void finalize() throws Throwable {
        super.finalize();
        OpenSsl.releaseIfNeeded(this);
    }
}