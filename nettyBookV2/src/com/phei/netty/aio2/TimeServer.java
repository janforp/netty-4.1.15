package com.phei.netty.aio2;

/**
 * Created by hmh on 15/05/2017.
 */
public class TimeServer {
    static int port = 8080;

    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        AsyncTimeServerHandler timeServerHandler = new AsyncTimeServerHandler(port);
        new Thread(timeServerHandler,"aio time server start!").start();
    }
}
