package github.liang118.remoting.transport.netty.server;

import github.liang118.config.CustomShutdownHook;
import github.liang118.config.RpcServiceConfig;
import github.liang118.remoting.transport.netty.codec.RpcMessageDecoder;
import github.liang118.remoting.transport.netty.codec.RpcMessageEncoder;
import github.liang118.remoting.transport.netty.handler.NettyRpcServerHandler;
import github.liang118.utils.RuntimeUtil;
import github.liang118.utils.ThreadPoolFactoryUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * 服务端运行，接受客户端的请求，并在本地执行，返回结果
 * 1. 启动服务前，注册服务
 * 2. 启动服务还是监听请求
 * 3. 关闭的时候，基础已注册的服务 todo 如果移除的时候失败了，如何保证服务会被下线？考虑新增心跳机制
 */
@Slf4j
public class NettyRpcServer {

    public static final int PORT = 9999;

    public void registerService(RpcServiceConfig rpcServiceConfig) {
        // TODO 服务启动的时候注册到注册中心
    }

    public static void main(String[] args) {
        NettyRpcServer server = new NettyRpcServer();
        server.start();
    }

    @SneakyThrows
    public void start() {
        // 注册服务器关闭的回调钩子函数
        CustomShutdownHook.getCustomShutdownHook().clearAll();
        // 启动服务监听
        String host = InetAddress.getLocalHost().getHostAddress();
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                RuntimeUtil.cpus(),
                ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false)
        );
        try {
            ServerBootstrap sb = new ServerBootstrap();
            sb.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。
                    // TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY, false)
                    // 是否开启 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // 当客户端第一次进行请求的时候才会进行初始化
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            // 这里可供用来处理心跳逻辑
                            pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            // 数据编码
                            pipeline.addLast(new RpcMessageEncoder());
                            // 数据的解码
                            pipeline.addLast(new RpcMessageDecoder());
                            // 入站处理器：使用自定义线程池处理业务逻辑
                            pipeline.addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                        }
                    });
            // 绑定端口，同步等待绑定成功
            // ChannelFuture f = sb.bind(host, PORT).sync(); // 这样回绑定到本地的首选IP地址，客户端通过localhost连接不上
            ChannelFuture f = sb.bind("0.0.0.0", PORT).sync();
            System.out.println("服务正常启动");
            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("occur exception when start server:", e);
        } finally {
            log.error("shutdown bossGroup and workerGroup");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }
    }

}
