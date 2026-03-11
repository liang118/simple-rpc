package github.liang118.provider.impl;

import github.liang118.config.RpcServiceConfig;
import github.liang118.enums.RpcErrorMessageEnum;
import github.liang118.enums.ServiceRegistryEnum;
import github.liang118.exception.RpcException;
import github.liang118.extension.ExtensionLoader;
import github.liang118.provider.ServiceProvider;
import github.liang118.remoting.transport.netty.server.NettyRpcServer;
import lombok.extern.slf4j.Slf4j;
import github.liang118.registry.ServiceRegistry;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DefaultServiceProviderImpl implements ServiceProvider {

    /**
     * key: rpc service name [interface + version + group]
     * value: service object
     */
    private final Map<String, Object> serviceMap;//服务端本地用来快速查找服务对象
    private final Set<String> registeredService;
    private final ServiceRegistry serviceRegistry;

    public DefaultServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        registeredService = ConcurrentHashMap.newKeySet();
        // 这里通过spi配置去获取具体要使用java spi加载哪种注册中心
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension(ServiceRegistryEnum.ZK.getName());
    }

    @Override
    public void addService(RpcServiceConfig rpcServiceConfig) {
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        if (registeredService.contains(rpcServiceName)) {
            return;
        }
        registeredService.add(rpcServiceName);
        serviceMap.put(rpcServiceName, rpcServiceConfig.getService());
        log.info("Add service: {} and interfaces:{}", rpcServiceName, rpcServiceConfig.getService().getClass().getInterfaces());
    }

    // 这个方法主要是服务器本地使用，不用将服务发布至注册中心
    @Override
    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {
        try {
            // 首先获取本地的网络信息
            String host = InetAddress.getLocalHost().getHostAddress();
            this.addService(rpcServiceConfig);
            // 将本机的服务发布至注册中心
            serviceRegistry.registerService(rpcServiceConfig.getRpcServiceName(), new InetSocketAddress(host, NettyRpcServer.PORT));
        } catch (UnknownHostException e) {
            log.error("occur exception when getHostAddress", e);
        }
    }
}
