package com.github.tsiangleo.sr.client.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;

import com.github.tsiangleo.sr.client.R;
import com.github.tsiangleo.sr.client.service.WatchDogService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tsiang on 2016/11/28.
 */

public class SettingActivity extends BaseActivity {

    private String[] voiceSettingTitles = new String[]{"采样频率设置","声道数设置"};
    private String[] serverSettingTitles = new String[]{"服务器地址设置"};

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        setVoiceListView();
        setServerListView();
        setApplockListView();
        setNumPwdListView();
//        Toast.makeText(this,"isWatchDogServiceRunning:"+isWatchDogServiceRunning(),Toast.LENGTH_SHORT).show();
    }
    private void setNumPwdListView() {
        SimpleAdapter voiceSettingAdapter = new SimpleAdapter(this,getPwdSettingData(), R.layout.activity_setting_listview,
                new String[]{"title"},
                new int[]{R.id.title});

        ListView pwdSetting = (ListView)findViewById(R.id.pwdSetting);
        pwdSetting.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0 ){
                    gotoPwdSetting();
                }

            }
        });
        pwdSetting.setAdapter(voiceSettingAdapter);
    }

    private void setApplockListView() {
        ListView voiceSetting = (ListView)findViewById(R.id.appLockSetting);
        voiceSetting.setAdapter(new AppSettingItemAdapter(this));
    }

    private void setVoiceListView() {
        SimpleAdapter voiceSettingAdapter = new SimpleAdapter(this,getVoiceSettingData(), R.layout.activity_setting_listview,
                new String[]{"title"},
                new int[]{R.id.title});

        ListView voiceSetting = (ListView)findViewById(R.id.voiceSetting);
        voiceSetting.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0 ){
                    gotoSetSapmleRate();
                }else if(position == 1){
                    gotoSetChannel();
                }
            }
        });
        voiceSetting.setAdapter(voiceSettingAdapter);
    }

    private void setServerListView() {
        SimpleAdapter voiceSettingAdapter = new SimpleAdapter(this,getServerSettingData(), R.layout.activity_setting_listview,
                new String[]{"title"},
                new int[]{R.id.title});

        ListView serverSetting = (ListView)findViewById(R.id.serverSetting);
        serverSetting.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0 ){
                    gotoServerSetting();
                }

            }
        });
        serverSetting.setAdapter(voiceSettingAdapter);
    }

    private List<Map<String, Object>> getVoiceSettingData() {
        List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
        for(int i = 0;i<voiceSettingTitles.length;i++) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("title", voiceSettingTitles[i]);
            listItems.add(item);
        }
        return listItems;
    }

    private List<Map<String, Object>> getServerSettingData() {
        List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
        for(int i = 0;i<serverSettingTitles.length;i++) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("title", serverSettingTitles[i]);
            listItems.add(item);
        }
        return listItems;
    }

    private List<Map<String, Object>> getPwdSettingData() {
        List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
        Map<String, Object> item = new HashMap<String, Object>();
        if(dataAccessService.getPwd() == null){
            item.put("title", "创建密码");
        }else{
            item.put("title", "修改密码");
        }
        listItems.add(item);
        return listItems;
    }

    private void gotoServerSetting() {
        // 将一个layout布局文件转为一个view对象。
//        LayoutInflater inflater=(LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View view = inflater.inflate(R.layout.activity_server_setting, null);
//
//        new AlertDialog.Builder(this)
//                .setTitle("服务器地址设置")
//                .setIcon(android.R.drawable.ic_dialog_info)
//                .setCancelable(false)
//                .setView(view)
//                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//
//                        dialog.dismiss();
//                    }
//                }).setNegativeButton("取消", null).create().show();


        Intent intent = new Intent(this,ServerSettingActivity.class);
        startActivity(intent);
    }
    private void gotoPwdSetting(){
        Intent intent = new Intent(this,PwdSettingActivity.class);
        if(dataAccessService.getPwd() == null){
            intent.putExtra(PwdSettingActivity.EXTRA_MESSAGE_CREATE_NEW_PWD,true);
        }else{
            intent.putExtra(PwdSettingActivity.EXTRA_MESSAGE_CREATE_NEW_PWD,false);
        }
        startActivity(intent);
    }
    private void gotoSetChannel() {
        new AlertDialog.Builder(this)
                .setTitle("请选择")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setSingleChoiceItems(new String[] {"单声道(Mono)","双声道(Stereo)"}, 0,
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }
                )
                .setNegativeButton("取消", null)
                .show();
    }

    private void gotoSetSapmleRate(){
        new AlertDialog.Builder(this)
                .setTitle("请选择")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setSingleChoiceItems(new String[] {"44100HZ","22050HZ","11025HZ","8000HZ"}, 0,
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }
                )
                .setNegativeButton("取消", null)
                .show();
    }

    class AppSettingItemViewHolder{
        public TextView appLockSettingTitle;
        public Switch appLockSettingSwitch;
    }

    class AppSettingItemAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private List<Map<String, Object>> mData;

        public AppSettingItemAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
            this.mData = new ArrayList<>();
            initData();
        }

        private void initData(){
            Map<String,Object> map = new HashMap<>();
            map.put("appLockSettingTitle","开启应用锁");
            mData.add(map);
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppSettingItemViewHolder holder;
            if (convertView == null) {
                holder = new AppSettingItemViewHolder();
                convertView = mInflater.inflate(R.layout.activity_setting_applock_item, null);
                holder.appLockSettingTitle = (TextView)convertView.findViewById(R.id.appLockSettingTitle);
                holder.appLockSettingSwitch = (Switch) convertView.findViewById(R.id.appLockSettingSwitch);
                convertView.setTag(holder);
            }else {
                holder = (AppSettingItemViewHolder)convertView.getTag();
            }

            holder.appLockSettingTitle.setText((String)mData.get(position).get("appLockSettingTitle"));
            holder.appLockSettingSwitch.setChecked(isWatchDogServiceRunning());
            holder.appLockSettingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){ /* 按钮由"关闭"到"打开"状态 */
                        openApplockService();
//                        Toast.makeText(SettingActivity.this,"isChecked:true",Toast.LENGTH_SHORT).show();
                    }else{ /* 按钮由"打开"到"关闭"状态 */

                        closeApplockService();
//                        Toast.makeText(SettingActivity.this,"isChecked:false",Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return convertView;
        }
    }

    private void openApplockService(){
        if(!isWatchDogServiceRunning()){
            Intent intent = new Intent(this, WatchDogService.class);
            /*
            Every call to this method will result in a corresponding call to
            the target service's onStartCommand() method,with the intent given here.
             */
            startService(intent);
//            Toast.makeText(this,"start Service",Toast.LENGTH_SHORT).show();
        }
    }
    private void closeApplockService(){
        if(isWatchDogServiceRunning()){
            Intent intent = new Intent(WatchDogService.SR_STOP_LOCK_SERVICE_ACTION);
            sendBroadcast(intent);
//            Toast.makeText(this,"close Service",Toast.LENGTH_SHORT).show();
        }
    }
}

