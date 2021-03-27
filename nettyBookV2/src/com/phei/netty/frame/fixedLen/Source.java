package com.phei.netty.frame.fixedLen;

public class Source{
    private final static int STACKLEVEL= 1;
    private final static int STACKLEVEL2= 2;

    public static String Position1(){     
        StackTraceElement[] stacks = new Throwable().getStackTrace();     

        if(stacks.length < (STACKLEVEL + 1)){
            return "";
        }

        StringBuffer sb = new StringBuffer();      
        sb.append("[")
            .append(Thread.currentThread().getName())
            .append(":" + stacks[STACKLEVEL].getFileName())
            .append(":" + stacks[STACKLEVEL].getClassName())
            .append(":" + stacks[STACKLEVEL].getMethodName() + "()")
            .append(":" + stacks[STACKLEVEL].getLineNumber())
            .append("]");

        return sb.toString();     
    }
    public static String Position2(){
        return "[" +
            Thread.currentThread().getName() + ":" +
            Thread.currentThread().getStackTrace()[STACKLEVEL2].getFileName()   + ":" +
            Thread.currentThread().getStackTrace()[STACKLEVEL2].getClassName()  + ":" +
            Thread.currentThread().getStackTrace()[STACKLEVEL2].getMethodName() + "()" + ":" +
            Thread.currentThread().getStackTrace()[STACKLEVEL2].getLineNumber() + "]";
    }
}