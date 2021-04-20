package io.netty.channel;

import io.netty.util.IntSupplier;

/**
 * Default select strategy.
 */
final class DefaultSelectStrategy implements SelectStrategy {

    static final SelectStrategy INSTANCE = new DefaultSelectStrategy();

    private DefaultSelectStrategy() {
    }

    /**
     * * private final IntSupplier selectNowSupplier = new IntSupplier() {
     * *         @Override
     * *         public int get() throws Exception {
     * *             return selectNow();
     * *         }
     * *     };
     *
     * @param selectSupplier 多路复用器selectNow()方法返回
     * @param hasTasks 如果正在等待处理任务，则为true。
     * @return
     * @throws Exception
     */
    @Override
    public int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception {
        return hasTasks ?
                selectSupplier.get() // 如果有任务，则执行多路复用器进行 Select
                : SelectStrategy.SELECT; //否则就返回 SELECT（-1）
    }
}
