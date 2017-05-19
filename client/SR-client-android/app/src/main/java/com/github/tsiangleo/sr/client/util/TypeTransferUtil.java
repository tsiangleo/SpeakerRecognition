package com.github.tsiangleo.sr.client.util;


import java.nio.ByteBuffer;

/**
 * 全是大端序列
 * Created by tsiang on 2016/11/26.
 */
public class TypeTransferUtil {

    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        // 由高位到低位
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }

    public static int byteArrayToInt(byte[] bytes) {
        int value = 0;
        // 由高位到低位
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (bytes[i] & 0x000000FF) << shift;// 往高位游
        }
        return value;
    }

    public static byte[] longToByteArray(long i) {
        return ByteBuffer.allocate(8).putLong(i).array();
    }

    public static long byteArrayToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).asLongBuffer().get();
    }

    public static byte[] shortToByteArray(short i) {
        return ByteBuffer.allocate(2).putShort(i).array();
    }

    public static short byteArrayToShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).asShortBuffer().get();
    }

    public static void main(String[] args) {
        //

        byte[] b = shortToByteArray((short)511);
        for(int i = 0;i<b.length;i++){
            System.out.println("b["+i+"]="+b[i]);
        }


        System.out.println(byteArrayToShort(b));
    }
}
