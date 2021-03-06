# 目录结构
```aidl
.
└── com
    └── coderandyli
        └── time                  --- 时间服务器示例
            ├── netty             --- 基于netty的实现
            │   ├── client
            │   └── server        
            └── nio               --- 原生nio实现
                ├── client        --- 客户端
                └── server        --- 服务端
```

# 背景知识
## 几种IO
\ | 同步阻塞IO(BIO) | 伪异步IO | 同步非阻塞IO(NIO) | 异步IO(AIO)
---|---|---|---|---
客户端个数: IO线程 | 1:1 | M:N(其中M可以大于N) | M:1(1个IO线程处理多个客户端连接) | M:0(不需要启动额外的线程，被动回调)
IO类型（阻塞）| 阻塞IO | 阻塞IO | 非阻塞IO | 非阻塞IO
IO类型（同步）| 同步IO | 同步IO | 同步IO（IO多路复用）| 异步IO
调试难度 | 简单 | 简单 | 非常复杂 | 复杂
可靠性 | 非常差 | 差 | 高 | 高 
吞吐量 | 低 | 中 | 高 | 高

### IO多路服用
- 待补充



# 项目介绍
## 时间服务器
### NIO实现方式
- NIO实现方式确实比BIO实现方式的编程难度大一些，而且还没有考虑"半包读"、"半包写"，但NIO有很大的优势
    - 客户端发起的连接是异步的，可以通过西在多路复用器注册OP_CONNECT等待后续结果，不需要像BIO那样被同步阻塞
    - SocketChannel的读写操作都是异步的，如果没有可读写的数据它不会同步等待，直接返回，这样IO通信线程就可以处理其他链路，不需要同步等待这个链路。
    - 线程模型的优化：由于JDK的Selector在Linux等主流操作系统通过epoll实现，它没有连接句柄的限制（只受限于操作系统的最大句柄数或者对单个进程的句柄限制），这就意味着一个Selector可以同时处理成千上万的客户端连接，性能并不会随着客户端的增加而线性下降，因此NIO非常适合做高性能、高负载的网路服务器。