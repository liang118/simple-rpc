package github.liang118.spring;

import github.liang118.annotation.RpcReference;
import github.liang118.annotation.RpcService;
import github.liang118.config.RpcServiceConfig;
import github.liang118.enums.RpcRequestTransportEnum;
import github.liang118.extension.ExtensionLoader;
import github.liang118.factory.SingletonFactory;
import github.liang118.provider.ServiceProvider;
import github.liang118.provider.impl.DefaultServiceProviderImpl;
import github.liang118.proxy.RpcClientProxy;
import github.liang118.remoting.RpcRequestTransport;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * 1. 服务发布
 * 2. 服务代理
 */
@Slf4j
@Component
public class SpringBeanPostProcessor implements BeanPostProcessor {

    private final ServiceProvider serviceProvider;
    // 这个除了使用zk，也可以灵活切换为其他注册中心实现
    private final RpcRequestTransport rpcClient;

    public SpringBeanPostProcessor() {
        this.serviceProvider = SingletonFactory.getInstance(DefaultServiceProviderImpl.class);
        // 这里使用的是Dubbo的SPI机制，没有使用Java原生SPI
        // 优点：缓存、可按需加载、支持按名获取
        this.rpcClient = ExtensionLoader.getExtensionLoader(RpcRequestTransport.class).getExtension(RpcRequestTransportEnum.NETTY.getName());
    }

    @SneakyThrows
    @Override
    public @Nullable Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean.getClass().isAnnotationPresent(RpcService.class)) {
            log.info("[{}] is annotated with  [{}]", bean.getClass().getName(), RpcService.class.getCanonicalName());
            // get RpcService annotation
            RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
            // build RpcServiceProperties
            RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                    .group(rpcService.group())
                    .version(rpcService.version())
                    .service(bean).build();
            // 本地服务记录&服务注册至注册中心
            serviceProvider.publishService(rpcServiceConfig);
        }
        return bean;
    }

    @SneakyThrows
    @Override
    public @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        // 获取类的所有方法属性
        Field[] declaredFields = targetClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            // 1.遍历所有的代理对象
            RpcReference rpcReference = declaredField.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                // 2. 针对含有rpcReference注解的示例进行处理
                RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                        .group(rpcReference.group())
                        .version(rpcReference.version()).build();
                // 3. 创建代理对象
                RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient, rpcServiceConfig);
                Object clientProxy = rpcClientProxy.getProxy(declaredField.getType());
                declaredField.setAccessible(true);
                try {
                    // 4. 注入代理对象
                    declaredField.set(bean, clientProxy);
                } catch (IllegalAccessException e) {
                    log.error("occur exception when set field,", e);
                }
            }

        }
        return bean;
    }
}
