package github.liang118.loadbalance;

import github.liang118.remoting.dto.RpcRequest;
import github.liang118.utils.CollectionUtil;

import java.util.List;

public abstract class AbstractLoadBalance implements LoadBalance {
    @Override
    public String selectServiceAddress(List<String> serviceAddresses, RpcRequest rpcRequest) {
        // 1. 判空
        if (CollectionUtil.isEmpty(serviceAddresses)) {
            return null;
        }
        // 2. 如果只有一个的情况
        if (serviceAddresses.size() == 1) {
            return serviceAddresses.get(0);
        }
        // 3. 使用钩子函数，进行选择
        return doSelect(serviceAddresses, rpcRequest);
    }

    protected abstract String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest);

}
