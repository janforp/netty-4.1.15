package com.netty.qwzn.codes.customerprotocal;

import lombok.Data;

/**
 * NettyMessage
 *
 * @author zhucj
 * @since 20210325
 */
@Data
public class NettyMessage {

    //消息头
    private Header header;

    //消息体
    private Object body;
}
