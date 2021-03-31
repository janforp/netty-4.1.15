package io.netty.channel.nio;

import java.nio.channels.SelectionKey;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;

final class SelectedSelectionKeySet extends AbstractSet<SelectionKey> {

    /**
     * 一个容量为 1024 的数组，当然可以扩容
     */
    SelectionKey[] keys;

    /**
     * 下一个存放元素的下标，也就是当前数组中的元素个数
     */
    int size;

    SelectedSelectionKeySet() {
        keys = new SelectionKey[1024];
    }

    @Override
    public boolean add(SelectionKey o) {
        if (o == null) {
            return false;
        }

        keys[size++] = o;
        if (size == keys.length) {
            //满了就扩容
            increaseCapacity();
        }

        return true;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator<SelectionKey> iterator() {
        throw new UnsupportedOperationException();
    }

    void reset() {
        reset(0);
    }

    void reset(int start) {
        //就是把数组的每一个元素都填成 null
        Arrays.fill(keys, start, size, null);
        size = 0;
    }

    private void increaseCapacity() {
        // 翻倍
        SelectionKey[] newKeys = new SelectionKey[keys.length << 1];
        System.arraycopy(keys, 0, newKeys, 0, size);
        keys = newKeys;
    }
}
