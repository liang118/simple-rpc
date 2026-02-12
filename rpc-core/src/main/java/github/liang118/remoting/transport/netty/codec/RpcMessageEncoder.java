package github.liang118.remoting.transport.netty.codec;

import github.liang118.compress.Compress;
import github.liang118.enums.CompressTypeEnum;
import github.liang118.extension.ExtensionLoader;
import github.liang118.remoting.constants.RpcConstants;
import github.liang118.enums.SerializationTypeEnum;
import github.liang118.remoting.dto.RpcMessage;
import github.liang118.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * custom protocol decoder
 * <pre>
 *   0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
 *   +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+-------+
 *   |   magic   code        |version | full length         | messageType| codec|compress|    RequestId       |
 *   +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 *   |                                                                                                       |
 *   |                                         body                                                          |
 *   |                                                                                                       |
 *   |                                        ... ...                                                        |
 *   +-------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B codec（序列化类型） 1B compress（压缩类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 * */
@Slf4j
public class RpcMessageEncoder extends MessageToByteEncoder<RpcMessage> {

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    /**
     * 服务端发送数据前对业务数据进行编码
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage rpcMessage, ByteBuf out) throws Exception {
        try {
            // 这里将rpcMessage封装成自定义协议格式
            // 1. 构造协议头
            out.writeBytes(RpcConstants.MAGIC_NUMBER);
            out.writeByte(RpcConstants.VERSION);
            // 先预留4个字节写full length
            out.writerIndex(out.writerIndex() + 4);
            byte messageType = rpcMessage.getMessageType();
            out.writeByte(messageType);
            byte codec = rpcMessage.getCodec();
            out.writeByte(codec);
            byte compress = rpcMessage.getCompress();
            out.writeByte(compress);
            // 请求ID
            out.writeInt(ATOMIC_INTEGER.getAndIncrement());

            // 2. 构造body数据
            byte[] bodyBytes = null;
            int fullLength = RpcConstants.HEAD_LENGTH;
            // 如果messageType不是心跳类型，fullLength = head + body
            // 如果是心跳类型，不需要用到fullLength，body为空即可
            if (messageType != RpcConstants.HEARTBEAT_REQUEST_TYPE
                    && messageType != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                // 序列化body数据
                String codecName = SerializationTypeEnum.getName(codec);
                log.info("codec name: [{}] ", codecName);
                // 通一通过SPI机制获取序列化器以及压缩器
                Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class)
                        .getExtension(codecName);
                bodyBytes = serializer.serialize(rpcMessage.getData());

                // 压缩序列化后的数据
                String compressName = CompressTypeEnum.getName(compress);
                Compress compressImpl = ExtensionLoader.getExtensionLoader(Compress.class)
                        .getExtension(compressName);
                bodyBytes = compressImpl.compress(bodyBytes);
                // 至此计算出fullLength
                fullLength += bodyBytes.length;
            }
            if(bodyBytes != null) {
                out.writeBytes(bodyBytes);
            }

            // 回填fullLength字段
            int writeIndex = out.writerIndex();
            out.writerIndex(writeIndex - fullLength + RpcConstants.MAGIC_NUMBER.length + 1);
            out.writeInt(fullLength);
            out.writerIndex(writeIndex);

        } catch (Exception e) {
            log.error("Encode request error!", e);
        }
    }

}
