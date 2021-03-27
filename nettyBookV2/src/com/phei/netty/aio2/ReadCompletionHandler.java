package com.phei.netty.aio2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * Created by hmh on 15/05/2017.
 */
public class ReadCompletionHandler implements CompletionHandler<Integer, ByteBuffer> {

    private AsynchronousSocketChannel channel;

    public ReadCompletionHandler(AsynchronousSocketChannel result) {
        if (channel != null) {
            channel = result;
        }
    }

    @Override
    public void completed(Integer result, ByteBuffer attachment) {
        attachment.flip();
        byte[] body = new byte[attachment.remaining()];
        attachment.get(body);
        try {
            String order = new String(body,"UTF-8");
            System.out.println("time server recv order : " + order);
            String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(order) ? new Date().toString() : "BAD_ORDER";
            doWrite(currentTime);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void doWrite(String currentTime) {
        if(currentTime!=null && currentTime.trim().length()>0){
            byte[] body = currentTime.getBytes(Charset.forName("UTF-8"));
            final ByteBuffer buff = ByteBuffer.allocate(body.length);
            buff.put(body);
            buff.flip();
            channel.write(buff, buff, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    if(buff.hasRemaining())
                        channel.write(buff,buff,this);
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    exc.printStackTrace();
                    try {
                        channel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void failed(Throwable exc, ByteBuffer attachment) {
        exc.printStackTrace();
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
