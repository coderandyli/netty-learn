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

    public TimeClientHandle(String host, int port) {
        this.host = (host == null) ? "127.0.0.1" : host;
        this.port = port;
        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);

        }
    }

    public void run() {
        try {
            doConnect();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

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
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void hadnleInput(SelectionKey key) throws IOException {
        if (key.isValid()){
            SocketChannel sc = (SocketChannel) key.channel();
            if (key.isConnectable()){
                if (sc.finishConnect()){
                    sc.register(selector, SelectionKey.OP_READ);
                    doWrite(sc);
                }else {
                    System.exit(1); // 连接失败，进程退出
                }
            }
        }

        if (key.isReadable()){
            ByteBuffer readBuffer = ByteBuffer.allocate(1024);
            int readBytes = socketChannel.read(readBuffer);
            if (readBytes > 0){
                readBuffer.flip();
                byte[] bytes = new byte[readBuffer.remaining()];
                readBuffer.get(bytes);
                String body = new String(bytes, "UTF-8");
                System.out.println("Now is : " + body);
                this.stop = true;
            }else if (readBytes < 0){
                key.cancel();
                socketChannel.close();
            }else {
                ; // 忽略
            }
        }

    }

    private void doConnect() throws IOException {
        // 如果直接连接成功，则注册到多路复用器中，发送请求消息，读应答
         if (socketChannel.connect(new InetSocketAddress(host, port))){
            socketChannel.register(selector, SelectionKey.OP_READ);
            doWrite(socketChannel);
         }else {
             socketChannel.register(selector, SelectionKey.OP_CONNECT);
         }
    }

    private void doWrite(SocketChannel channel) throws IOException{
        byte[] req = "QUERY TIME ORDER".getBytes();
        ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
        writeBuffer.put(req);
        writeBuffer.flip();
        socketChannel.write(writeBuffer);
        if (!writeBuffer.hasRemaining()){
            System.out.println("Send order 2 server succeed");
        }

    }
}
