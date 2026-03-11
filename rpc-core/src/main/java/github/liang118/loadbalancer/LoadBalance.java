package github.liang118.loadbalancer;

import github.liang118.remoting.dto.RpcRequest;

import java.util.List;

public interface LoadBalance {

    String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest);

}
