package github.liang118.remoting.transport.netty.handler;

import github.liang118.enums.CompressTypeEnum;
import github.liang118.enums.RpcResponseCodeEnum;
import github.liang118.enums.SerializationTypeEnum;
import github.liang118.factory.SingletonFactory;
import github.liang118.remoting.constants.RpcConstants;
import github.liang118.remoting.dto.RpcMessage;
import github.liang118.remoting.dto.RpcRequest;
import github.liang118.remoting.dto.RpcResponse;
import github.liang118.remoting.handler.RpcRequestHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 定义服务器处理请求的核心逻辑
 */
@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {

    private final RpcRequestHandler rpcRequestHandler;

    public NettyRpcServerHandler() {
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try{
            if(msg instanceof RpcMessage) {
                log.info("server receive msg: [{}] ", msg);
                byte messageType = ((RpcMessage) msg).getMessageType();
                // 开始根据消息类型构造响应
                RpcMessage rpcMessage = new RpcMessage();
                rpcMessage.setCodec(SerializationTypeEnum.HESSIAN.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                // 接受的是发送的ping包，发送pong包
                if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                    rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                    rpcMessage.setData(RpcConstants.PONG);
                } else {
                    // 从rpcMessage中解析出来原始请求
                    RpcRequest rpcRequest = (RpcRequest) ((RpcMessage) msg).getData();
                    // 开始在本地执行请求
                    Object result = rpcRequestHandler.handle(rpcRequest);
                    log.info("server get result: {}", result.toString());
                    rpcMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                        // 所有响应必须关联相同的requestId
                        RpcResponse<Object> rpcResponse = RpcResponse.success(rpcRequest.getRequestId(), result);
                        rpcMessage.setData(rpcResponse);
                    } else {
                        RpcResponse<Object> rpcResponse = RpcResponse.fail(rpcRequest.getRequestId(), RpcResponseCodeEnum.FAIL);
                        rpcMessage.setData(rpcResponse);
                        log.error("not writable now, message dropped");
                    }
                }
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } finally {
            // TODO 这里其实是多余的了，ByteBuff才需要释放的
            ReferenceCountUtil.release(msg);
        }
    }
}
