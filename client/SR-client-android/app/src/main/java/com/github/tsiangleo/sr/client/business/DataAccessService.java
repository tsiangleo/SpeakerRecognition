package com.github.tsiangleo.sr.client.business;

import android.content.SharedPreferences;


/**
 * 数据访问的业务对象
 * Created by tsiang on 2016/11/26.
 */
public class DataAccessService {
    public static final String KEY_SERVER_IP    = "com.github.tsiangleo.sr.server.ip";
    public static final String KEY_SERVER_PORT  = "com.github.tsiangleo.sr.server.port";
    public static final String KEY_PWD          = "com.github.tsiangleo.sr.pwd";

    private SharedPreferences  preferences;
    private SharedPreferences.Editor editor;

   public DataAccessService(SharedPreferences  preferences){
       this.preferences =  preferences;
       this.editor = preferences.edit();
   }

    /**
     * 保存服务器的地址到本地
     * @param ip
     * @param port
     */
    public void saveServerAddr(String ip, int port) {
        editor.putString(KEY_SERVER_IP,ip);
        editor.putInt(KEY_SERVER_PORT,port);
        editor.commit();
    }

    /**
     * 获取服务的ip地址
     * @return
     */
    public String getServerIP(){
        return preferences.getString(KEY_SERVER_IP,null);
    }
    /**
     * 获取服务的ip地址
     * @return
     */
    public int getServerPort(){
        return preferences.getInt(KEY_SERVER_PORT,0);
    }


    /**
     * 保存密码
     * @param pwd
     */
    public void savePwd(String pwd) {
        editor.putString(KEY_PWD,pwd);
        editor.commit();
    }

    /**
     * 获取密码
     */
    public String getPwd() {
        return preferences.getString(KEY_PWD,null);
    }
}
