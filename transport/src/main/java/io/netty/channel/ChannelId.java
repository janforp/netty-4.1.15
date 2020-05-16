package io.netty.channel;

import java.io.Serializable;

/**
 * Represents the globally unique identifier of a {@link Channel}.
 * <p>
 * The identifier is generated from various sources listed in the following:
 * <ul>
 * <li>MAC address (EUI-48 or EUI-64) or the network adapter, preferably a globally unique one,</li>
 * <li>the current process ID,</li>
 * <li>{@link System#currentTimeMillis()},</li>
 * <li>{@link System#nanoTime()},</li>
 * <li>a random 32-bit integer, and</li>
 * <li>a sequentially incremented 32-bit integer.</li>
 * </ul>
 * </p>
 * <p>
 * The global uniqueness of the generated identifier mostly depends on the MAC address and the current process ID,
 * which are auto-detected at the class-loading time in best-effort manner.  If all attempts to acquire them fail,
 * a warning message is logged, and random values will be used instead.  Alternatively, you can specify them manually
 * via system properties:
 * <ul>
 * <li>{@code io.netty.machineId} - hexadecimal representation of 48 (or 64) bit integer,
 *     optionally separated by colon or hyphen.</li>
 * <li>{@code io.netty.processId} - an integer between 0 and 65535</li>
 * </ul>
 * </p>
 */
public interface ChannelId extends Serializable, Comparable<ChannelId> {

    /**
     * Returns the short but globally non-unique string representation of the {@link ChannelId}.
     */
    String asShortText();

    /**
     * Returns the long yet globally unique string representation of the {@link ChannelId}.
     */
    String asLongText();
}
