package io.netty.util.concurrent;

import io.netty.util.internal.StringUtil;

import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ThreadFactory} implementation with a simple naming rule.
 */
public class DefaultThreadFactory implements ThreadFactory {

    /**
     * 每个DefaultThreadFactory实例有自己的 poolId
     */
    private static final AtomicInteger poolId = new AtomicInteger();

    /**
     * 线程名称的一部分
     *
     * 每个DefaultThreadFactory实例内部生产的线程都有自己的线程ID
     */
    private final AtomicInteger nextId = new AtomicInteger();

    /**
     * 线程名称前缀
     */
    private final String prefix;

    /**
     * 是否是守护线程
     */
    private final boolean daemon;

    /**
     * 线程的优先级,默认 5
     */
    private final int priority;

    /**
     * 线程所在的线程组
     */
    protected final ThreadGroup threadGroup;

    public DefaultThreadFactory(Class<?> poolType) {
        this(poolType, false, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(String poolName) {
        this(poolName, false, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(Class<?> poolType, boolean daemon) {
        this(poolType, daemon, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(String poolName, boolean daemon) {
        this(poolName, daemon, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(Class<?> poolType, int priority) {
        this(poolType, false, priority);
    }

    public DefaultThreadFactory(String poolName, int priority) {
        this(poolName, false, priority);
    }

    public DefaultThreadFactory(Class<?> poolType, boolean daemon, int priority) {
        this(
                /**
                 * 获取名字
                 * @see NioEventLoopGroup
                 */
                toPoolName(poolType),
                daemon, priority
        );
    }

    /**
     * TODO 此处是否可以缓存？？？
     */
    public static String toPoolName(Class<?> poolType) {
        if (poolType == null) {
            throw new NullPointerException("poolType");
        }

        //com.a.b.NioEventLoopGroup ====》 NioEventLoopGroup 获取一个不包含包名称的 className
        String poolName = StringUtil.simpleClassName(poolType);
        switch (poolName.length()) {
            case 0:
                //一般不会发生
                return "unknown";
            case 1:

                //类只有一个字符
                return poolName.toLowerCase(Locale.US);
            default:

                //正常的类名称
                if (Character.isUpperCase(poolName.charAt(0))//第一个字符大写？
                        && Character.isLowerCase(poolName.charAt(1)))//第二个字符小写？

                {
                    /**
                     * 如果是 Aaxxxx 这样，则转换为 aaxxxx
                     * NioEventLoopGroup -----> nioEventLoopGroup
                     */
                    return Character.toLowerCase(poolName.charAt(0)) + poolName.substring(1);
                } else {

                    return poolName;
                }
        }
    }

    public DefaultThreadFactory(String poolName, boolean daemon, int priority, ThreadGroup threadGroup) {
        if (poolName == null) {
            throw new NullPointerException("poolName");
        }
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException("priority: " + priority + " (expected: Thread.MIN_PRIORITY <= priority <= Thread.MAX_PRIORITY)");
        }

        prefix = poolName + '-' + poolId.incrementAndGet() + '-';
        this.daemon = daemon;
        this.priority = priority;
        this.threadGroup = threadGroup;
    }

    public DefaultThreadFactory(String poolName, boolean daemon, int priority) {
        this(poolName, daemon, priority,

                //线程组
                System.getSecurityManager() == null ?
                        Thread.currentThread().getThreadGroup()
                        : System.getSecurityManager().getThreadGroup());
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = newThread(

                /**
                 * 装饰下传入的任务
                 */
                new DefaultRunnableDecorator(r),

                //线程名称:前缀 + 线程ID
                prefix + nextId.incrementAndGet()
        );

        try {
            if (t.isDaemon() != daemon) {
                t.setDaemon(daemon);
            }

            if (t.getPriority() != priority) {
                t.setPriority(priority);
            }
        } catch (Exception ignored) {
            // Doesn't matter even if failed to set.
        }
        return t;
    }

    protected Thread newThread(Runnable r, String name) {
        return new FastThreadLocalThread(threadGroup, r, name);
    }

    /**
     * 装饰一下
     */
    private static final class DefaultRunnableDecorator implements Runnable {

        private final Runnable r;

        DefaultRunnableDecorator(Runnable r) {
            this.r = r;
        }

        @Override
        public void run() {
            try {
                r.run();
            } finally {
                FastThreadLocal.removeAll();
            }
        }
    }
}
