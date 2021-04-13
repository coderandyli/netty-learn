package com.coderandyli.time.nio.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author lizhenzhen
 * @version 1.0
 * @date 2021/4/9 下午9:29
 */
public class TimeClientHandle implements Runnable {
    private String host;
    private int port;
    private Selector selector;
    private SocketChannel socketChannel;
    private volatile boolean stop;

    /**
     * 初始化NIO多路复用器和SocketChannel对象
     */
    public TimeClientHandle(String host, int port) {
        this.host = (host == null) ? "127.0.0.1" : host;
        this.port = port;
        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false); // 设置为异步非阻塞
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1); // 退出

        }
    }

    public void run() {
        // 发送连接请求
        try {
            doConnect();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1); // 退出
        }

        // 在循环体中轮询多路复用器Selector，当有就绪的Channel时，执行handleInput()方法
        while (!stop) {
            try {
                selector.select(1000);
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectionKeys.iterator();
                SelectionKey key = null;
                while (it.hasNext()) {
                    key = it.next();
                    it.remove();
                    try {
                        hadnleInput(key);
                    } catch (Exception e) {
                        if (key != null) {
                            key.cancel();
                            if (key.channel() != null) {
                                key.channel().close();
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        // 多路复用关闭后，所有注册在上面的Channel、pipe等资源都关闭，释放资源
        if (selector != null){
            try {
                // 优雅关闭，jdk底层会自动释放所有跟此多路复用器的所有资源
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理input
     *  - 判断key的状态，执行不同逻辑
     */
    private void hadnleInput(SelectionKey key) throws IOException {
        if (key.isValid()){
            SocketChannel sc = (SocketChannel) key.channel();
            // 判断是否处于连接状态
            if (key.isConnectable()){
                // 如果处于连接状态说明服务端已经返回ack应答消息
                // 此时需要调用 sc.finishConnect()判断客户端是否连接成功，
                if (sc.finishConnect()){
                    // 将socketchannel注册到Selector多路复用器上，打开SelectionKey.OP_READ，监听网路读操作。
                    sc.register(selector, SelectionKey.OP_READ);
                    // 发送请求消息给服务端
                    doWrite(sc);
                }else {
                    System.exit(1); // 连接失败，进程退出
                }
            }
        }

        // 读取服务器应答消息
        if (key.isReadable()){
            // 由于发送判断预判应答码流的大小，所以预先分配1MB的接收缓冲区用于读取应答消息
            ByteBuffer readBuffer = ByteBuffer.allocate(1024);
            // read是异步的所以必须对结果进行判断
            int readBytes = socketChannel.read(readBuffer);
            if (readBytes > 0){
                // 读到了字节，对字节进行编码
                readBuffer.flip();
                byte[] bytes = new byte[readBuffer.remaining()];
                readBuffer.get(bytes);
                String body = new String(bytes, "UTF-8");
                System.out.println("Now is : " + body);
                // 执行完成后，stop设置为true，线程退出循环
                this.stop = true;
            }else if (readBytes < 0){
                // 对端链路关闭，需要关闭channel, 释放资源
                key.cancel();
                socketChannel.close();
            }else {
                ; // 未读到字节，正常现象，忽略
            }
        }

    }

    /**
     *
     */
    private void doConnect() throws IOException {
        // 如果直接连接成功，则注册到多路复用器中，发送请求消息，读应答
         if (socketChannel.connect(new InetSocketAddress(host, port))){
             // 注册到Selector多路复用器上，
            socketChannel.register(selector, SelectionKey.OP_READ);
            doWrite(socketChannel);
         }else {
             // 没有连接成功，说明服务端并没有返回TCP握手消息应答消息，但并不代表连接失败，
             // 需要将socketChannel注册到Selector多路复用器上，注册SelectionKey.OP_CONNECT，
             // 当服务端返回syn-ack消息后，Selector就能轮询到这个SocketChannel处于连接就绪状态。
             socketChannel.register(selector, SelectionKey.OP_CONNECT);
         }
    }

    /**
     * 发送请求消息到服务器
     */
    private void doWrite(SocketChannel channel) throws IOException{
        // 构造请求消息体
        byte[] req = "QUERY TIME ORDER".getBytes();
        ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
        writeBuffer.put(req);
        writeBuffer.flip();
        // 调用write()方法进行发送
        socketChannel.write(writeBuffer);
        // 由于发送是异步的，所以存在"半包写"的问题, 通过hasRemaining对发送结果进行判断，待**缓冲区消息**全部发送完成，
        // 打印输出 Send order 2 server succeed
        if (!writeBuffer.hasRemaining()){
            System.out.println("Send order 2 server succeed");
        }
    }
}
