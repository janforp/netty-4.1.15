package com.netty.qwzn.codes.customerprotocal;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Header
 *
 * @author zhucj
 * @since 20210325
 */
@Data
public class Header {

    private int crcCode = 0xabef0101;

    //消息长度
    private int length;

    //会话ID
    private long sessionID;

    //消息类型
    private byte type;

    //消息优先级
    private byte priority;

    //附件
    private Map<String, Object> attachment = new HashMap<>();
}
