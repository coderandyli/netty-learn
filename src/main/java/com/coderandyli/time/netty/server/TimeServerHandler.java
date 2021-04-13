package com.coderandyli.time.netty.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.nio.ByteBuffer;
import java.util.Date;

/**
 * 对网络事件的读写进行操作
 *
 * @author lizhenzhen
 * @version 1.0
 * @date 2021/4/11 下午9:34
 */
public class TimeServerHandler extends ChannelHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg; //ByteBuf类似于JDK的ByteBuffer
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String body = new String(req, "UTF-8");
        System.out.println("The time server receive order: " + body);

        String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body)
                ? new Date(System.currentTimeMillis()).toString()
                : "BAD ORDER";

        ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
        // 异步发送消息给客户端
        ctx.write(resp);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // 将消息发送队列中的消息写入到SocketChannel中发送给对方，从性能角度考虑，为了防止频繁地唤醒Selector进行消息发送，write方法并不直接将消息写入SocketChannel
        // 调用write方法只是把待发送的消息放到发送缓冲中，再通过调用flush方法，将发送缓冲区中的消息全部写到SocketChannel中
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
