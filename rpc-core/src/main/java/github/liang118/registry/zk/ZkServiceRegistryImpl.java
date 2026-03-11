package github.liang118.registry.zk;

import org.apache.curator.framework.CuratorFramework;
import github.liang118.registry.ServiceRegistry;
import github.liang118.registry.zk.utils.CuratorUtils;

import java.net.InetSocketAddress;

public class ZkServiceRegistryImpl implements ServiceRegistry {

    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        CuratorUtils.createPersistentNode(zkClient, servicePath);
    }
}
