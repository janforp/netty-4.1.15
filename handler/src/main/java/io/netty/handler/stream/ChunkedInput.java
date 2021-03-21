package io.netty.handler.stream;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;

/**
 * A data stream of indefinite length which is consumed by {@link ChunkedWriteHandler}.
 *
 * Chunked：块状
 *
 * 其中类型参数 B 是 readChunk()方法返回的 类型。
 */
public interface ChunkedInput<B> {

    /**
     * ChunkedFile              -- 从文件中逐块获取数据，当你的平台不支持零拷贝或者你需要转换数据的时候使用
     * ChunkedNioFile           -- 和 ChunkedFile 类似，只是它使用了 FileChannel
     * ChunkedStream            -- 从 InputStream 中逐块传输内容
     * ChunkedNioStream         -- 从ReadableByteChannel中逐块传输内容
     */

    /**
     * Return {@code true} if and only if there is no data left in the stream
     * and the stream has reached at its end.
     */
    boolean isEndOfInput() throws Exception;

    /**
     * Releases the resources associated with the input.
     */
    void close() throws Exception;

    /**
     * @param ctx The context which provides a {@link ByteBufAllocator} if buffer allocation is necessary.
     * @return the fetched chunk.
     * {@code null} if there is no data left in the stream.
     * Please note that {@code null} does not necessarily mean that the
     * stream has reached at its end.  In a slow stream, the next chunk
     * might be unavailable just momentarily.
     * @deprecated Use {@link #readChunk(ByteBufAllocator)}.
     *
     * <p>Fetches a chunked data from the stream. Once this method returns the last chunk
     * and thus the stream has reached at its end, any subsequent {@link #isEndOfInput()}
     * call must return {@code true}.
     */
    @Deprecated
    B readChunk(ChannelHandlerContext ctx) throws Exception;

    /**
     * Fetches a chunked data from the stream. Once this method returns the last chunk
     * and thus the stream has reached at its end, any subsequent {@link #isEndOfInput()}
     * call must return {@code true}.
     *
     * @param allocator {@link ByteBufAllocator} if buffer allocation is necessary.
     * @return the fetched chunk.
     * {@code null} if there is no data left in the stream.
     * Please note that {@code null} does not necessarily mean that the
     * stream has reached at its end.  In a slow stream, the next chunk
     * might be unavailable just momentarily.
     */
    B readChunk(ByteBufAllocator allocator) throws Exception;

    /**
     * Returns the length of the input.
     *
     * @return the length of the input if the length of the input is known.
     * a negative value if the length of the input is unknown.
     */
    long length();

    /**
     * Returns current transfer progress.
     */
    long progress();
}