package com.coderandyli.time.nio.server;

/**
 * 时间服务器-服务端
 *
 * @author lizhenzhen
 * @version 1.0
 * @date 2021/4/9 下午3:44
 */
public class TimeServer {

    public static void main(String[] args) {
        int port = 8080;
        if (args != null && args.length > 0){
            try {
                port = Integer.valueOf(args[0]);
            }catch (NumberFormatException e ){
                // 使用默认值
                port = 8080;
            }
        }
        // 创建多路复用器，并启动线程
        MultiplexerTimeServer timeServer = new MultiplexerTimeServer(port);
        new Thread(timeServer, "NIO-MultiplexerTimeServer-001").start();
    }
}
