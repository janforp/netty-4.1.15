package io.netty.channel;

/**
 * Factory that creates a new {@link SelectStrategy} every time.
 */
public interface SelectStrategyFactory {

    /**
     * Creates a new {@link SelectStrategy}.
     */
    SelectStrategy newSelectStrategy();
}
