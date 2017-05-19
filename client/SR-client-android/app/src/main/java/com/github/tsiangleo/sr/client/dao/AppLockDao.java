package com.github.tsiangleo.sr.client.dao;

/**
 * Created by tsiang on 2016/11/29.
 */

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.github.tsiangleo.sr.client.util.DBHelper;

public class AppLockDao
{
    private DBHelper dbHelper;

    public AppLockDao(Context context)
    {
        dbHelper = new DBHelper(context, "com.github.tsiangleo.sr.db", 1);

//        synchronized (AppLockDao.class){
//            if(dbHelper == null) {
//                dbHelper = new DBHelper(context, "com.github.tsiangleo.sr.db", 1);
//            }
//        }
    }

    /**
     * 返回所有的加锁列表.
     * @return
     */
    public List<String> getAllLockList(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> packageNames = new ArrayList<String>();
        if(db.isOpen())
        {
            Cursor cursor = db.rawQuery("select packagename from applocklist", null);
            while(cursor.moveToNext())
            {
                String packageName = cursor.getString(0);
                packageNames.add(packageName);
            }
            cursor.close();
            db.close();
        }
        return packageNames;
    }


    public void insert(String packagename) {
        if(find(packagename)){
            return ;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if(db.isOpen())
        {
            db.execSQL("insert into applocklist (packagename) values (?)", new String[] {packagename});
            db.close();
        }
    }

    public boolean find(String packagename)
    {
        boolean result = false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        if(db.isOpen())
        {
            Cursor cursor = db.rawQuery("select packagename from applocklist where packagename = ? ", new String[] {packagename});
            if(cursor.moveToNext())
            {
                result = true;
            }
            cursor.close();
            db.close();
        }
        return result;
    }

    public void delete(String packagename) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if(db.isOpen()) {
            db.execSQL("delete from applocklist where packagename = ? ", new String[]{packagename});
            db.close();
        }

    }
}
