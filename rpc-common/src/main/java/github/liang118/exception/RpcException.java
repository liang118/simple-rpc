package github.liang118.exception;

public class RpcException extends RuntimeException {

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

}
