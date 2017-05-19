package com.github.tsiangleo.sr.client.activity;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.github.tsiangleo.sr.client.R;
import com.github.tsiangleo.sr.client.business.AppLockService;
import com.github.tsiangleo.sr.client.domain.AppInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tsiang on 2016/11/29.
 */

public class AppListActivity extends TabActivity {

    private AppItemAdapter unlockAdaper;
    private List<Map<String, Object>> unlockDataList;
    private AppItemAdapter lockAdaper;
    private List<Map<String, Object>> lockDataList;

    private AppLockService appLockService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appLockService = new AppLockService(this);

        unlockDataList = getUnlockData();
        unlockAdaper = new AppItemAdapter(this,unlockDataList);

        lockDataList = getLockData();
        lockAdaper = new AppItemAdapter(this,lockDataList);


        TabHost tabHost = getTabHost();
        LayoutInflater.from(this).inflate(R.layout.activity_applist,tabHost.getTabContentView(),true);
        //添加第一个标签页
        tabHost.addTab(tabHost.newTabSpec("tab_unlock").setIndicator("未加锁").setContent(R.id.unlockTabLayout));
        //添加第二个标签页
        tabHost.addTab(tabHost.newTabSpec("tab_lock").setIndicator("已加锁").setContent(R.id.lockTabLayout));

        initUnlockListView();

        initLockListView();

        //注册事件监听器，当点击tabwidget的时候，进行响应。
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if("tab_unlock".equals(tabId)){
                    unlockAdaper.notifyDataSetChanged();
                }else if("tab_lock".equals(tabId)){
                    lockAdaper.notifyDataSetChanged();
                }
            }
        });

        Toast.makeText(AppListActivity.this,"已加锁："+lockDataList.size()+"，未加锁："+unlockDataList.size()
                ,Toast.LENGTH_SHORT).show();
    }

    private void initLockListView() {
        ListView lockListView = (ListView)findViewById(R.id.lockAppListView);
        lockListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(AppListActivity.this)
                        .setTitle("消息提示")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setMessage("确定要将“"+lockDataList.get(position).get("appName")+"”从加锁列表中移除吗？")
                        .setCancelable(true)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //更新list
                                unlockDataList.add(lockDataList.get(position));
                                appLockService.removeFromLockList((String)lockDataList.get(position).get("appPackageName"));
                                lockDataList.remove(position);

                                Toast.makeText(AppListActivity.this,"已加锁："+lockDataList.size()+"，未加锁："+unlockDataList.size()
                                        ,Toast.LENGTH_SHORT).show();

                                //通知更新
                                lockAdaper.notifyDataSetChanged();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
            }
        });
        lockListView.setAdapter(lockAdaper);
    }

    private void initUnlockListView() {
        ListView unlockListView = (ListView)findViewById(R.id.unlockAppListView);
        unlockListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(AppListActivity.this)
                        .setTitle("消息提示")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setMessage("确保要将“"+unlockDataList.get(position).get("appName")+"”放进加锁列表中吗？")
                        .setCancelable(true)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //更新list
                                lockDataList.add(unlockDataList.get(position));
                                appLockService.addIntoLockList((String)unlockDataList.get(position).get("appPackageName"));
                                unlockDataList.remove(position);

                                Toast.makeText(AppListActivity.this,"已加锁："+lockDataList.size()+"，未加锁："+unlockDataList.size()
                                        ,Toast.LENGTH_SHORT).show();

                                //通知更新
                                unlockAdaper.notifyDataSetChanged();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
            }
        });
        unlockListView.setAdapter(unlockAdaper);
    }

    private List<Map<String, Object>> getUnlockData() {
        List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
        List<AppInfo> apps = appLockService.getAllUnlockApps();
        if(apps != null) {
            for (AppInfo app: apps) {
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("appIcon", app.getIcon());
                item.put("appName", app.getAppName());
                item.put("appPackageName", app.getPackageName());
                item.put("appOperate", app.getIcon());
                listItems.add(item);
            }
        }
        return listItems;
    }

    private List<Map<String, Object>> getLockData() {
        List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
        List<AppInfo> apps = appLockService.getAllLockApps();
        if(apps != null) {
            for (AppInfo app: apps) {
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("appIcon", app.getIcon());
                item.put("appName", app.getAppName());
                item.put("appPackageName", app.getPackageName());
                item.put("appOperate", app.getIcon());
                listItems.add(item);
            }
        }
        return listItems;
    }

    class ViewHolder{
        public ImageView appIcon;
        public TextView appName;
        public TextView appPackageName;
    }

    class AppItemAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private List<Map<String, Object>> mData;

        public AppItemAdapter(Context context,List<Map<String, Object>> mData) {
            this.mInflater = LayoutInflater.from(context);
            this.mData = mData;
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
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.activity_applist_listview, null);
                holder.appIcon = (ImageView)convertView.findViewById(R.id.appIcon);
                holder.appName = (TextView)convertView.findViewById(R.id.appName);
                holder.appPackageName = (TextView)convertView.findViewById(R.id.appPackageName);
                convertView.setTag(holder);
            }else {
                holder = (ViewHolder)convertView.getTag();
            }

            holder.appIcon.setBackgroundDrawable((Drawable) mData.get(position).get("appIcon"));
            holder.appName.setText((String)mData.get(position).get("appName"));
            holder.appPackageName.setText((String)mData.get(position).get("appPackageName"));
            return convertView;
        }
    }
}
