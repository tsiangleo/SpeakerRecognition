package com.github.tsiangleo.sr.client.activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;

public class MainActivity extends BaseActivity{
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
            finish();
        }
        if (KeyEvent.KEYCODE_HOME == event.getKeyCode()) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void requestPromission() {
        new AlertDialog.Builder(this).
                setTitle("设置").
                setMessage("开启usagestats权限")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        startActivity(intent);
                        //finish();
                    }
                }).show();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPromission();

        Intent intent = new Intent(this,HomeActivity.class);
        startActivity(intent);
    }
}
