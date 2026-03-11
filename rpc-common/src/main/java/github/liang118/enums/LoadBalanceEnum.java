package github.liang118.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoadBalanceEnum {

    LOADBALANCE("loadBalance"),

    LOADBALANCENEW("loadBalanceNew");

    private final String name;

}
