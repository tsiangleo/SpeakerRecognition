package com.github.tsiangleo.sr.client.proto;

import java.io.File;

/**
 *
 SR请求协议

 一、ping测试
 1.flag字段：固定为"SRREQ"，占5个字节，req[0]='S',req[1]='R',....
 2.请求id字段：标志本次请求，占36个字节。
 3.type字段：一个字节：type取值含义：1.连通性测试，2为注册声纹，3为验证声纹，4位清除声纹，5为获取声纹模型

 二、注册声纹
 1.flag字段：固定为"SRREQ"，占5个字节，req[0]='S',req[1]='R',....
 2.请求id字段：标志本次请求，占36个字节。
 3.type字段：一个字节：type取值含义：1.连通性测试，2为注册声纹，3为验证声纹，4位清除声纹，5为获取声纹模型
 4.deviceId信息：一个字节的deviceId长度，n个字节的deviceId信息。
 5.文件名信息：1个字节的文件名长度信息，n个字节的文件名信息。（所以文件名的长度不能超过127个字节）。
 6.语音流信息：8个字节的语音流长度信息，n个字节的语音流信息。

 三、验证声纹
 同注册声纹，只是type字段不同。

 四、清除声纹
 1.flag字段：固定为"SRREQ"，占5个字节，req[0]='S',req[1]='R',....
 2.请求id字段：标志本次请求，占36个字节。
 3.type字段：一个字节：type取值含义：1.连通性测试，2为注册声纹，3为验证声纹，4位清除声纹，5为获取声纹模型
 4.deviceId信息：一个字节的deviceId长度，n个字节的deviceId信息。

 五、获取声纹模型
 同清除声纹，只是type字段不同。

 * Created by tsiang on 2017/5/17.
 */

public class SRRequest {

    public static final byte REQUEST_TYPE_PING = 0x01;
    public static final byte REQUEST_TYPE_REGIST = 0x02;
    public static final byte REQUEST_TYPE_VERIFY = 0x03;
    public static final byte REQUEST_TYPE_CLEAR = 0x04;
    public static final byte REQUEST_TYPE_GETMODEL = 0x05;


    private final String magic = "SRRQ";
    private byte requestType;
    private String requestId;
    private String deviceId;
    private File audioFile;

    public String getMagic() {
        return magic;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public File getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(File audioFile) {
        this.audioFile = audioFile;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public byte getRequestType() {
        return requestType;
    }

    public void setRequestType(byte requestType) {
        this.requestType = requestType;
    }

    @Override
    public String toString() {
        return "SRRequest{" +
                "audioFile=" + audioFile +
                ", magic='" + magic + '\'' +
                ", requestType=" + requestType +
                ", requestId='" + requestId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                '}';
    }
}
