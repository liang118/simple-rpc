package github.liang118.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public enum RpcErrorMessageEnum {

    SERVICE_CAN_NOT_BE_FOUND("没有找到指定的服务");

    private final String message;

}
