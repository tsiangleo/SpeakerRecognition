package com.github.tsiangleo.sr.client.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.github.tsiangleo.sr.client.R;
import com.github.tsiangleo.sr.client.service.WatchDogService;

/**
 * Created by tsiang on 2016/11/30.
 */

public class EnterPwdActivity extends BaseActivity {
    public static final String EXTRA_MESSAGE_RET_RESULT  = "com.github.tsiangleo.sr.EnterPwdActivity.EXTRA_MESSAGE_RET_RESULT";

    private EditText etPwd;
    private Button button;
    private String packageName;

    private boolean needReturnResult;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enterpwd);
        packageName = getIntent().getStringExtra("packageName");

//        Toast.makeText(this,"packageName:"+packageName,Toast.LENGTH_LONG).show();

        etPwd = (EditText) findViewById(R.id.et_pwd);
        button = (Button)findViewById(R.id.btn);

        etPwd.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (hasFocus) {
                    // //显示软键盘//
                    imm.showSoftInputFromInputMethod(v.getWindowToken(), 0);
                } else {
                    // 隐藏软键盘
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = etPwd.getText().toString().trim();
                if (text.equals(EnterPwdActivity.this.dataAccessService.getPwd())) {
                    Intent intent = new Intent(WatchDogService.SR_UNCHECKED_ACTION);
                    intent.putExtra("packageName", packageName);
                    sendBroadcast(intent);
                    if(needReturnResult){
                        setResult(RESULT_OK, new Intent());
                    }
                    finish();
                } else {
                    Toast.makeText(EnterPwdActivity.this, "密码不对", Toast.LENGTH_SHORT).show();
                }
            }
        });

        getDataFromIntent();
    }
    private void getDataFromIntent() {
        // Get the message from the intent
        Intent intent = getIntent();
        needReturnResult = intent.getBooleanExtra(EXTRA_MESSAGE_RET_RESULT,false);
    }
    @Override
    public void onBackPressed() {
        /*
         * <activity android:name="com.android.launcher2.Launcher"
         * android:launchMode="singleTask" android:clearTaskOnLaunch="true"
         * android:stateNotNeeded="true" android:theme="@style/Theme"
         * android:screenOrientation="nosensor"
         * android:windowSoftInputMode="stateUnspecified|adjustPan">
         * <intent-filter> <action android:name="android.intent.action.MAIN" />
         * <category android:name="android.intent.category.HOME" /> <category
         * android:name="android.intent.category.DEFAULT" /> <category
         * android:name="android.intent.category.MONKEY"/> </intent-filter>
         * </activity>
         */
        // 打开桌面
//        Intent intent = new Intent();
//        intent.setAction("android.intent.action.MAIN");
//        intent.addCategory("android.intent.category.HOME");
//        startActivity(intent);

        Intent intent = new Intent();
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addCategory("android.intent.category.MONKEY");
        startActivity(intent);

    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}
