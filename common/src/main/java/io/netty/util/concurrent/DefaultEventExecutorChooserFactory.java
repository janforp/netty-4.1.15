package io.netty.util.concurrent;

import io.netty.util.internal.UnstableApi;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation which uses simple round-robin to choose next {@link EventExecutor}.
 *
 * 创建新的EventExecutorChooserFactory.EventExecutorChoosers的工厂。
 * 每一个工厂实现中都会实现 EventExecutorChooser 接口，然后返回该类型的 EventExecutorChooser
 * 抽象工厂方法设计模式
 *
 * 该工厂本身又是单例模式
 */
@UnstableApi
public final class DefaultEventExecutorChooserFactory implements EventExecutorChooserFactory {

    public static final DefaultEventExecutorChooserFactory INSTANCE = new DefaultEventExecutorChooserFactory();

    private DefaultEventExecutorChooserFactory() {
    }

    @Override
    public EventExecutorChooser newChooser(EventExecutor[] executors) {
        //是不是2个幂数，选择不通的选择器
        //目的是为了提供性能
        if (isPowerOfTwo(executors.length)) {
            return new PowerOfTwoEventExecutorChooser(executors);
        } else {
            return new GenericEventExecutorChooser(executors);
        }
    }

    /**
     * val 是不是2的次方
     *
     * @param val
     * @return
     */
    private static boolean isPowerOfTwo(int val) {
        /**
         * 假设 val = 16
         * 则16的二进制：0001 0000
         * - 16的二进制：1111 0000
         * 则二者进行& ：0001 0000
         *
         * 反例。val = 15
         * 则15的二进制：0000 1111
         * - 15的二进制：1111 0001
         * 则二者进行& ：0000 0001
         */
        return (val & -val) == val;
    }

    private static final class PowerOfTwoEventExecutorChooser implements EventExecutorChooser {

        private final AtomicInteger idx = new AtomicInteger();

        private final EventExecutor[] executors;

        PowerOfTwoEventExecutorChooser(EventExecutor[] executors) {
            this.executors = executors;
        }

        @Override
        public EventExecutor next() {
            /**
             * idx.getAndIncrement() & executors.length - 1 该算法相当于取模,性能好点
             */
            return executors[idx.getAndIncrement() & executors.length - 1];
        }
    }

    private static final class GenericEventExecutorChooser implements EventExecutorChooser {

        private final AtomicInteger idx = new AtomicInteger();

        private final EventExecutor[] executors;

        GenericEventExecutorChooser(EventExecutor[] executors) {
            this.executors = executors;
        }

        @Override
        public EventExecutor next() {
            /**
             * Math.abs(idx.getAndIncrement() % executors.length) 该算法直接取模,性能差点
             */
            return executors[Math.abs(idx.getAndIncrement() % executors.length)];
        }
    }
}
