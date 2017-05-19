package com.github.tsiangleo.sr.client.service;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.github.tsiangleo.sr.client.R;
import com.github.tsiangleo.sr.client.activity.EnterPwdActivity;
import com.github.tsiangleo.sr.client.activity.SettingActivity;
import com.github.tsiangleo.sr.client.dao.AppLockDao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by tsiang on 2016/11/29.
 */

public class WatchDogService extends IntentService {
    public static final String SR_UNCHECKED_ACTION = "com.github.tsiangleo.sr.action.UNCHECKED";
    public static final String SR_STOP_LOCK_SERVICE_ACTION = "com.github.tsiangleo.sr.action.STOP_LOCK_SERVICE";
    private Context context;
    private Set<String> unCheckedPackageNameList;
    private UnCheckedReceiver receiver;
    private AppLockDao appLockDao;
    //
    private boolean runningFlag = true;

    public WatchDogService() {
        super("WatchDogService");
        context = this;
        appLockDao = new AppLockDao(context);
        unCheckedPackageNameList = new HashSet<>();
    }
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());

        receiver = new UnCheckedReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SR_UNCHECKED_ACTION);
        filter.addAction(SR_STOP_LOCK_SERVICE_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, filter);

        runInForeground();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStart(intent, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        stopForeground(true);
    }

    private void showMsg(final String msg){
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        while(runningFlag) {
            String packageName =  getCurrentActivityPackageName();
            if(packageName != null) {
                // 获取最近打开的App包名
                boolean isLockApp = appLockDao.find(packageName);
//                showMsg("Toast from WachDogService: \n appLockDao.find(packageName): packageName is: " + packageName+", result is:"+isLockApp);
                // 说明是加锁的程序
                if (isLockApp) {
                    if (! unCheckedPackageNameList.contains(packageName)) {
                        Intent intent2 = new Intent(context, EnterPwdActivity.class);
                        intent2.putExtra("packageName", packageName);// TODO：这一行不加，就没有办法去临时取消保护了！！！
                        intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent2);
                    }
                }
            }
            SystemClock.sleep(300);
        }
//        showMsg("Toast from WachDogService: shuting down WachDogService!");

    }

    public  class UnCheckedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getAction().equals(SR_UNCHECKED_ACTION)) {
                    if(intent.getStringExtra("packageName") != null) {
                        unCheckedPackageNameList.add(intent.getStringExtra("packageName"));
                    }
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    //熄屏以后再次打开时，重新输入密码。
                    unCheckedPackageNameList.clear();
                } else if (intent.getAction().equals(SR_STOP_LOCK_SERVICE_ACTION)) {
                    /* 关闭service*/
                    runningFlag = false;
                }
            }
        }
    }

    /**
     * 获取当前运行的栈顶Activity的PackageName。
     * @return
     */
    private String getCurrentActivityPackageName() {

        if (Build.VERSION.SDK_INT > 20) {
            UsageStatsManager usageStatsManager = (UsageStatsManager)getApplicationContext()
                    .getSystemService(Context.USAGE_STATS_SERVICE);

            long ts = System.currentTimeMillis();
            List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST,ts-2000, ts);
            if (queryUsageStats == null || queryUsageStats.isEmpty()) {
                return null;
            }

            UsageStats recentStats = null;
            for (UsageStats usageStats : queryUsageStats) {
                if (recentStats == null ||
                        recentStats.getLastTimeUsed() < usageStats.getLastTimeUsed()) {
                    recentStats = usageStats;
                }
            }
            return recentStats.getPackageName();
        } else{
            // 5.0之前
            // 获取正在运行的任务栈(一个应用程序占用一个任务栈) 最近使用的任务栈会在最前面
            // 1表示给集合设置的最大容量 List<RunningTaskInfo> infos = am.getRunningTasks(1);
            // 获取最近运行的任务栈中的栈顶Activity(即用户当前操作的activity)的包名
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> appTasks = activityManager.getRunningTasks(1);
            if (null != appTasks && !appTasks.isEmpty()) {
                return appTasks.get(0).topActivity.getPackageName();
            }
            return null;
        }
    }

    private void runInForeground(){
        Intent notificationIntent = new Intent(this, SettingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new Notification.Builder(this)
                .setTicker("正在开启应用锁")
                .setContentTitle("应用锁已开启")
                .setContentText("正在保护您的应用")
                .setSmallIcon(R.drawable.sr)
                .setOngoing(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(524, notification);
    }
}
