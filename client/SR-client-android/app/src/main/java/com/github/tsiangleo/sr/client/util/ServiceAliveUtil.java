package com.github.tsiangleo.sr.client.util;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

/**
 * Created by tsiang on 2016/11/30.
 */

public class ServiceAliveUtil {

    public static boolean isServiceRunning(Context context, String serviceName){
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> infos =  am.getRunningServices(100);
        for(ActivityManager.RunningServiceInfo info : infos){
            String name = info.service.getClassName();
            if(serviceName.equals(name)){
                return true;
            }
        }
        return false;
    }


}
