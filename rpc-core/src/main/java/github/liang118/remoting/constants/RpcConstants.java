package github.liang118.remoting.constants;

public class RpcConstants {

    /**
     * Magic number. Verify RpcMessage
     */
    public static final byte[] MAGIC_NUMBER = {(byte) 'g', (byte) 'r', (byte) 'p', (byte) 'c'};
    public static final byte VERSION = 1;
    // 协议头大小
    public static final int HEAD_LENGTH = 16;

    public static final int MAX_FRAME_LENGTH = 8 * 1024 * 1024;

}
