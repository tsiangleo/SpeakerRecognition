package com.github.tsiangleo.sr.client.proto;

import java.io.File;

/**
 SR响应协议

 一、ping测试
 1.flag字段：固定为"SRRSP"，占5个字节，req[0]='S',req[1]='R',....
 2.响应id字段：标志本次响应，占36个字节。取值为对应的request的id
 3.type字段：一个字节：type取值含义：1.连通性测试，2为注册声纹，3为验证声纹，4位清除声纹，5为获取声纹模型
 4.retCode字段：一个字节；返回码。


 二、注册声纹
 同一

 三、验证声纹
 同一

 四、清除声纹
 同一

 五、获取声纹模型
 1.flag字段：固定为"SRRSP"，占5个字节，req[0]='S',req[1]='R',....
 2.响应id字段：标志本次响应，占36个字节。取值为对应的request的id
 3.type字段：一个字节：type取值含义：1.连通性测试，2为注册声纹，3为验证声纹，4位清除声纹，5为获取声纹模型
 4.retCode字段：一个字节；返回码。
 5.文件名信息：1个字节的文件名长度信息，n个字节的文件名信息。（所以文件名的长度不能超过127个字节）。
 6.语音流信息：8个字节的语音流长度信息，n个字节的语音流信息。
 *
 * Created by tsiang on 2017/5/17.
 */

public class SRResponse {

    /* 返回码的含义 */
    public static final byte RET_CODE_SUCCESS = 0x01;
    public static final byte RET_CODE_FAIL = 0x02;

    private final String magic = "SRRS";
    private String responseId;
    private byte responseType; //对应SRClientRequest中的请求类型
    private byte retCode;
    private File modelFile;

    public String getMagic() {
        return magic;
    }

    public File getModelFile() {
        return modelFile;
    }

    public void setModelFile(File modelFile) {
        this.modelFile = modelFile;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public byte getResponseType() {
        return responseType;
    }

    public void setResponseType(byte responseType) {
        this.responseType = responseType;
    }

    public byte getRetCode() {
        return retCode;
    }

    public void setRetCode(byte retCode) {
        this.retCode = retCode;
    }

    @Override
    public String toString() {
        return "SRResponse{" +
                "magic='" + magic + '\'' +
                ", responseId='" + responseId + '\'' +
                ", responseType=" + responseType +
                ", retCode=" + retCode +
                ", modelFile=" + modelFile +
                '}';
    }
}
