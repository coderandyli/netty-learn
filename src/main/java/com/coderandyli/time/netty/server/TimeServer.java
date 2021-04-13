package com.coderandyli.time.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 基于Netty的时间服务器-服务端
 *
 * @author lizhenzhen
 * @version 1.0
 * @date 2021/4/11 下午9:28
 */
public class TimeServer {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }

        new TimeServer().bind(port);
    }

    public void bind(int port) throws Exception {
        // 配置服务端的NIO线程组(创建两个第NioEventLoopGroup)，其实就是两个NIO线程组
        // 一个用于接受客户端连接；另一个用于SocketChannel的读写
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {
            // Netty NIO服务的启动辅助类。目的是降低服务端的开发复杂度
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 将两个NIO线程组传递给serverBootstrap
            serverBootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class) // 将channel设置为NioServerSocketChannel 对应 JDK NIO的ServerSocketChannel
                    .option(ChannelOption.SO_BACKLOG, 1024) // 配置tcp参数
                    .childHandler(new ChildChannelHandler()); // 绑定IO事件的处理类ChildChannelHandler（主要用于处理网路IO事件，eg：记录日志、对消息进行编码）
            // 绑定端口，同步等待成功
            ChannelFuture f = serverBootstrap.bind(port).sync();  // ChannelFuture功能类似于jdk中的Future

            // 等待服务器监听端口关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅退出，释放线程池资源
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            socketChannel.pipeline().addLast(new TimeServerHandler());
        }
    }
}
