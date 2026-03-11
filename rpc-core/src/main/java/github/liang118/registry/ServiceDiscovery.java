package github.liang118.registry;

import github.liang118.extension.SPI;
import github.liang118.remoting.dto.RpcRequest;

import java.net.InetSocketAddress;

@SPI
public interface ServiceDiscovery {

    InetSocketAddress lookupService(RpcRequest rpcRequest);

}
