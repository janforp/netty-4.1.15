package io.netty.buffer;

import io.netty.util.IllegalReferenceCountException;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static io.netty.util.internal.ObjectUtil.checkPositive;

/**
 * Abstract base class for {@link ByteBuf} implementations that count references.
 */
public abstract class AbstractReferenceCountedByteBuf extends AbstractByteBuf {

    /**
     * @see AbstractReferenceCountedByteBuf#refCnt 该字段用于修改 refCnt 字段
     */
    private static final AtomicIntegerFieldUpdater<AbstractReferenceCountedByteBuf> refCntUpdater =
            AtomicIntegerFieldUpdater.newUpdater(AbstractReferenceCountedByteBuf.class, "refCnt");

    private volatile int refCnt = 1;

    protected AbstractReferenceCountedByteBuf(int maxCapacity) {
        super(maxCapacity);
    }

    @Override
    public int refCnt() {
        return refCnt;
    }

    /**
     * An unsafe operation intended for use by a subclass that sets the reference count of the buffer directly
     */
    protected final void setRefCnt(int refCnt) {
        this.refCnt = refCnt;
    }

    @Override
    public ByteBuf retain() {
        //每调用一次该方法，引用计数就会增加1
        return retain0(1);
    }

    //retain ： 保持
    @Override
    public ByteBuf retain(int increment) {
        return retain0(checkPositive(increment, "increment"));
    }

    private ByteBuf retain0(int increment) {
        //每调用一次该方法，引用计数就会增加 increment
        for (; ; ) {
            //保证线程安全，使用 cas 直到成功

            int refCnt = this.refCnt;
            final int nextCnt = refCnt + increment;

            // Ensure we not resurrect (which means the refCnt was 0) and also that we encountered an overflow.
            // 确保我们不复活（这意味着refCnt为0），并且我们也遇到溢出。
            if (nextCnt <= increment) {
                // 只有 this.refCnt == 0 的时候才发生
                throw new IllegalReferenceCountException(refCnt, increment);
            }
            if (refCntUpdater.compareAndSet(this, refCnt, nextCnt)) {
                //如果 cas 修改成功，则退出循环，否则一直 cas 修改
                break;
            }
        }
        return this;
    }

    @Override
    public ByteBuf touch() {
        return this;
    }

    @Override
    public ByteBuf touch(Object hint) {
        return this;
    }

    @Override
    public boolean release() {
        //与 retain 相反
        return release0(1);
    }

    @Override
    public boolean release(int decrement) {
        return release0(checkPositive(decrement, "decrement"));
    }

    private boolean release0(int decrement) {
        for (; ; ) {
            int refCnt = this.refCnt;
            if (refCnt < decrement) {
                throw new IllegalReferenceCountException(refCnt, -decrement);
            }

            if (refCntUpdater.compareAndSet(this, refCnt, refCnt - decrement)) {
                if (refCnt == decrement) {//如果该条件成立，则 cas 成功之后就会失去引用，也就是引用计数 = 0
                    //调用回收方法，模版方法
                    deallocate();
                    return true;
                }

                //释放部分引用之后还没有完全释放完成，则说明还有其他地方在使用当前对象
                return false;
            }
        }
    }

    /**
     * Called once {@link #refCnt()} is equals 0.
     *
     * 引用计数 = 0 的时候被调用
     */
    protected abstract void deallocate();
}
