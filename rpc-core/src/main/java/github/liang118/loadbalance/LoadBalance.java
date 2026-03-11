package github.liang118.loadbalance;

import github.liang118.extension.SPI;
import github.liang118.remoting.dto.RpcRequest;

import java.util.List;

@SPI
public interface LoadBalance {

    String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest);

}
