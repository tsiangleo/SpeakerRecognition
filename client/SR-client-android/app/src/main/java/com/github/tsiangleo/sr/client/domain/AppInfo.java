package com.github.tsiangleo.sr.client.domain;

import android.graphics.drawable.Drawable;

/**
 * Created by tsiang on 2016/11/29.
 */

public class AppInfo {
    private Drawable icon;
    private String appName;
    private String packageName;
    private boolean isSystemApp;

    public Drawable getIcon()
    {
        return icon;
    }
    public void setIcon(Drawable icon)
    {
        this.icon = icon;
    }
    public String getAppName()
    {
        return appName;
    }
    public void setAppName(String appName)
    {
        this.appName = appName;
    }
    public String getPackageName()
    {
        return packageName;
    }
    public void setPackageName(String packageName)
    {
        this.packageName = packageName;
    }
    public boolean isSystemApp()
    {
        return isSystemApp;
    }
    public void setSystemApp(boolean isSystemApp)
    {
        this.isSystemApp = isSystemApp;
    }


    @Override
    public String toString() {
        return "AppInfo{" +
                "icon=" + icon +
                ", appName='" + appName + '\'' +
                ", packageName='" + packageName + '\'' +
                ", isSystemApp=" + isSystemApp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AppInfo appInfo = (AppInfo) o;

        if (appName != null ? !appName.equals(appInfo.appName) : appInfo.appName != null)
            return false;
        return packageName != null ? packageName.equals(appInfo.packageName) : appInfo.packageName == null;

    }

    @Override
    public int hashCode() {
        int result = appName != null ? appName.hashCode() : 0;
        result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
        return result;
    }
}
