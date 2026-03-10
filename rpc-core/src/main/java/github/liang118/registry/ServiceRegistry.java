package github.liang118.registry;

import java.net.InetSocketAddress;

public interface ServiceRegistry {

    void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress);

}
