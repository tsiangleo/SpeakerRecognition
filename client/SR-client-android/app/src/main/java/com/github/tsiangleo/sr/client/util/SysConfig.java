package com.github.tsiangleo.sr.client.util;

import android.os.Build;

/**
 * Created by tsiang on 2016/11/26.
 */

public class SysConfig {
    /**
     * 获取Android设置唯一id
     * @return
     */
    public static String getDeviceId(){
        return Build.SERIAL;
    }

}
