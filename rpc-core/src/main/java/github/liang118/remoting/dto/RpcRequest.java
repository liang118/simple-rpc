package github.liang118.remoting.dto;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1905122041950251207L;
    private String requestId;
    // 接口详情
    private String interfaceName;
    private String methodName;
    private Object[] parameters;
    private Class<?>[] paramTypes;
    // 接口版本信息
    private String version;
    private String group;

    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }

}
