/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.phei.netty.frame.fixedLen;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author lilinfeng
 * @date 2014年2月14日
 * @version 1.0
 *
[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo:handlerAdded():90]
[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo1:handlerAdded():148]
[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo2:handlerAdded():206]
[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo1:channelRegistered():166]
[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo2:channelRegistered():224]
[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo:channelActive():114]
[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo1:channelActive():172]
[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo2:channelActive():230]
 */
public class EchoServer {
    public void bind(int port) throws Exception {
	// 配置服务端的NIO线程组
	EventLoopGroup bossGroup = new NioEventLoopGroup();
	EventLoopGroup workerGroup = new NioEventLoopGroup();
	try {
	    ServerBootstrap b = new ServerBootstrap();
	    b.group(bossGroup, workerGroup)
		    .channel(NioServerSocketChannel.class)
		    .option(ChannelOption.SO_BACKLOG, 100)
		    .handler(new LoggingHandler(LogLevel.INFO))
		    .childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch)
				throws Exception {
				ch.pipeline().addFirst(new Demo());
				ch.pipeline().addLast(new Demo1());
				ch.pipeline().addLast(new Demo2());
			    ch.pipeline().addLast(
				    new FixedLengthFrameDecoder(5));
			    ch.pipeline().addLast(new StringDecoder());
			    ch.pipeline().addLast(new EchoServerHandler());
			}
		    });

	    // 绑定端口，同步等待成功
	    ChannelFuture f = b.bind(port).sync();

	    // 等待服务端监听端口关闭
	    f.channel().closeFuture().sync();
	} finally {
	    // 优雅退出，释放线程池资源
	    bossGroup.shutdownGracefully();
	    workerGroup.shutdownGracefully();
	}
    }

    public static void main(String[] args) throws Exception {
	int port = 8080;
	if (args != null && args.length > 0) {
	    try {
		port = Integer.valueOf(args[0]);
	    } catch (NumberFormatException e) {
		// 采用默认值
	    }
	}
	new EchoServer().bind(port);
    }

	static class Demo extends ChannelHandlerAdapter{
		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.handlerAdded(ctx);
		}

		@Override
		public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.handlerRemoved(ctx);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			System.out.println(Source.Position1());
			super.exceptionCaught(ctx, cause);
		}

		@Override
		public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.channelRegistered(ctx);
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.channelActive(ctx);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.channelInactive(ctx);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			System.out.println(Source.Position1());
			super.channelRead(ctx, msg);
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.channelReadComplete(ctx);
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			System.out.println(Source.Position1());
			System.out.println("userEvent:"+evt);
//			super.userEventTriggered(ctx, evt);
//			ctx.fireUserEventTriggered(evt);
			ctx.pipeline().fireUserEventTriggered(evt);
		}


	}

    static class Demo1 extends ChannelHandlerAdapter{
		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.handlerAdded(ctx);
		}

		@Override
		public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.handlerRemoved(ctx);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			System.out.println(Source.Position1());
			super.exceptionCaught(ctx, cause);
		}

		@Override
		public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.channelRegistered(ctx);
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.channelActive(ctx);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.channelInactive(ctx);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			System.out.println(Source.Position1());
			ctx.pipeline().fireUserEventTriggered("demo1 channel read");
			super.channelRead(ctx, msg);
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.channelReadComplete(ctx);
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			System.out.println(Source.Position1());
			System.out.println("userEvent:"+evt);
			/*
			[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo:userEventTriggered():147]
			userEvent:demo1 channel read
			[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo1:userEventTriggered():207]
			userEvent:demo1 channel read
			[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo2:userEventTriggered():266]
			userEvent:demo1 channel read
			 */
			super.userEventTriggered(ctx, evt);
			/*
			[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo:userEventTriggered():147]
			userEvent:demo1 channel read
			[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo1:userEventTriggered():208]
			userEvent:demo1 channel read
			[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo2:userEventTriggered():278]
			userEvent:demo1 channel read
			 */
			ctx.fireUserEventTriggered(evt);// equals to super.userEventTriggered(ctx, evt);
			/*
			[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo1:channelRead():196]
			[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo:userEventTriggered():147]
			userEvent:demo1 channel read
			[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo:userEventTriggered():147]
			userEvent:demo1 channel read
			[nioEventLoopGroup-3-1:EchoServer.java:com.phei.netty.frame.fixedLen.EchoServer$Demo:userEventTriggered():147]
			userEvent:demo1 channel read
			 */
			ctx.pipeline().fireUserEventTriggered(evt);


		}


	}

	static class Demo2 extends ChannelHandlerAdapter{
		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.handlerAdded(ctx);
		}

		@Override
		public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.handlerRemoved(ctx);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			System.out.println(Source.Position1());
			super.exceptionCaught(ctx, cause);
		}

		@Override
		public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.channelRegistered(ctx);
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.channelActive(ctx);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.channelInactive(ctx);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			System.out.println(Source.Position1());
			super.channelRead(ctx, msg);
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
			System.out.println(Source.Position1());
			super.channelReadComplete(ctx);
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			System.out.println(Source.Position1());
			System.out.println("userEvent:"+evt);
//			super.userEventTriggered(ctx, evt);
//			ctx.fireUserEventTriggered(evt);
			ctx.pipeline().fireUserEventTriggered(evt);
		}


	}
}
