package com.github.tsiangleo.sr.client.activity;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.tsiangleo.sr.client.R;
import com.github.tsiangleo.sr.client.proto.NetHandler;
import com.github.tsiangleo.sr.client.proto.SRRequest;
import com.github.tsiangleo.sr.client.proto.SRResponse;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;

/**
 * Created by tsiang on 2016/11/23.
 *
 */
public class ServerSettingActivity extends BaseActivity implements View.OnClickListener{

    private EditText ipEditText;
    private EditText portEditText;
    private Button saveButton,cancelButton,connectButton;
    private ProgressDialog progressDialog;

    private String host;
    private int port;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_setting);

        ipEditText = (EditText) findViewById(R.id.ipEditText);
        portEditText = (EditText) findViewById(R.id.portEditText);
        saveButton = (Button) findViewById(R.id.saveButton);
        cancelButton = (Button) findViewById(R.id.cancelButton);
        connectButton = (Button) findViewById(R.id.connectButton);

        saveButton.setOnClickListener(this);
        saveButton.setOnTouchListener(this);
        cancelButton.setOnClickListener(this);
        cancelButton.setOnTouchListener(this);
        connectButton.setOnClickListener(this);
        connectButton.setOnTouchListener(this);

        if(dataAccessService.getServerIP() != null){
            ipEditText.setText(dataAccessService.getServerIP());
        }
        if(dataAccessService.getServerPort() > 0){
            //注意：""
            portEditText.setText(""+dataAccessService.getServerPort());
        }
    }


    @Override
    public void onClick(View v) {
        if (v == connectButton){
            if(ipEditText.getText().toString().isEmpty()){
                Toast.makeText(this,"服务器地址不能为空",Toast.LENGTH_LONG).show();
                return;
            }
            if(portEditText.getText().toString().isEmpty()){
                Toast.makeText(this,"服务器端口号不能为空",Toast.LENGTH_LONG).show();
                return;
            }
            new NetCheckTask().execute(ipEditText.getText().toString(),portEditText.getText().toString());
            createProgressDialog();
        }else if (v == saveButton){
            if(ipEditText.getText().toString().isEmpty()){
                Toast.makeText(this,"服务器地址不能为空",Toast.LENGTH_LONG).show();
                return;
            }
            if(portEditText.getText().toString().isEmpty()){
                Toast.makeText(this,"服务器端口号不能为空",Toast.LENGTH_LONG).show();
                return;
            }
            if(host == null){
                host = ipEditText.getText().toString();
            }
            if(port <= 0){
                port = Integer.parseInt(portEditText.getText().toString());
            }
            dataAccessService.saveServerAddr(host,port);
//            Toast.makeText(this, "保存成功", Toast.LENGTH_LONG).show();
            showMsgAndCloseActivity("保存成功",this);

        }else if (v == cancelButton){
            this.finish();
        }
    }

    private class NetCheckTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {

            String h = params[0];
            int p = Integer.parseInt(params[1]);

            boolean isOk = true;

            SRRequest req = new SRRequest();
            req.setRequestId(UUID.randomUUID().toString());
            req.setRequestType(SRRequest.REQUEST_TYPE_PING);
            try {
                Socket client = new Socket(h, p);
                NetHandler netHandler = new NetHandler(client);
                //发送请求
                netHandler.sendRequest(req);
                //读取响应
                SRResponse response = netHandler.readResponse();
                netHandler.close();
                client.close();
             }catch (Exception e){
                isOk = false;
            }finally {
                if(isOk){
                    host = h;
                    port = p;
                }
            }
            return isOk;
        }

        protected void onPostExecute(Boolean result) {
            //关闭进度框
            progressDialog.dismiss();
            if (result) {
//                Toast.makeText(ServerSettingActivity.this, "连接成功", Toast.LENGTH_LONG).show();
                showMsgAndCloseDialog("连接成功");

            } else {
//                Toast.makeText(ServerSettingActivity.this, "连接失败！", Toast.LENGTH_LONG).show();
                showMsgAndCloseDialog("连接失败");
            }
        }
    }

    private void createProgressDialog(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("连通性测试");
        progressDialog.setMessage("正在连接服务器...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(true);
        progressDialog.show();
    }


}
