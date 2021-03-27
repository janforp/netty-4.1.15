package com.phei.netty.aio2;

/**
 * Created by hmh on 15/05/2017.
 */
public class TimeClient {
    static int port = 8080;
    public static void main(String[] args) {
        if(args!=null && args.length>0){
            port = Integer.parseInt(args[0]);
        }
    }
}
