package com.github.tsiangleo.sr.client.activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.github.tsiangleo.sr.client.R;

/**
 * Created by tsiang on 2016/11/30.
 */
public class PwdSettingActivity extends BaseActivity implements View.OnClickListener{

    public static final String EXTRA_MESSAGE_RET_RESULT  = "com.github.tsiangleo.sr.PwdSettingActivity.EXTRA_MESSAGE_RET_RESULT";
    public static final String EXTRA_MESSAGE_CREATE_NEW_PWD  = "com.github.tsiangleo.sr.PwdSettingActivity.EXTRA_MESSAGE_CREATE_NEW_PWD";
    private EditText pwd1EditText,pwd2EditText,oldPwdEditText;
    private Button savePwdButton;
    private TextView showTextView;
    private TableRow oldPwdTableRow;

    private boolean needReturnResult;
    private boolean isCreateNewPwd;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pwd_setting);

        oldPwdEditText = (EditText) findViewById(R.id.oldPwdEditText);
        pwd1EditText = (EditText) findViewById(R.id.pwd1EditText);
        pwd2EditText = (EditText) findViewById(R.id.pwd2EditText);
        savePwdButton = (Button) findViewById(R.id.savePwdButton);
        showTextView = (TextView)findViewById(R.id.showTextView);
        oldPwdTableRow = (TableRow) findViewById(R.id.oldPwdTableRow);

        savePwdButton.setOnClickListener(this);
        savePwdButton.setOnTouchListener(this);

        getDataFromIntent();

        if(isCreateNewPwd){
            showTextView.setText("创建密码");
            oldPwdTableRow.setVisibility(View.GONE);
        }else{
            showTextView.setText("修改密码");
            oldPwdTableRow.setVisibility(View.VISIBLE);
        }
    }

    private void getDataFromIntent() {
        // Get the message from the intent
        Intent intent = getIntent();
        needReturnResult = intent.getBooleanExtra(EXTRA_MESSAGE_RET_RESULT,false);
        isCreateNewPwd = intent.getBooleanExtra(EXTRA_MESSAGE_CREATE_NEW_PWD,true);
    }

    @Override
    public void onClick(View v) {
        if(isCreateNewPwd){
            createNewPwd();
        }else{
            updatePwd();
        }

    }

    private void updatePwd() {
        if(oldPwdEditText.getText().toString().isEmpty()){
            Toast.makeText(this,"原始密码不能为空",Toast.LENGTH_LONG).show();
            return;
        }

        if(!oldPwdEditText.getText().toString().equals(dataAccessService.getPwd())){
            Toast.makeText(this,"原始密码错误",Toast.LENGTH_LONG).show();
            return;
        }

        if(pwd1EditText.getText().toString().isEmpty() ||pwd2EditText.getText().toString().isEmpty() ){
            Toast.makeText(this,"新密码不能为空",Toast.LENGTH_LONG).show();
            return;
        }
        if(!pwd1EditText.getText().toString().equals(pwd2EditText.getText().toString())){
            Toast.makeText(this,"两次输入的新密码不一致",Toast.LENGTH_LONG).show();
            return;
        }
        dataAccessService.savePwd(pwd1EditText.getText().toString());
        if(needReturnResult){
            new AlertDialog.Builder(this)
                    .setTitle("消息提示")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setMessage("保存成功")
                    .setCancelable(false)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PwdSettingActivity.this.setResult(RESULT_OK, new Intent());
                            PwdSettingActivity.this.finish();
                        }
                    }).create().show();
        }else {
            showMsgAndCloseActivity("保存成功", this);
        }
    }

    private void createNewPwd(){
        if(pwd1EditText.getText().toString().isEmpty() ||pwd2EditText.getText().toString().isEmpty() ){
            Toast.makeText(this,"密码不能为空",Toast.LENGTH_LONG).show();
            return;
        }
        if(!pwd1EditText.getText().toString().equals(pwd2EditText.getText().toString())){
            Toast.makeText(this,"两次输入的密码不一致",Toast.LENGTH_LONG).show();
            return;
        }
        dataAccessService.savePwd(pwd1EditText.getText().toString());
        if(needReturnResult){
            new AlertDialog.Builder(this)
                    .setTitle("消息提示")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setMessage("保存成功")
                    .setCancelable(false)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PwdSettingActivity.this.setResult(RESULT_OK, new Intent());
                            PwdSettingActivity.this.finish();
                        }
                    }).create().show();
        }else {
            showMsgAndCloseActivity("保存成功", this);
        }

    }
}

