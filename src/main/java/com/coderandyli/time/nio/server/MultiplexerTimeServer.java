package com.coderandyli.time.nio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * 多路复用类
 *
 * @author lizhenzhen
 * @version 1.0
 * @date 2021/4/9 下午3:48
 */
public class MultiplexerTimeServer implements Runnable {
    /**
     * 多路复用器（翻译成选择器不太合适）
     */
    private Selector selector;
    /**
     * 通道（所有客户端连接的父通道）
     */
    private ServerSocketChannel serverSocketChannel;
    /**
     * 是否停止
     */
    private volatile boolean stop;

    /**
     * 初始化多路复用器，绑定监听端口（资源初始化）
     *
     * @param port
     */
    public MultiplexerTimeServer(int port) {
        try {
            selector = Selector.open();
            // 打开ServerSocketChannel，监听客户端连接（所有客户端连接的父管道）
            serverSocketChannel = ServerSocketChannel.open();
            //  绑定监听端口; 设置连接为非阻塞模式; backlog设置为1024
            serverSocketChannel.socket().bind(new InetSocketAddress(port), 1024);
            serverSocketChannel.configureBlocking(false);
            // 将ServerSocketChannel注册到Selector上，监听OP_ACCEPT事件
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("The time server is start in port : " + port);
        } catch (IOException e) {
            e.printStackTrace();
            // 资源初始化失败，eg: 端口被占用，退出
            System.exit(1);
        }
    }

    public void stop() {
        this.stop = true;
    }

    public void run() {
        // 无限循环体内轮询准备就绪的key，无论是否有读写事件发生，selector每隔一秒就会被唤醒一次。
        while (!stop) {
            try {
                // 每个1秒被唤醒一次
                selector.select(1000);
                // 准备就绪的SelectionKey( 准备就绪的Channel的SelectionKey，通过SelectionKey找到Channel)
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectionKeys.iterator();
                SelectionKey key = null;
                while (it.hasNext()) {
                    key = it.next();
                    it.remove();
                    try {
                        handleInput(key);
                    } catch (Exception e) {
                        if (key != null) {
                            key.cancel();
                            if (key.channel() != null) {
                                key.channel().close();
                            }
                        }
                    }
                }

            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        // 多路复用器关闭后，所有注册在上面的channel和pipe等资源都会被自动去注册并关闭，所以不需要重复释放资源
        if (selector != null){
            try {
                selector.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void handleInput(SelectionKey key) throws IOException {
        if (key.isValid()){
            // 处理新接入的请求信息
            if (key.isAcceptable()){
                // 通过accept()接收客户端连接请求，并创建SocketChannel实例（相当于完成TCP三次握手，TCP物理链路正式建立）
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);

                // Add the new connection to the selector
                sc.register(selector, SelectionKey.OP_READ);
            }

            // 读取客户端的请求信息
            if (key.isReadable()){
                SocketChannel sc = (SocketChannel) key.channel();
                // 开辟一个1M的缓冲区
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                // 读取请求码流（由于开启了非阻塞，所以read操作是非阻塞的，通过返回值进行判断）
                int readBytes = sc.read(readBuffer);
                if (readBytes > 0){
                    // 读到了字节，对字节进行编码
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String body = new String(bytes, "UTF-8");
                    System.out.println("The time server receive order : " + body);
                    String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
                    doWrite(sc, currentTime);
                }else if (readBytes < 0){
                    // 对端链路关闭，需要关闭channel, 释放资源
                    key.channel();
                    sc.close();
                }else { // 没有读取到字节，属于正常场景，忽略
                    ; // 读取到0字节，忽略
                }
            }
        }
    }

    /**
     * 将响应消息异步应道给客户端，
     *  * 由于SocketChannel是异步非阻塞的，并不能保证一次能够把字节数组发送完，从而出现"写半包"问题。---- 后续跟进。
     */
    private void doWrite(SocketChannel channel, String response) throws IOException {
        if (response != null && response.trim().length() > 0){
            //
            byte[] bytes = response.getBytes();
            // 开辟一个bytes.length大小的缓冲区
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            // 将字节数组复制到缓冲区
            writeBuffer.put(bytes);
            // 使缓冲区为一系列新的通道写入或相对获取 操作做好准备
            writeBuffer.flip();
            // 将缓冲区的字节数组，发送出去
            channel.write(writeBuffer);
        }
    }
}
