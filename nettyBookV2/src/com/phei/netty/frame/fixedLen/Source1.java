package com.phei.netty.frame.fixedLen;


/**
 * Created by hmh on 7/5/17.
 * <br/>
 * 记录代码执行位置
 * <br/>
 * http://blog.csdn.net/tumuzhuanjia/article/details/8792121#
 */
public class Source1 {
	private final static int STACKLEVEL = 1;
	private final static int STACKLEVEL2 = 2;

	public static String Position1( ) {
		StackTraceElement[] stacks = new Throwable( ).getStackTrace( );

		if( stacks.length < ( STACKLEVEL + 1 ) ) {
			return "";
		}

		StringBuffer sb = new StringBuffer( );
		sb.append( "[" ).append( stacks[STACKLEVEL].getFileName( ) ).append( ";" + stacks[STACKLEVEL].getLineNumber( ) ).append( "]" );

		return sb.toString( );
	}

	/**
	 * @param reason 记录日志原因
	 * @return
	 */
	public static String Position1( String reason ) {
		StackTraceElement[] stacks = new Throwable( ).getStackTrace( );

		if( stacks.length < ( STACKLEVEL + 1 ) ) {
			return "";
		}

		StringBuffer sb = new StringBuffer( );
		sb.append( "[" ).append( stacks[STACKLEVEL].getFileName( ) ).append( ";" + stacks[STACKLEVEL].getLineNumber( ) );

		if( reason != null && !"".equals(reason) ) {
			sb.append( ";" + reason );
		}
		sb.append( "]" );

		return sb.toString( );
	}


	public static String Position2( ) {
		return "[" + Thread.currentThread( ).getStackTrace( )[STACKLEVEL2].getFileName( ) + ";"
				+ Thread.currentThread( ).getStackTrace( )[STACKLEVEL2].getLineNumber( ) + "]";
	}

	public static String Position2( String reason ) {
		String reasonStr = "";
		if( reason != null && !"".equals(reason) ) {
			reasonStr = ";" + reason;
		}
		return "[" + Thread.currentThread( ).getStackTrace( )[STACKLEVEL2].getFileName( ) + ";"
				+ Thread.currentThread( ).getStackTrace( )[STACKLEVEL2].getLineNumber( ) + reasonStr + "]";
	}
}
