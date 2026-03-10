package github.liang118.annotation;

import java.lang.annotation.*;

@Documented
@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcReference {

    // 区分服务接口的版本
    String version() default "";

    //区分服务的不通分组
    String group() default "";

}
