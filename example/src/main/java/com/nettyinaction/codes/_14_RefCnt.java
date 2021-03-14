package com.nettyinaction.codes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * _14_RefCnt
 *
 * @author zhucj
 * @since 20210325
 */
public class _14_RefCnt {

    public static void main(String[] args) {
        refCnt();
    }

    private static void refCnt() {
        ByteBuf buffer = Unpooled.buffer();
        System.out.println(buffer.refCnt());
        boolean release = buffer.release();
        System.out.println(release);
        System.out.println(buffer.refCnt());
    }
}
