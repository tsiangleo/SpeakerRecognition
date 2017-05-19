package com.github.tsiangleo.sr.client.business;

import android.content.Context;

import com.github.tsiangleo.sr.client.dao.AppLockDao;
import com.github.tsiangleo.sr.client.domain.AppInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tsiang on 2016/11/29.
 */

public class AppLockService {

    private AppLockDao appLockDao;
    private AppInfoProvider appInfoProvider;

    public AppLockService(Context context){
        appLockDao = new AppLockDao(context);
        appInfoProvider = new AppInfoProvider(context);
    }
    /**
     * 获取所有的未加锁应用
     * @return
     */
    public List<AppInfo> getAllUnlockApps(){
        List<AppInfo> appInfoList = appInfoProvider.getAllAppInfo();
        List<AppInfo> lockList = getAllLockApps();
        appInfoList.removeAll(lockList);
        return appInfoList;
    }

    /**
     * 获取所有的已加锁应用
     * @return
     */
    public List<AppInfo> getAllLockApps(){
        List<AppInfo> appInfos = new ArrayList<>();
        List<String>  fullNames = appLockDao.getAllLockList();
        if(fullNames != null) {
            for (String fullName:fullNames) {
                AppInfo appInfo = appInfoProvider.getAppInfo(fullName);
                appInfos.add(appInfo);
            }
        }
        return  appInfos;
    }

    /**
     * 将packageName标志的一个应用加入到加锁列表中。
     * @param packageName
     */
    public void addIntoLockList(String packageName){
        appLockDao.insert(packageName);
    }

    /**
     * 将packageName标志的一个应用从加锁列表中移除。
     * @param packageName
     */
    public void removeFromLockList(String packageName){
        appLockDao.delete(packageName);
    }
}
