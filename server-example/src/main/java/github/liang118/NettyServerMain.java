package github.liang118;

import github.liang118.serviceimpl.HelloServiceImpl;
import github.liang118.annotation.RpcScan;
import github.liang118.remoting.transport.netty.server.NettyRpcServer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@RpcScan(basePackage = {"github.liang118"})
public class NettyServerMain {
    public static void main(String[] args) {
        autoRegistry();
    }

    public static void autoRegistry() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(NettyServerMain.class);
        NettyRpcServer nettyRpcServer = (NettyRpcServer) applicationContext.getBean("nettyRpcServer");
        HelloService helloService = applicationContext.getBean(HelloServiceImpl.class);
        helloService.hello(new Hello("本地测试", "你好服务端"));
        nettyRpcServer.start();
    }
}
