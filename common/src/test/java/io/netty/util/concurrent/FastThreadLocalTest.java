package io.netty.util.concurrent;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class FastThreadLocalTest {
    @Before
    public void setUp() {
        FastThreadLocal.removeAll();
        assertThat(FastThreadLocal.size(), is(0));
    }

    @Test(timeout = 10000)
    public void testRemoveAll() throws Exception {
        final AtomicBoolean removed = new AtomicBoolean();
        final FastThreadLocal<Boolean> var = new FastThreadLocal<Boolean>() {
            @Override
            protected void onRemoval(Boolean value) {
                removed.set(true);
            }
        };

        // Initialize a thread-local variable.
        assertThat(var.get(), is(nullValue()));
        assertThat(FastThreadLocal.size(), is(1));

        // And then remove it.
        FastThreadLocal.removeAll();
        assertThat(removed.get(), is(true));
        assertThat(FastThreadLocal.size(), is(0));
    }

    @Test(timeout = 10000)
    public void testRemoveAllFromFTLThread() throws Throwable {
        final AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();
        final Thread thread = new FastThreadLocalThread() {
            @Override
            public void run() {
                try {
                    testRemoveAll();
                } catch (Throwable t) {
                    throwable.set(t);
                }
            }
        };

        thread.start();
        thread.join();

        Throwable t = throwable.get();
        if (t != null) {
            throw t;
        }
    }
}
