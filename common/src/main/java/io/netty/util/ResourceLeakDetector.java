package io.netty.util;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentMap;

import static io.netty.util.internal.StringUtil.EMPTY_STRING;
import static io.netty.util.internal.StringUtil.NEWLINE;
import static io.netty.util.internal.StringUtil.simpleClassName;

/**
 * 每当通过调用 ChannelInboundHandler.channelRead()
 * 或者 ChannelOutbound- Handler.write()方法来处理数据时，
 * 你都需要确保没有任何的资源泄漏。你可能还记得在前
 * 面的章节中所提到的，Netty 使用引用计数来处理池化的 ByteBuf。
 * 所以在完全使用完某个 ByteBuf 后，调整其引用计数是很重要的。
 * 为了帮助你诊断潜在的(资源泄漏)问题，Netty提供了class ResourceLeakDetector，
 * 它将对你应用程序的缓冲区分配做大约 1%的采样来检测内存泄露。相关的开销是非常小的。
 *
 * @param <T>
 */
public class ResourceLeakDetector<T> {

    /**
     * 泄露检测级别可以通过将下面的 Java 系统属性设置为表中的一个值来定义:
     * java -Dio.netty.leakDetectionLevel=ADVANCED
     */
    private static final String PROP_LEVEL_OLD = "io.netty.leakDetectionLevel";

    private static final String PROP_LEVEL = "io.netty.leakDetection.level";

    private static final Level DEFAULT_LEVEL = Level.SIMPLE;

    private static final String PROP_MAX_RECORDS = "io.netty.leakDetection.maxRecords";

    private static final int DEFAULT_MAX_RECORDS = 4;

    private static final int MAX_RECORDS;

    /**
     * Represents the level of resource leak detection.
     *
     * Netty 目前定义了 4 种泄漏检测级别
     */
    public enum Level {
        /**
         * Disables resource leak detection.
         *
         * 禁用泄漏检测。只有在详尽的测试之后才应设置为这个值
         */
        DISABLED,
        /**
         * Enables simplistic sampling resource leak detection which reports there is a leak or not,
         * at the cost of small overhead (default).
         *
         * 使用 1%的默认采样率检测并报告任何发现的泄露。这是默认级别，适合绝大部分的情况
         */
        SIMPLE,
        /**
         * Enables advanced sampling resource leak detection which reports where the leaked object was accessed
         * recently at the cost of high overhead.
         *
         * 使用默认的采样率，报告所发现的任何的泄露以及对应的消息被访问的位置
         */
        ADVANCED,
        /**
         * Enables paranoid resource leak detection which reports where the leaked object was accessed recently,
         * at the cost of the highest possible overhead (for testing purposes only).
         *
         * 类似于ADVANCED，但是其将会对每次(对消息的)访问都进行采样。这对性能将会有很 大的影响，应该只在调试阶段使用
         *
         * PARANOID：泛醇类
         */
        PARANOID;

        /**
         * Returns level based on string value. Accepts also string that represents ordinal number of enum.
         *
         * @param levelStr - level string : DISABLED, SIMPLE, ADVANCED, PARANOID. Ignores case.
         * @return corresponding level or SIMPLE level in case of no match.
         */
        static Level parseLevel(String levelStr) {
            String trimmedLevelStr = levelStr.trim();
            for (Level l : values()) {
                if (trimmedLevelStr.equalsIgnoreCase(l.name()) || trimmedLevelStr.equals(String.valueOf(l.ordinal()))) {
                    return l;
                }
            }
            /**
             * @see ResourceLeakDetector#DEFAULT_LEVEL
             */
            return DEFAULT_LEVEL;
        }
    }

