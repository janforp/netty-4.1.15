package io.netty.util.concurrent;

import java.util.EventListener;

/**
 * Listens to the result of a {@link Future}.  The result of the asynchronous operation is notified once this listener
 * is added by calling {@link Future#addListener(GenericFutureListener)}.
 *
 * 这个监听器可以被添加到 一个异步操作的结果中-Future，在一些操作完成的时候可以执行回调到 operationComplete 方法
 */
public interface GenericFutureListener<F extends Future<?>> extends EventListener {

    /**
     * Invoked when the operation associated with the {@link Future} has been completed.
     *
     * 该方法在：与Future相关的操作完成时调用。
     *
     * @param future the source {@link Future} which called this callback （调用此回调的源Future）
     */
    void operationComplete(F future) throws Exception;
}
