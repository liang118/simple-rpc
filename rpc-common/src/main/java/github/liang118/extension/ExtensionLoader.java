package github.liang118.extension;

import github.liang118.factory.SingletonFactory;
import github.liang118.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 整体的实现参考了dubbo spi <a href="https://dubbo.apache.org/zh-cn/docs/source_code_guide/dubbo-spi.html">dubbo</>
 *   整体工作流程
 *
 *   1. 获取 ExtensionLoader：通过 getExtensionLoader(type) 获取对应接口的加载器
 *   2. 加载扩展类：从 META-INF/extensions/ 配置文件中读取实现类
 *   3. 创建扩展实例：通过 getExtension(name) 获取对应名称的实例
 *   4. 缓存管理：所有加载的类和实例都会被缓存，避免重复创建
 * @param <T>
 */
@Slf4j
public class ExtensionLoader<T> {

    private static final String SERVICE_DIRECTORY = "META-INF/extensions/";

    /**
     * 扩展器的类型属性
     * */
    private final Class<?> type;

    /**
     * 扩展器缓存，每一个类都有一个扩展器实例
     * */
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();

    /**
     * 类缓存，根据名称进行缓存，从文件中进行读取的key，value
     * */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    /**
     * 实例缓存，key例如：github.javaguide.registry.ServiceRegistry ，value即对应的实例
     * */
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();

    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }


    /**
     * 获取扩展类加载器
     * */
    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type should not be null.");
        }
        if (!type.isInterface()) {
            // 需要是接口，根据接口+配置加载不同的实现
            throw new IllegalArgumentException("Extension type must be an interface.");
        }
        if (type.getAnnotation(SPI.class) == null) {
            // 类上需要包含SPI注解
            throw new IllegalArgumentException("Extension type must be annotated by @SPI");
        }
        // 通过单例模式创建每种type的唯一ExtensionLoader实例
        ExtensionLoader<S> extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        // 这里使用的是双重检测机制，避免同一时刻大量创建
        if (extensionLoader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<S>(type));
            extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        }

        return extensionLoader;
    }

    // 这里使用holder的原因：锁粒度优化，提高性能
    public T getExtension(String name) {
        if (StringUtil.isBlank(name)) {
            throw new IllegalArgumentException("Extension name should not be null or empty.");
        }
        // 创建一个对象，如果没有的情况下，创建一个新的
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }
        // 标准的双重检测锁创建单实例
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    private T createExtension(String name) {
        // 1. 首先获取扩展类加载器
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw new RuntimeException("扩展类不存在:  " + name);
        }
        // 2. 这里还要创建单例，主要是考虑多个key银蛇用一个class的情况下
        // 可能多个线程运行到这里，创建出同一个class的多个实例，违反了单例
        return (T) SingletonFactory.getInstance(clazz);
    }

    // 扩展类就是指定目录下的实现
    private Map<String, Class<?>> getExtensionClasses() {
        // 1. 从缓存中获取所有的类
        Map<String, Class<?>> classes = cachedClasses.get();
        // 2. 缓存中没有，进行双检测锁
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = new HashMap<>();
                    // 3. 从文件夹中加载所有的扩展类
                    loadDirectory(classes);
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    private void loadDirectory(Map<String, Class<?>> extensionClasses) {
        // 1. 构建配置文件的路径
        String fileName = ExtensionLoader.SERVICE_DIRECTORY + type.getName();
        try {
            Enumeration<URL> urls;
            // 2. 类似Java的SPI，扩展类加载器，然后设置文件的URl
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            urls = classLoader.getResources(fileName);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL resourceUrl = urls.nextElement();
                    // 3. 加载并解析
                    loadResource(extensionClasses, classLoader, resourceUrl);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL resourceUrl) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream(), UTF_8))) {
            String line;
            // 读取配置文件的每一行
            while ((line = reader.readLine()) != null) {
                // 1. 过滤掉注释
                final int ci = line.indexOf('#');
                if (ci >= 0) {
                    line = line.substring(0, ci);
                }
                // 2. 去掉空格
                line = line.trim();
                if (line.length() > 0) {
                    try {
                        // 3. = 实现的key value对解析，存入到map中
                        final int ei = line.indexOf('=');
                        String name = line.substring(0, ei).trim();
                        String clazzName = line.substring(ei + 1).trim();
                        if (name.length() > 0 && clazzName.length() > 0) {
                            // 4. Java的SPI的具体实现
                            Class<?> clazz = classLoader.loadClass(clazzName);
                            extensionClasses.put(name, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.error(e.getMessage());
                    }
                }

            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

}