    private static Level level;

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ResourceLeakDetector.class);

    static {
        final boolean disabled;
        if (SystemPropertyUtil.get("io.netty.noResourceLeakDetection") != null) {
            disabled = SystemPropertyUtil.getBoolean("io.netty.noResourceLeakDetection", false);
            logger.debug("-Dio.netty.noResourceLeakDetection: {}", disabled);
            logger.warn(
                    "-Dio.netty.noResourceLeakDetection is deprecated. Use '-D{}={}' instead.",
                    PROP_LEVEL, DEFAULT_LEVEL.name().toLowerCase());
        } else {
            disabled = false;
        }

        Level defaultLevel = disabled ? Level.DISABLED : DEFAULT_LEVEL;

        /**
         * 泄露检测级别可以通过将下面的 Java 系统属性设置为表中的一个值来定义:
         * java -Dio.netty.leakDetectionLevel=ADVANCED
         */
        // First read old property name
        String levelStr = SystemPropertyUtil.get(PROP_LEVEL_OLD, defaultLevel.name());

        // If new property name is present, use it
        levelStr = SystemPropertyUtil.get(PROP_LEVEL, levelStr);
        Level level = Level.parseLevel(levelStr);

        MAX_RECORDS = SystemPropertyUtil.getInt(PROP_MAX_RECORDS, DEFAULT_MAX_RECORDS);

        ResourceLeakDetector.level = level;
        if (logger.isDebugEnabled()) {
            logger.debug("-D{}: {}", PROP_LEVEL, level.name().toLowerCase());
            logger.debug("-D{}: {}", PROP_MAX_RECORDS, MAX_RECORDS);
        }
    }

    // Should be power of two.
    static final int DEFAULT_SAMPLING_INTERVAL = 128;

    /**
     * @deprecated Use {@link #setLevel(Level)} instead.
     */
    @Deprecated
    public static void setEnabled(boolean enabled) {
        setLevel(enabled ? Level.SIMPLE : Level.DISABLED);
    }

    /**
     * Returns {@code true} if resource leak detection is enabled.
     */
    public static boolean isEnabled() {
        return getLevel().ordinal() > Level.DISABLED.ordinal();
    }

    /**
     * Sets the resource leak detection level.
     */
    public static void setLevel(Level level) {
        if (level == null) {
            throw new NullPointerException("level");
        }
        ResourceLeakDetector.level = level;
    }

    /**
     * Returns the current resource leak detection level.
     */
    public static Level getLevel() {
        return level;
    }

    /**
     * the collection of active resources
     */
    private final ConcurrentMap<DefaultResourceLeak, LeakEntry> allLeaks = PlatformDependent.newConcurrentHashMap();

    private final ReferenceQueue<Object> refQueue = new ReferenceQueue<Object>();

    private final ConcurrentMap<String, Boolean> reportedLeaks = PlatformDependent.newConcurrentHashMap();

    private final String resourceType;

    private final int samplingInterval;

    /**
     * @deprecated use {@link ResourceLeakDetectorFactory#newResourceLeakDetector(Class, int, long)}.
     */
    @Deprecated
    public ResourceLeakDetector(Class<?> resourceType) {
        this(simpleClassName(resourceType));
    }

    /**
     * @deprecated use {@link ResourceLeakDetectorFactory#newResourceLeakDetector(Class, int, long)}.
     */
    @Deprecated
    public ResourceLeakDetector(String resourceType) {
        this(resourceType, DEFAULT_SAMPLING_INTERVAL, Long.MAX_VALUE);
    }

    /**
     * @param maxActive This is deprecated and will be ignored.
     * @deprecated Use {@link ResourceLeakDetector#ResourceLeakDetector(Class, int)}.
     * <p>
     * This should not be used directly by users of {@link ResourceLeakDetector}.
     * Please use {@link ResourceLeakDetectorFactory#newResourceLeakDetector(Class)}
     * or {@link ResourceLeakDetectorFactory#newResourceLeakDetector(Class, int, long)}
     */
    @Deprecated
    public ResourceLeakDetector(Class<?> resourceType, int samplingInterval, long maxActive) {
        this(resourceType, samplingInterval);
    }

    /**
     * This should not be used directly by users of {@link ResourceLeakDetector}.
     * Please use {@link ResourceLeakDetectorFactory#newResourceLeakDetector(Class)}
     * or {@link ResourceLeakDetectorFactory#newResourceLeakDetector(Class, int, long)}
     */
    @SuppressWarnings("deprecation")
    public ResourceLeakDetector(Class<?> resourceType, int samplingInterval) {
        this(simpleClassName(resourceType), samplingInterval, Long.MAX_VALUE);
    }

    /**
     * @param maxActive This is deprecated and will be ignored.
     * @deprecated use {@link ResourceLeakDetectorFactory#newResourceLeakDetector(Class, int, long)}.
     * <p>
     */
    @Deprecated
    public ResourceLeakDetector(String resourceType, int samplingInterval, long maxActive) {
        if (resourceType == null) {
            throw new NullPointerException("resourceType");
        }

        this.resourceType = resourceType;
        this.samplingInterval = samplingInterval;
    }

    /**
     * Creates a new {@link ResourceLeak} which is expected to be closed via {@link ResourceLeak#close()} when the
     * related resource is deallocated.
     *
     * @return the {@link ResourceLeak} or {@code null}
     * @deprecated use {@link #track(Object)}
     */
    @Deprecated
    public final ResourceLeak open(T obj) {
        return track0(obj);
    }

    /**
     * Creates a new {@link ResourceLeakTracker} which is expected to be closed via
     * {@link ResourceLeakTracker#close(Object)} when the related resource is deallocated.
     *
     * @return the {@link ResourceLeakTracker} or {@code null}
     */
    public final ResourceLeakTracker<T> track(T obj) {
        return track0(obj);
    }

    private DefaultResourceLeak track0(T obj) {
        Level level = ResourceLeakDetector.level;
        if (level == Level.DISABLED) {
            return null;
        }

        if (level.ordinal() < Level.PARANOID.ordinal()) {
            if ((PlatformDependent.threadLocalRandom().nextInt(samplingInterval)) == 0) {
                reportLeak(level);
                return new DefaultResourceLeak(obj);
            } else {
                return null;
            }
        } else {
            reportLeak(level);
            return new DefaultResourceLeak(obj);
        }
    }

    private void reportLeak(Level level) {
        if (!logger.isErrorEnabled()) {
            for (; ; ) {
                @SuppressWarnings("unchecked")
                DefaultResourceLeak ref = (DefaultResourceLeak) refQueue.poll();
                if (ref == null) {
                    break;
                }
                ref.close();
            }
            return;
        }

        // Detect and report previous leaks.
        for (; ; ) {
            @SuppressWarnings("unchecked")
            DefaultResourceLeak ref = (DefaultResourceLeak) refQueue.poll();
            if (ref == null) {
                break;
            }

            ref.clear();

            if (!ref.close()) {
                continue;
            }

            String records = ref.toString();
            if (reportedLeaks.putIfAbsent(records, Boolean.TRUE) == null) {
                if (records.isEmpty()) {
                    reportUntracedLeak(resourceType);
                } else {
                    reportTracedLeak(resourceType, records);
                }
            }
        }
    }

    /**
     * This method is called when a traced leak is detected. It can be overridden for tracking how many times leaks
     * have been detected.
     */
    protected void reportTracedLeak(String resourceType, String records) {
        logger.error(
                "LEAK: {}.release() was not called before it's garbage-collected. See http://netty.io/wiki/reference-counted-objects.html for more information.{}",
                resourceType, records);
    }

    /**
     * This method is called when an untraced leak is detected. It can be overridden for tracking how many times leaks
     * have been detected.
     */
    protected void reportUntracedLeak(String resourceType) {
        logger.error("LEAK: {}.release() was not called before it's garbage-collected. " +
                        "Enable advanced leak reporting to find out where the leak occurred. " +
                        "To enable advanced leak reporting, " +
                        "specify the JVM option '-D{}={}' or call {}.setLevel() " +
                        "See http://netty.io/wiki/reference-counted-objects.html for more information.",
                resourceType, PROP_LEVEL, Level.ADVANCED.name().toLowerCase(), simpleClassName(this));
    }

    /**
     * @deprecated This method will no longer be invoked by {@link ResourceLeakDetector}.
     */
    @Deprecated
    protected void reportInstancesLeak(String resourceType) {
    }

    @SuppressWarnings("deprecation")
    private final class DefaultResourceLeak extends PhantomReference<Object> implements ResourceLeakTracker<T>,
            ResourceLeak {

        private final String creationRecord;

        private final Deque<String> lastRecords = new ArrayDeque<String>();

        private final int trackedHash;

        private int removedRecords;

        DefaultResourceLeak(Object referent) {
            super(referent, refQueue);

            assert referent != null;

            // Store the hash of the tracked object to later assert it in the close(...) method.
            // It's important that we not store a reference to the referent as this would disallow it from
            // be collected via the PhantomReference.
            trackedHash = System.identityHashCode(referent);

            Level level = getLevel();
            if (level.ordinal() >= Level.ADVANCED.ordinal()) {
                creationRecord = newRecord(null, 3);
            } else {
                creationRecord = null;
            }
            allLeaks.put(this, LeakEntry.INSTANCE);
        }

        @Override
        public void record() {
            record0(null, 3);
        }

        @Override
        public void record(Object hint) {
            record0(hint, 3);
        }

        private void record0(Object hint, int recordsToSkip) {
            // Check MAX_RECORDS > 0 here to avoid similar check before remove from and add to lastRecords
            if (creationRecord != null && MAX_RECORDS > 0) {
                String value = newRecord(hint, recordsToSkip);

                synchronized (lastRecords) {
                    int size = lastRecords.size();
                    if (size == 0 || !lastRecords.getLast().equals(value)) {
                        if (size >= MAX_RECORDS) {
                            lastRecords.removeFirst();
                            ++removedRecords;
                        }
                        lastRecords.add(value);
                    }
                }
            }
        }

        @Override
        public boolean close() {
            // Use the ConcurrentMap remove method, which avoids allocating an iterator.
            return allLeaks.remove(this, LeakEntry.INSTANCE);
        }

        @Override
        public boolean close(T trackedObject) {
            // Ensure that the object that was tracked is the same as the one that was passed to close(...).
            assert trackedHash == System.identityHashCode(trackedObject);

            // We need to actually do the null check of the trackedObject after we close the leak because otherwise
            // we may get false-positives reported by the ResourceLeakDetector. This can happen as the JIT / GC may
            // be able to figure out that we do not need the trackedObject anymore and so already enqueue it for
            // collection before we actually get a chance to close the enclosing ResourceLeak.
            return close() && trackedObject != null;
        }

        @Override
        public String toString() {
            if (creationRecord == null) {
                return EMPTY_STRING;
            }

            final Object[] array;
            final int removedRecords;
            synchronized (lastRecords) {
                array = lastRecords.toArray();
                removedRecords = this.removedRecords;
            }

            StringBuilder buf = new StringBuilder(16384).append(NEWLINE);
            if (removedRecords > 0) {
                buf.append("WARNING: ")
                        .append(removedRecords)
                        .append(" leak records were discarded because the leak record count is limited to ")
                        .append(MAX_RECORDS)
                        .append(". Use system property ")
                        .append(PROP_MAX_RECORDS)
                        .append(" to increase the limit.")
                        .append(NEWLINE);
            }
            buf.append("Recent access records: ")
                    .append(array.length)
                    .append(NEWLINE);

            if (array.length > 0) {
                for (int i = array.length - 1; i >= 0; i--) {
                    buf.append('#')
                            .append(i + 1)
                            .append(':')
                            .append(NEWLINE)
                            .append(array[i]);
                }
            }

            buf.append("Created at:")
                    .append(NEWLINE)
                    .append(creationRecord);

            buf.setLength(buf.length() - NEWLINE.length());
            return buf.toString();
        }
    }

    private static final String[] STACK_TRACE_ELEMENT_EXCLUSIONS = {
            "io.netty.util.ReferenceCountUtil.touch(",
            "io.netty.buffer.AdvancedLeakAwareByteBuf.touch(",
            "io.netty.buffer.AbstractByteBufAllocator.toLeakAwareBuffer(",
            "io.netty.buffer.AdvancedLeakAwareByteBuf.recordLeakNonRefCountingOperation("
    };

    static String newRecord(Object hint, int recordsToSkip) {
        StringBuilder buf = new StringBuilder(4096);

        // Append the hint first if available.
        if (hint != null) {
            buf.append("\tHint: ");
            // Prefer a hint string to a simple string form.
            if (hint instanceof ResourceLeakHint) {
                buf.append(((ResourceLeakHint) hint).toHintString());
            } else {
                buf.append(hint);
            }
            buf.append(NEWLINE);
        }

        // Append the stack trace.
        StackTraceElement[] array = new Throwable().getStackTrace();
        for (StackTraceElement e : array) {
            if (recordsToSkip > 0) {
                recordsToSkip--;
            } else {
                String estr = e.toString();

                // Strip the noisy stack trace elements.
                boolean excluded = false;
                for (String exclusion : STACK_TRACE_ELEMENT_EXCLUSIONS) {
                    if (estr.startsWith(exclusion)) {
                        excluded = true;
                        break;
                    }
                }

                if (!excluded) {
                    buf.append('\t');
                    buf.append(estr);
                    buf.append(NEWLINE);
                }
            }
        }

        return buf.toString();
    }

    private static final class LeakEntry {

        static final LeakEntry INSTANCE = new LeakEntry();

        private static final int HASH = System.identityHashCode(INSTANCE);

        private LeakEntry() {
        }

        @Override
        public int hashCode() {
            return HASH;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }
    }
}