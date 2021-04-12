package com.coderandyli.time.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;

/**
 * @author lizhenzhen
 * @version 1.0
 * @date 2021/4/12 上午8:12
 */
public class TimeClient {
    public void connect(int port, String host) throws IOException, InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new TimeClientHandler(firstMessage))
                        }
                    });

            // 发起异步连接操作
            ChannelFuture f = bootstrap.connect();
            // 等待客户端链路关闭
            f.channel().closeFuture().sync();
        }finally {
            // 优雅退出
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        int port = 8080;
        if (args != null && args.length > 0){
            try {
                port =
            }catch (NumberFormatException e){

            }
        }

    }
}
