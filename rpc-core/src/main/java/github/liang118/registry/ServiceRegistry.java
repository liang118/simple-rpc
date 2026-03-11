package github.liang118.registry;

import github.liang118.extension.SPI;

import java.net.InetSocketAddress;

@SPI
public interface ServiceRegistry {

    void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress);

}
