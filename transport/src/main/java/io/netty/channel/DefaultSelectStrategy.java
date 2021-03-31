package io.netty.channel;

import io.netty.util.IntSupplier;

/**
 * Default select strategy.
 */
final class DefaultSelectStrategy implements SelectStrategy {

    static final SelectStrategy INSTANCE = new DefaultSelectStrategy();

    private DefaultSelectStrategy() {
    }

    @Override
    public int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception {
        return hasTasks ?
                selectSupplier.get() // 如果有任务，则执行多路复用器进行 Select
                : SelectStrategy.SELECT; //否则就返回 SELECT
    }
}
