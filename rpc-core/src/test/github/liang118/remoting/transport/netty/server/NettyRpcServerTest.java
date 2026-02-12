package github.liang118.remoting.transport.netty.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NettyRpcServerTest {

    @org.junit.jupiter.api.Test
    void start() {
        NettyRpcServer server = new NettyRpcServer();

        // 异步启动服务器
        Thread serverThread = new Thread(server::start);
        serverThread.start();

        // 等待服务器启动完成（最多等待10秒）
        await().atMost(10, java.util.concurrent.TimeUnit.SECONDS)
                .until(() -> isServerListening(NettyRpcServer.PORT));

        // 验证服务器是否成功启动
        assertTrue(isServerListening(NettyRpcServer.PORT), "Server should be listening on port " + NettyRpcServer.PORT);

        // 关闭服务器(可选)
        // serverThread.interrupt();
    }

    private boolean isServerListening(int port) {
        System.out.println("Checking if server is listening on port: " + port);
        try {
            NioEventLoopGroup group = new NioEventLoopGroup();
            ChannelFuture future = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelActive(ChannelHandlerContext ctx) {
                            ctx.close(); // 连接成功后立即关闭
                        }
                    })
                    .connect(new InetSocketAddress("localhost", port))
                    .sync();

            boolean isConnected = future.isSuccess();
            System.out.println("Connection result: " + isConnected);
            group.shutdownGracefully();
            return isConnected;
        } catch (Exception e) {
            System.err.println("Error while checking server status: " + e.getMessage());
            return false;
        }
    }

}