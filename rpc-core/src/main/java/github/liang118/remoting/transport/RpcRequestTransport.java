package github.liang118.remoting.transport;

import github.liang118.extension.SPI;
import github.liang118.remoting.dto.RpcRequest;

@SPI
public interface RpcRequestTransport {

    Object sendRpcRequest(RpcRequest rpcRequest);

}
