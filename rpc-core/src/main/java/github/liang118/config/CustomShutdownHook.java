package github.liang118.config;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomShutdownHook {

    // 饿汉式单例模式
    private static final CustomShutdownHook CUSTOM_SHUTDOWN_HOOK = new CustomShutdownHook();

    public static CustomShutdownHook getCustomShutdownHook() {
        return CUSTOM_SHUTDOWN_HOOK;
    }

    public void clearAll() {
        log.info("addShutdownHook for clearAll");
        // TODO 服务停止的时候从注册中心移除服务

    }

}
