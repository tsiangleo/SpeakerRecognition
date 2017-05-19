package com.github.tsiangleo.sr.client.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.github.tsiangleo.sr.client.R;
import com.github.tsiangleo.sr.client.service.WatchDogService;
import com.github.tsiangleo.sr.client.util.ServiceAliveUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tsiang on 2016/11/26.
 */

public class HomeActivity extends BaseActivity {

    private static final String TAG = "HomeActivity" ;
    private int[] imageIds = new int[]{R.drawable.regist,R.drawable.verify,R.drawable.dld,R.drawable.setting,R.drawable.setting};
    private String[] titles = new String[]{"上传说话人语音","识别说话人语音","更新模型库","服务器地址设置","选择文件识别"};
    private int arrowId = R.drawable.arrow;

    private void requestPromission() {
        new AlertDialog.Builder(this).
                setTitle("设置").
                setMessage("开启usagestats权限")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        startActivity(intent);
                        //finish();
                    }
                }).show();
    }

    private boolean hasPackageUsageStatsPermission(){
        if (Build.VERSION.SDK_INT > 20) {
            long ts = System.currentTimeMillis();
            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            List queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, ts - 2000, ts);
            if(queryUsageStats == null || queryUsageStats.isEmpty()){
               return false;
            }
            return true;
        }
        return true;
    }
    private void checkPermission(){
        requestReadExternalPermission();
        requestRecordAudioPermission();

//        if(!hasPackageUsageStatsPermission()){
//            requestPromission();
//        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        SimpleAdapter adapter = new SimpleAdapter(this,getData(),R.layout.activity_home_listview,
                new String[]{"header","title","arrow"},
                new int[]{R.id.header,R.id.title,R.id.arrow});

        ListView listView = (ListView)findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0 ){
                    gotoRegist();
                }else if(position == 1){
                    gotoVerify();
                }else if(position == 2){
//                    gotoAppLock();
                    gotoGetModelFile();
                }else if(position == 3){

                    gotoSettingServer();
                }else if(position == 4){

                    gotoFileChoose();
                }

            }
        });
        listView.setAdapter(adapter);

        //启动授权页面，需要用户授权
        checkPermission();
    }



    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
        for(int i = 0;i<imageIds.length;i++) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("header", imageIds[i]);
            item.put("title", titles[i]);
            item.put("arrow",arrowId);
            listItems.add(item);
        }
        return listItems;
    }

    private void gotoSetting() {

        Intent intent = new Intent(this,SettingActivity.class);
//        Intent intent = new Intent(this,ServerSettingActivity.class);
        startActivity(intent);
    }
    private void gotoFileChoose() {
        Intent intent = new Intent(this,FileChooseActivity.class);
        startActivity(intent);
    }

    private void gotoGetModelFile(){
        new AlertDialog.Builder(this)
                .setTitle("更新模型库")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setMessage("确定要更新模型库吗")
                .setCancelable(true)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        Intent intent = new Intent(HomeActivity.this,VerifyVoiceActivity.class);
//                        intent.putExtra(VerifyVoiceActivity.EXTRA_MESSAGE_RET_RESULT,true);
//                        startActivityForResult(intent ,ACTIVITY_REQUEST_CODE_VERIFY_VOICE);
                        ProgressDialog progressDialog = new ProgressDialog(HomeActivity.this);
                        progressDialog.setTitle("操作提示");
                        progressDialog.setMessage("正在更新模型库...");
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        progressDialog.setCancelable(true);
                        progressDialog.show();
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(HomeActivity.this,HomeActivity.class);
                startActivity(intent);
            }
        }).create().show();
    }

    private void gotoSettingServer() {

//        Intent intent = new Intent(this,SettingActivity.class);
        Intent intent = new Intent(this,ServerSettingActivity.class);
        startActivity(intent);
    }

    private void gotoVerify() {
        Intent intent = new Intent(this,VerifyVoiceActivity.class);
        startActivity(intent);
    }

    private void gotoRegist() {
        Intent intent = new Intent(this,RegistVoiceActivity.class);
        startActivity(intent);
    }

    private void gotoAppLock(){
        checkBeforeGotoAppLock();
//        Intent intent = new Intent(this,AppListActivity.class);
//        startActivity(intent);
    }

    public final static int ACTIVITY_REQUEST_CODE_PWD_SETTING = 1;
    public final static int ACTIVITY_REQUEST_CODE_ENTER_PWD = 2;
    public final static int ACTIVITY_REQUEST_CODE_VERIFY_VOICE = 3;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTIVITY_REQUEST_CODE_PWD_SETTING :
               if(resultCode == RESULT_OK){
                    startActivity(new Intent(this,AppListActivity.class));
               }
                break;
            case ACTIVITY_REQUEST_CODE_ENTER_PWD :
                if(resultCode == RESULT_OK){
                    startActivity(new Intent(this,AppListActivity.class));
                }
                break;
            case ACTIVITY_REQUEST_CODE_VERIFY_VOICE :
                if(resultCode == RESULT_OK){
                    startActivity(new Intent(this,AppListActivity.class));
                }
                break;
            default:
                break;
        }
    }

    private void checkBeforeGotoAppLock(){
        //用户还未设置密码
        if(dataAccessService.getPwd() == null){
            new AlertDialog.Builder(this)
                    .setTitle("消息提示")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setMessage("还没设置密码哦，先去设置密码吧！")
                    .setCancelable(true)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(HomeActivity.this,PwdSettingActivity.class);
                            intent.putExtra(PwdSettingActivity.EXTRA_MESSAGE_RET_RESULT,true);
                            startActivityForResult(intent ,ACTIVITY_REQUEST_CODE_PWD_SETTING);
                            dialog.dismiss();
                        }
                    }).create().show();
        }
        else{ //用户核身
            new AlertDialog.Builder(this)
                    .setTitle("系统需要验证您的身份")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setMessage("请选择验证方式")
                    .setCancelable(true)
                    .setPositiveButton("声纹验证", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(HomeActivity.this,VerifyVoiceActivity.class);
                            intent.putExtra(VerifyVoiceActivity.EXTRA_MESSAGE_RET_RESULT,true);
                            startActivityForResult(intent ,ACTIVITY_REQUEST_CODE_VERIFY_VOICE);
                        }
                    }).setNegativeButton("密码验证", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(HomeActivity.this,EnterPwdActivity.class);
                            intent.putExtra(EnterPwdActivity.EXTRA_MESSAGE_RET_RESULT,true);
                            startActivityForResult(intent ,ACTIVITY_REQUEST_CODE_ENTER_PWD);
                        }
                    }).create().show();
        }
    }

    /**
     * update 04-05　从SD卡上读取需要用户授权。
     * http://blog.csdn.net/likesidehu/article/details/52668879
     *
     */
    @SuppressLint("NewApi")
    private void requestReadExternalPermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ_EXTERNAL_STORAGE permission IS NOT granted...");

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);

        } else {
            Log.d(TAG, "READ_EXTERNAL_STORAGE permission is granted...");
        }
    }
    @SuppressLint("NewApi")
    private void requestRecordAudioPermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "RECORD_AUDIO permission IS NOT granted...");

            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);

        } else {
            Log.d(TAG, "RECORD_AUDIO permission is granted...");
        }
    }

    /**
     * update 04-05
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d(TAG, "requestCode=" + requestCode + "; --->" + permissions.toString()
                + "; grantResult=" + grantResults.toString());
        switch (requestCode) {
            case 0: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted
                    // request successfully, handle you transactions

                } else {

                    // permission denied
                    // request failed
                }

                return;
            }
            default:
                break;

        }
    }
}
