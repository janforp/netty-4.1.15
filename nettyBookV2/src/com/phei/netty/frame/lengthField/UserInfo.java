package com.phei.netty.frame.lengthField;

import org.msgpack.annotation.Message;

/**
 * Created by hmh on 21/08/2017.
 */
@Message
public class UserInfo {
    public Integer age;
    public String name;
}
