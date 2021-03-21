package io.netty.handler.codec.http;


/**
 * An interface that defines a HTTP message, providing common properties for
 * {@link HttpRequest} and {@link HttpResponse}.
 *
 * @see HttpResponse
 * @see HttpRequest
 * @see HttpHeaders
 */
public interface HttpMessage extends HttpObject {

    /**
     * @deprecated Use {@link #protocolVersion()} instead.
     */
    @Deprecated
    HttpVersion getProtocolVersion();

    /**
     * Returns the protocol version of this {@link HttpMessage}
     */
    HttpVersion protocolVersion();

    /**
     * Set the protocol version of this {@link HttpMessage}
     */
    HttpMessage setProtocolVersion(HttpVersion version);

    /**
     * Returns the headers of this message.
     */
    HttpHeaders headers();
}
