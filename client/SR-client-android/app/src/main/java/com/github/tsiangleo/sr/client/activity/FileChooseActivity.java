package com.github.tsiangleo.sr.client.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.tsiangleo.sr.client.R;
import com.github.tsiangleo.sr.client.business.AudioRecordService;
import com.github.tsiangleo.sr.client.business.NetService;
import com.github.tsiangleo.sr.client.business.RecognitionService;
import com.github.tsiangleo.sr.client.spectrogram.SpectrogramOnAndroid;
import com.github.tsiangleo.sr.client.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by tsiang on 2016/11/26.
 */

public class FileChooseActivity extends BaseActivity implements View.OnClickListener{
    private static final String TAG = "FileChooseActivity";



    private Button fileChooseButton,startRecognitionButton,stopSpectrogramButton,stopRecognitionButton;
    private TextView statusTextView;
    private ImageView imageViewSpectrogram;

    private File rawFile;
    private File wavFile;

    private AudioRecordService audioRecordService;
    private NetService netService;

    private PlayAudioTask playTask;
    private RecognitionTask recognitionTask;
    private SpectrogramTask spectrogramTask;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_choose);

        statusTextView = (TextView) findViewById(R.id.statusTextView);

        imageViewSpectrogram = (ImageView) findViewById(R.id.imageViewSpectrogram);

        fileChooseButton = (Button) findViewById(R.id.fileChooseButton);
        startRecognitionButton = (Button) findViewById(R.id.startRecognitionButton);
        stopSpectrogramButton = (Button) findViewById(R.id.stopSpectrogramButton);
        stopRecognitionButton = (Button) findViewById(R.id.stopRecognitionButton);


        fileChooseButton.setOnClickListener(this);
        startRecognitionButton.setOnClickListener(this);
        stopSpectrogramButton.setOnTouchListener(this);
        stopRecognitionButton.setOnTouchListener(this);

        startRecognitionButton.setVisibility(View.GONE);
        statusTextView.setVisibility(View.GONE);
        stopSpectrogramButton.setVisibility(View.GONE);
        stopRecognitionButton.setVisibility(View.GONE);
        imageViewSpectrogram.setVisibility(View.GONE);

        netService = new NetService(dataAccessService.getServerIP(),dataAccessService.getServerPort());


    }

    @Override
    public void onClick(View v) {
        if(v == fileChooseButton){
            chooseFile();
            fileChooseButton.setText("选择文件");
        }else if(v == startRecognitionButton){
            spectrogram();
            startRecognitionButton.setVisibility(View.GONE);
            stopSpectrogramButton.setVisibility(View.VISIBLE);

        }else if (v == stopSpectrogramButton){
            playTask.cancel(true);
            stopSpectrogramButton.setVisibility(View.GONE);

        }else if(v == stopRecognitionButton){
            recognitionTask.cancel(true);
            stopRecognitionButton.setVisibility(View.GONE);
        }
    }

    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult( Intent.createChooser(intent, "Select a File to Upload"), 1);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1) {
                Uri uri = data.getData();
                Log.i(TAG,"uri:"+uri);

                try {
                    String realPath = FileUtils.getPath(this,uri);
                    Log.i(TAG,"realPath:"+realPath);
                    statusTextView.setVisibility(View.VISIBLE);
                    if(realPath == null) {
                        statusTextView.setText("文件不存在，请重新选择！");
                        fileChooseButton.setText("重新选择文件");
                    }else if(!realPath.endsWith(".wav")) {
                        statusTextView.setText("文件必须是.wav格式的，请重新选择！");
                        fileChooseButton.setText("重新选择文件");
                    }else{
                        wavFile = new File(realPath);
                        statusTextView.setText("当前选择文件:"+realPath);
                        Log.i(TAG,"wavFile:"+wavFile);
                        startRecognitionButton.setVisibility(View.VISIBLE);
                        fileChooseButton.setVisibility(View.GONE);
                    }

                }catch (Exception e){
                    Log.w(TAG,"onActivityResult:"+e);
                }

            }
        }
    }

    private void play() {
        playTask = new PlayAudioTask();
        playTask.execute();
    }

    private void spectrogram() {
        spectrogramTask = new SpectrogramTask();
        spectrogramTask.execute();
    }

    private void recognition() {
        recognitionTask = new RecognitionTask();
        recognitionTask.execute();
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
    private class SpectrogramTask extends AsyncTask<Void, Long, Bitmap> {

        /**
         *
         * @param params
         * @return 返回值说明：0成功; 1服务器地址和端口号不符合规范; 2文件上传出错
         */
        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                return SpectrogramOnAndroid.getBitmap(RecognitionService.FFTLEN,RecognitionService.HOPSIZE,wavFile);
            }catch (Exception e) {
                Log.i(TAG,"SpectrogramTask.doInBackground:"+e);
                return null;
            }
        }

        // 点击停止录音后由UI线程执行
        protected void onPostExecute(Bitmap result) {

            if(result == null) {
                statusTextView.setText("生成语谱图失败!");
                //停止按钮不可见
                stopSpectrogramButton.setVisibility(View.GONE);
            }else {
                imageViewSpectrogram.setVisibility(View.VISIBLE);
                imageViewSpectrogram.setImageBitmap(result);
                statusTextView.setText("生成语谱图成功！正在识别语谱图...\n");

                stopSpectrogramButton.setVisibility(View.GONE);
                stopRecognitionButton.setVisibility(View.VISIBLE);
                recognition();
            }
        }
    }

    private class RecognitionResult{
        List<float[]> spectrogramPixelList;
        List<float[]> probList; //记录每张语谱图属于各个标签的概率值.
        String status;
        List<Integer> labelList;//每张语谱图所属的标签值
        int[] labelCount; //统计每张语谱图属于各个标签的次数
        int predictLabel; //最后的预测值
    }

    private class RecognitionTask extends AsyncTask<Void, Long, RecognitionResult> {

        private int numSpectrogram;


        @Override
        protected RecognitionResult doInBackground(Void... params) {

            RecognitionResult result = new RecognitionResult();
            try {

                List<float[]>  spectrogramPixelList = SpectrogramOnAndroid.getSlicedPixelList(RecognitionService.FFTLEN,RecognitionService.HOPSIZE,wavFile,RecognitionService.SLICED_WIDTH,RecognitionService.SLICED_HEIGHT);

                RecognitionService recognitionService = new RecognitionService(FileChooseActivity.this);

                recognitionService.init();


                result.spectrogramPixelList = spectrogramPixelList;

                numSpectrogram = spectrogramPixelList.size();

                //记录每张语谱图属于各个标签的概率值.
                List<float[]> probList = new ArrayList<>();
                //记录每张语谱图所属的标签值.
                List<Integer> labelList = new ArrayList<>();
                //进度标志
                Long progress = 1L;
                for (float[] spectrogramPixel : spectrogramPixelList) {

                    float[] results  = recognitionService.recognize(spectrogramPixel);

                    probList.add(results);

                    labelList.add(RecognitionService.argmax(results));

                    publishProgress(progress++);
                }
                result.probList= probList;
                result.labelList = labelList;

                //统计每张语谱图属于各个标签的次数
                int[] labelCount = new int[RecognitionService.NUM_CLASSES];
                for(Integer label : labelList){
                    labelCount[label] += 1;
                }
                result.labelCount = labelCount;
                result.predictLabel = RecognitionService.argmax(labelCount);


            } catch (Exception e) {
                result.status = e.getMessage();
                Log.e(TAG,"RecognitionTask.doInBackground:"+e);
            }
            return result;

        }
        protected void onProgressUpdate(Long... progress) {
            long total = numSpectrogram;
            if(total != 0) {
                int percent = (int)((progress[0] / (double)total) * 100);
                statusTextView.setText("生成语谱图成功！正在识别语谱图...\n\n正在识别第"+progress[0]+"张（"+progress[0]+"/"+total+"）语谱图,已完成:"+percent+"%");
            }
        }

        // 点击停止录音后由UI线程执行
        protected void onPostExecute(RecognitionResult result) {

            if(result.status == null) {
                statusTextView.setText("识别完成：语谱图个数："+result.spectrogramPixelList.size()
                        +"\n最后的预测值："+result.predictLabel
                        +"\n名字:"+ RecognitionService.NAMES[result.predictLabel]
                        +"\n每张语谱图属于各个标签的次数:"+Arrays.toString(result.labelCount)
                        +"\n每张语谱图所属的标签值:"+ Arrays.toString(result.labelList.toArray()));

                stopRecognitionButton.setVisibility(View.GONE);

                fileChooseButton.setText("继续选择文件");
                fileChooseButton.setVisibility(View.VISIBLE);

            }else {
                statusTextView.setText("语谱图识别中发生异常！\n"+result.status);
                stopRecognitionButton.setVisibility(View.GONE);

                fileChooseButton.setText("重新选择文件");
                fileChooseButton.setVisibility(View.VISIBLE);
            }
        }
    }

}
