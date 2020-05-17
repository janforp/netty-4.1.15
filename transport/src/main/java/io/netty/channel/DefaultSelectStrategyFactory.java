package io.netty.channel;

/**
 * 使用了抽象工厂模式
 *
 * 每一个抽象工厂的实例都会生成一种类型的产品
 * <p></p>
 * Factory which uses the default select strategy.
 */
public final class DefaultSelectStrategyFactory implements SelectStrategyFactory {

    public static final SelectStrategyFactory INSTANCE = new DefaultSelectStrategyFactory();

    private DefaultSelectStrategyFactory() {
    }

    @Override
    public SelectStrategy newSelectStrategy() {
        return DefaultSelectStrategy.INSTANCE;
    }
}
