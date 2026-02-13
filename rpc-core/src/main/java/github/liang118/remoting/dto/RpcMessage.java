package github.liang118.remoting.dto;

import lombok.*;

/**
 * 这个对应协议里面的数据部分
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RpcMessage {

    /**
     * rpc message type
     */
    private byte messageType;
    /**
     * serialization type
     */
    private byte codec;
    /**
     * compress type
     */
    private byte compress;
    /**
     * request id,这里是在编码的时候自动写入的
     */
    private int requestId;
    /**
     * request data
     */
    private Object data;

}
