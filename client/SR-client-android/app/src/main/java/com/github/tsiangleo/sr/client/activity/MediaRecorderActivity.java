package com.github.tsiangleo.sr.client.activity;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.tsiangleo.sr.client.R;

import java.io.File;


public class MediaRecorderActivity extends AppCompatActivity implements View.OnClickListener {

    private File audioFile;
    private Button btnStart,btnStop,btnPlay;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_recorder);

        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop = (Button) findViewById(R.id.btn_stop);
        btnPlay = (Button) findViewById(R.id.btn_play);

        /**
         * invisible：不显示,但保留所占的空间
         visible：正常显示
         gone：不显示,且不保留所占的空间
         */
        btnStart.setVisibility(View.VISIBLE);
        btnStop.setVisibility(View.GONE);
        btnPlay.setVisibility(View.GONE);

        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnPlay.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_start:
                startVoice();
                break;
            case R.id.btn_stop:
                stopVoice();
                break;
            case R.id.btn_play:
                playVoice();
                break;
        }

    }
    private void startVoice(){
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Toast.makeText(this,"SD卡不存在，请插入SD卡!",Toast.LENGTH_SHORT).show();
            return;
        }

        try {

            audioFile = new File(Environment.getExternalStorageDirectory().getCanonicalFile()+ File.separator +System.currentTimeMillis()+".amr");
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());

            btnStart.setVisibility(View.GONE);
            btnStop.setVisibility(View.VISIBLE);
            btnPlay.setVisibility(View.GONE);

            mediaRecorder.prepare();

            //开始录音
            mediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,"录音出错:"+e.getMessage(),Toast.LENGTH_SHORT).show();
        }

        Toast.makeText(this,"录音中....文件名是:"+audioFile.getAbsolutePath(),Toast.LENGTH_SHORT).show();
    }
    private void stopVoice(){
        if(audioFile != null && audioFile.exists()) {
            //停止录音
            mediaRecorder.stop();
            //释放资源
            mediaRecorder.release();
            mediaRecorder = null;

            btnStart.setVisibility(View.GONE);
            btnStop.setVisibility(View.GONE);
            btnPlay.setVisibility(View.VISIBLE);
        }
    }

    private void playVoice(){
        if(audioFile != null && audioFile.exists()) {
            if(mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }
            mediaPlayer.reset();
            try {
                mediaPlayer.setDataSource(audioFile.getAbsolutePath());
                mediaPlayer.prepare();
                //播放
                mediaPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MediaRecorderActivity.this,"播放录音出错:"+e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        }
    }
}
