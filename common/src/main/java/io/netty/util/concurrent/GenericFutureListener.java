package io.netty.util.concurrent;

import java.util.EventListener;

/**
 * Listens to the result of a {@link Future}.  The result of the asynchronous operation is notified once this listener
 * is added by calling {@link Future#addListener(GenericFutureListener)}.
 *
 * 坚挺 Future 的结果。通过调用Future.addListener（GenericFutureListener）添加此侦听器后，便会通知异步操作的结果。
 *
 * 这个监听器可以被添加到 一个异步操作的结果中-Future，在一些操作完成的时候可以执行回调到 operationComplete 方法
 */
public interface GenericFutureListener<F extends Future<?>> extends EventListener {

    /**
     * Invoked when the operation associated with the {@link Future} has been completed.
     *
     * 该方法在：与Future相关的操作完成时调用。
     *
     * @param future the source {@link Future} which called this callback （调用此回调的源Future）观察者模式中的主题模式，把主题对象（future）传递给了观察者
     *
     * 通过该 future 可以拿到 Channel 对象
     */
    void operationComplete(F future) throws Exception;
}
