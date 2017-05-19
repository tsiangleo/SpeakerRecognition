package com.github.tsiangleo.sr.client.activity;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.github.tsiangleo.sr.client.R;
import com.github.tsiangleo.sr.client.business.AudioRecordService;
import com.github.tsiangleo.sr.client.business.NetService;
import com.github.tsiangleo.sr.client.util.SysConfig;

import java.io.File;
import java.io.IOException;

/**
 * Created by tsiang on 2016/11/26.
 */

public class RegistVoiceActivity extends BaseActivity implements View.OnClickListener{

    private Button startRecordButton,stopRecordButton,stopConvertButton,stopPlayButton,stopUploadButton;
    private TextView statusTextView,hintTextView;
    private ProgressDialog progressDialog;

    private File rawFile;
    private File wavFile;

    private AudioRecordService audioRecordService;
    private NetService netService;

    private RecordAudioTask recordTask;
    private PlayAudioTask playTask;
    private UploadAudioTask uploadTask;
    private ConvertToWAVTask convertTask;

    private Chronometer chronometer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regist_voice);

        statusTextView = (TextView) findViewById(R.id.statusTextView);
        hintTextView = (TextView) findViewById(R.id.hintTextView);

        startRecordButton = (Button) findViewById(R.id.startRecordButton);
        stopRecordButton = (Button) findViewById(R.id.stopRecordButton);
        stopConvertButton = (Button) findViewById(R.id.stopConvertButton);
        stopPlayButton = (Button) findViewById(R.id.stopPlayButton);
        stopUploadButton = (Button) findViewById(R.id.stopUploadButton);
        chronometer = (Chronometer) findViewById(R.id.chronometer);


        startRecordButton.setOnClickListener(this);
        stopRecordButton.setOnClickListener(this);
        stopConvertButton.setOnTouchListener(this);
        stopPlayButton.setOnTouchListener(this);
        stopUploadButton.setOnTouchListener(this);

        chronometer.setVisibility(View.INVISIBLE);
        statusTextView.setVisibility(View.GONE);
        stopRecordButton.setVisibility(View.GONE);
        stopConvertButton.setVisibility(View.GONE);
        stopPlayButton.setVisibility(View.GONE);
        stopUploadButton.setVisibility(View.GONE);

        netService = new NetService(dataAccessService.getServerIP(),dataAccessService.getServerPort());

    }

    private void initFile(String fileNamePrefix) {

        try {
            rawFile = File.createTempFile(fileNamePrefix, ".pcm",getFilesDir());
            wavFile = File.createTempFile(fileNamePrefix, ".wav",getFilesDir());
        } catch (IOException e) {
//            Toast.makeText(this,"内部存储：文件创建异常："+e.getMessage(),Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        if(rawFile == null || wavFile == null){
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                try {
                    File path = new File( Environment.getExternalStorageDirectory().getCanonicalPath()
                            + "/Android/data/com.github.tsiangleo.sr.client/files/");
                    path.mkdirs();
                    rawFile = File.createTempFile(fileNamePrefix, ".pcm", path);
                    wavFile = File.createTempFile(fileNamePrefix, ".wav", path);
                } catch (IOException e) {
//                    Toast.makeText(this,"SD卡：文件创建异常："+e.getMessage(),Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }

        if(rawFile == null || wavFile == null){
            showMsgAndCloseActivity("无法创建临时文件，请先确保应用具有相应的授权，再使用！",this);
        }
    }


    @Override
    public void onClick(View v) {
        if(v == startRecordButton){
            record();
        }else if(v == stopRecordButton){
            stopRecord();
        }else if (v == stopConvertButton){
            convertTask.cancel(true);
            stopConvertButton.setVisibility(View.GONE);
            startRecordButton.setVisibility(View.VISIBLE);
        }else if (v == stopPlayButton){
            playTask.cancel(true);
            stopPlayButton.setVisibility(View.GONE);
            startRecordButton.setVisibility(View.VISIBLE);

        }else if(v == stopUploadButton){
            uploadTask.cancel(true);
            stopUploadButton.setVisibility(View.GONE);
            startRecordButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 文件名前缀的生成规则
     * @param order 标志第几次
     * @return
     */
    private String getFileNamePrefix(int order){
        return SysConfig.getDeviceId()+"_regist_"+order+"_";
    }

    private void record() {
        /* 每次录音都创建一个新的文件. */
        initFile(getFileNamePrefix(1));
        /* audioRecordService */
        audioRecordService = new AudioRecordService(rawFile);

        startRecordButton.setText("开始录音");
        startRecordButton.setVisibility(View.GONE);
        stopRecordButton.setVisibility(View.VISIBLE);

        statusTextView.setVisibility(View.GONE);
        chronometer.setVisibility(View.VISIBLE);


        recordTask = new RecordAudioTask();
        recordTask.execute();

        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

    }
    private void stopRecord() {
        audioRecordService.stopRecording();
        chronometer.stop();
//        createProgressDialog();
    }

    private void play() {
        playTask = new PlayAudioTask();
        playTask.execute();
    }

    private void upload() {
        uploadTask = new UploadAudioTask();
        uploadTask.execute();
    }
    private void convert() {
        convertTask = new ConvertToWAVTask();
        convertTask.execute();
    }
    private class RecordAudioTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                audioRecordService.startRecording();
            } catch (Exception e) {
                return e.getMessage();
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
            statusTextView.setText("正在录音:"+progress[0].toString());
        }

        // 点击停止录音后由UI线程执行
        protected void onPostExecute(String result) {
            if(result == null) {
                statusTextView.setText("完成录音，文件是:" + getFilePath(rawFile));
                //停止按钮不可见
                stopRecordButton.setVisibility(View.GONE);
                chronometer.setVisibility(View.GONE);
                statusTextView.setVisibility(View.VISIBLE);
                //开始按钮可见，但不出于enable状态，得等到文件上传完成后才能出于enable状态。
                stopConvertButton.setVisibility(View.VISIBLE);

                convert();
//                play();
            }else {
                showMsgAndCloseActivity(result,RegistVoiceActivity.this);
            }
        }


    }

    private class PlayAudioTask extends AsyncTask<Void, Long, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                audioRecordService.startPlaying();
            } catch (Exception e) {
                return e.getMessage();
            }
            return null;
        }

        protected void onProgressUpdate(Long... progress) {
            long total = rawFile.length() / 2;  //以short为单位，2个字节。

            if(total != 0) {
                int percent = (int)((progress[0] / (double)total) * 100);
                statusTextView.setText("正在播放录音:"+percent+"%");
            }
        }

        // 点击停止录音后由UI线程执行
        protected void onPostExecute(String result) {
            if(result != null){
                statusTextView.setText("播放录音出错:"+result);
//                Toast.makeText(RegistVoiceActivity.this,"播放录音出错:"+result,Toast.LENGTH_SHORT).show();
                return;
            }

//            statusTextView.setText("录音播放完成，文件是:"+getFilePath(rawFile));
        }
    }

    private class UploadAudioTask extends AsyncTask<Void, Long, String> {

        /**
         *
         * @param params
         * @return 返回值说明：0成功; 1服务器地址和端口号不符合规范; 2文件上传出错
         */
        @Override
        protected String doInBackground(Void... params) {
            try {
                netService.upload(wavFile);
            } catch (Exception e) {
                return e.getMessage();
            }
            return null;

        }
        protected void onProgressUpdate(Long... progress) {
            long total = wavFile.length();
            if(total != 0) {
                int percent = (int)((progress[0] / (double)total) * 100);
                statusTextView.setText("正在上传文件:"+percent+"%");
            }

        }

        // 点击停止录音后由UI线程执行
        protected void onPostExecute(String result) {

            if(result == null) {
                statusTextView.setText("恭喜！语音文件上传成功~");
                //停止按钮不可见
                stopUploadButton.setVisibility(View.GONE);
                startRecordButton.setVisibility(View.VISIBLE);
            }else {
//                showMsgAndCloseActivity(result,RegistVoiceActivity.this);
                statusTextView.setText("文件上传失败！\n"+result);
                stopUploadButton.setVisibility(View.GONE);
                startRecordButton.setText("重新开始录音");
                startRecordButton.setVisibility(View.VISIBLE);
            }
            deleteFile();
        }
    }

    private String getFilePath(File file){
        try {
           return file.getCanonicalPath();
        } catch (IOException e) {
            return null;
        }
    }

    private class ConvertToWAVTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                audioRecordService.rawToWavFile(wavFile);
            } catch (Exception e) {
                return e.getMessage();
            }
            return null;
        }

        // 点击停止录音后由UI线程执行
        protected void onPostExecute(String result) {
            if(result == null) {
                upload();

                statusTextView.setText("正在上传文件...");
                //停止按钮不可见
                stopConvertButton.setVisibility(View.GONE);
                stopUploadButton.setVisibility(View.VISIBLE);
                createProgressDialog();

            }else {
                showMsgAndCloseActivity("转换出错："+result,RegistVoiceActivity.this);
            }
        }
    }

    // 停止录音时，弹出
    private void createProgressDialog(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("提示");
        progressDialog.setMessage("正在上传语音文件...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    private void deleteFile(){
        rawFile.delete();
        wavFile.delete();
    }
}
