package com.github.tsiangleo.sr.client.activity;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.tsiangleo.sr.client.R;
import com.github.tsiangleo.sr.client.business.AudioRecordService;
import com.github.tsiangleo.sr.client.business.NetService;
import com.github.tsiangleo.sr.client.business.RecognitionService;
import com.github.tsiangleo.sr.client.business.SpectrogramService;
import com.github.tsiangleo.sr.client.spectrogram.SpectrogramOnAndroid;
import com.github.tsiangleo.sr.client.util.SysConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by tsiang on 2016/11/26.
 */

public class VerifyVoiceActivity extends BaseActivity implements View.OnClickListener{
    public static final String EXTRA_MESSAGE_RET_RESULT  = "com.github.tsiangleo.sr.VerifyVoiceActivity.EXTRA_MESSAGE_RET_RESULT";
    private static final String TAG = "VerifyVoiceActivity" ;




    private Button startRecordButton,stopRecordButton,stopConvertButton,stopSpectrogramButton,stopRecognitionButton;
    private TextView statusTextView,hintTextView;
    private ProgressDialog progressDialog;
    private ImageView imageViewSpectrogram;
//    private ImageView imageViewSpectrogram2;

    private File rawFile;
    private File wavFile;

    private AudioRecordService audioRecordService;
    private NetService netService;

    private RecordAudioTask recordTask;
    private PlayAudioTask playTask;
    private RecognitionTask recognitionTask;
    private ConvertToWAVTask convertTask;
    private SpectrogramTask spectrogramTask;

    private Chronometer chronometer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_voice);

        statusTextView = (TextView) findViewById(R.id.statusTextView);
        hintTextView = (TextView) findViewById(R.id.hintTextView);

        imageViewSpectrogram = (ImageView) findViewById(R.id.imageViewSpectrogram);
//        imageViewSpectrogram2 = (ImageView) findViewById(R.id.imageViewSpectrogram2);

        startRecordButton = (Button) findViewById(R.id.startRecordButton);
        stopRecordButton = (Button) findViewById(R.id.stopRecordButton);
        stopConvertButton = (Button) findViewById(R.id.stopConvertButton);
        stopSpectrogramButton = (Button) findViewById(R.id.stopSpectrogramButton);
        stopRecognitionButton = (Button) findViewById(R.id.stopRecognitionButton);
        chronometer = (Chronometer) findViewById(R.id.chronometer);


        startRecordButton.setOnClickListener(this);
        stopRecordButton.setOnClickListener(this);
        stopConvertButton.setOnTouchListener(this);
        stopSpectrogramButton.setOnTouchListener(this);
        stopRecognitionButton.setOnTouchListener(this);

        chronometer.setVisibility(View.INVISIBLE);
        statusTextView.setVisibility(View.GONE);
        stopRecordButton.setVisibility(View.GONE);
        stopConvertButton.setVisibility(View.GONE);
        stopSpectrogramButton.setVisibility(View.GONE);
        stopRecognitionButton.setVisibility(View.GONE);
        imageViewSpectrogram.setVisibility(View.GONE);
//        imageViewSpectrogram2.setVisibility(View.GONE);

        netService = new NetService(dataAccessService.getServerIP(),dataAccessService.getServerPort());



    }

    private void initFile(String fileNamePrefix) {

        try {
            rawFile = File.createTempFile(fileNamePrefix, ".pcm",getFilesDir());
            wavFile = File.createTempFile(fileNamePrefix, ".wav",getFilesDir());
        } catch (IOException e) {
            Log.e(TAG,"initFile1:"+e);
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
                    Log.e(TAG,"initFile2:"+e);
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
        }else if (v == stopSpectrogramButton){
            playTask.cancel(true);
            stopSpectrogramButton.setVisibility(View.GONE);
            startRecordButton.setVisibility(View.VISIBLE);

        }else if(v == stopRecognitionButton){
            recognitionTask.cancel(true);
            stopRecognitionButton.setVisibility(View.GONE);
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

    private void spectrogram() {
        spectrogramTask = new SpectrogramTask();
        spectrogramTask.execute();
    }

    private void recognition() {
        recognitionTask = new RecognitionTask();
        recognitionTask.execute();
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


        // 点击停止录音后由UI线程执行
        protected void onPostExecute(String result) {
            if(result == null) {
                statusTextView.setText("完成录音，文件是:" + getFilePath(rawFile));
                //停止按钮不可见
                stopRecordButton.setVisibility(View.GONE);
                chronometer.setVisibility(View.GONE);
                statusTextView.setVisibility(View.VISIBLE);
                stopConvertButton.setVisibility(View.VISIBLE);

                convert();
//                play();
            }else {
                showMsgAndCloseActivity(result,VerifyVoiceActivity.this);
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


        // 点击停止录音后由UI线程执行
        protected void onPostExecute(String result) {
            if(result != null){
                statusTextView.setText("播放录音出错:"+result);

            }else{
                statusTextView.setText("完成录音播放");
            }

        }
    }
    private class SpectrogramTask extends AsyncTask<Void, Long, List<Bitmap>> {

        /**
         *
         * @param params
         * @return 返回值说明：0成功; 1服务器地址和端口号不符合规范; 2文件上传出错
         */
        @Override
        protected List<Bitmap> doInBackground(Void... params) {
            try {
                long ctm = System.currentTimeMillis();
                File rawBG = File.createTempFile("bg_"+ctm, ".pcm",getFilesDir());
                File wavBG = File.createTempFile("bg_"+ctm, ".wav",getFilesDir());

                AudioRecordService ars = new AudioRecordService(rawBG);
                //录制1秒钟的背景语音
                ars.startRecording(0.5f);
                ars.rawToWavFile(wavBG);

                Bitmap bitmap1 = SpectrogramOnAndroid.getBitmap(RecognitionService.FFTLEN,RecognitionService.HOPSIZE,wavFile);
//                Bitmap bitmap2 = SpectrogramOnAndroid.getDenoisedBitmap(RecognitionService.FFTLEN,RecognitionService.HOPSIZE,wavFile,wavBG);
                Bitmap bitmap2 = SpectrogramService.getBitmap(rawFile,RecognitionService.FFTLEN,RecognitionService.HOPSIZE);
                SpectrogramService.compare(rawFile,wavFile);

                List<Bitmap> bitmapList = new ArrayList<>();
                bitmapList.add(bitmap1);
                bitmapList.add(bitmap2);

                rawBG.delete();
                wavBG.delete();
                return bitmapList;

            }catch (Exception e) {
                Log.e(TAG,"SpectrogramTask.doInBackground:"+e);
                return null;
            }
        }

        // 点击停止录音后由UI线程执行
        protected void onPostExecute(List<Bitmap> result) {

            if(result == null) {
                statusTextView.setText("生成语谱图失败!");
                //停止按钮不可见
                stopSpectrogramButton.setVisibility(View.GONE);
                startRecordButton.setVisibility(View.VISIBLE);
                startRecordButton.setText("重新开始录音");
                deleteFile();
            }else {
                imageViewSpectrogram.setVisibility(View.VISIBLE);
                imageViewSpectrogram.setImageBitmap(result.get(0));
//                imageViewSpectrogram2.setVisibility(View.GONE);
//                imageViewSpectrogram2.setImageBitmap(result.get(1));
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

                // update
                long ctm = System.currentTimeMillis();
                File rawBG = File.createTempFile("bg_"+ctm, ".pcm",getFilesDir());
                File wavBG = File.createTempFile("bg_"+ctm, ".wav",getFilesDir());

                AudioRecordService ars = new AudioRecordService(rawBG);
                //录制1秒钟的背景语音
                ars.startRecording(0.5f);
                ars.rawToWavFile(wavBG);

                rawBG.delete();

//                List<float[]>  spectrogramPixelList = SpectrogramOnAndroid.getSlicedPixelList(RecognitionService.FFTLEN,RecognitionService.HOPSIZE,wavFile,RecognitionService.SLICED_WIDTH,RecognitionService.SLICED_HEIGHT);
                List<float[]>  spectrogramPixelList = SpectrogramOnAndroid.getSlicedAndDenoisedPixelList(RecognitionService.FFTLEN,RecognitionService.HOPSIZE,wavFile,wavBG,RecognitionService.SLICED_WIDTH,RecognitionService.SLICED_HEIGHT);
                wavBG.delete();

                // update end

                Log.i(TAG,"spectrogramPixelList[0]:"+Arrays.toString(spectrogramPixelList.get(0)));
                Log.i(TAG,"spectrogramPixelList[1]:"+Arrays.toString(spectrogramPixelList.get(1)));

                RecognitionService recognitionService = new RecognitionService(VerifyVoiceActivity.this);

                recognitionService.init();


                result.spectrogramPixelList = spectrogramPixelList;

                numSpectrogram = spectrogramPixelList.size();

                //记录每张语谱图属于各个标签的概率值.
                List<float[]> probList = new ArrayList<>();
                //记录每张语谱图所属的标签值.

                //进度标志
                Long progress = 1L;

                List<Integer> labelList = new ArrayList<>();
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
                StringBuilder show = new  StringBuilder();
                show.append("语谱图个数："+result.spectrogramPixelList.size()
                        +"\n最后的预测值："+result.predictLabel
                        +"\n名字:"+ RecognitionService.NAMES[result.predictLabel]
                        +"\n每张语谱图属于各个标签的次数:"+Arrays.toString(result.labelCount)
                        +"\n每张语谱图所属的标签值:"+ Arrays.toString(result.labelList.toArray()));

//                statusTextView.setText(show.toString());
                Toast.makeText(VerifyVoiceActivity.this,show.toString(),Toast.LENGTH_LONG).show();

                statusTextView.setText("识别完成，结果如下~\n\n"+
                        result.predictLabel+"号Speaker："+RecognitionService.NAMES[result.predictLabel]);

                stopRecognitionButton.setVisibility(View.GONE);
                startRecordButton.setVisibility(View.VISIBLE);



            }else {
                statusTextView.setText("语谱图识别中发生异常！\n"+result.status);
                stopRecognitionButton.setVisibility(View.GONE);
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
                statusTextView.setText("正在生成语谱图...");
                //停止按钮不可见
                stopConvertButton.setVisibility(View.GONE);
                stopSpectrogramButton.setVisibility(View.VISIBLE);

                spectrogram();

            }else {
                showMsgAndCloseActivity("转换出错："+result,VerifyVoiceActivity.this);
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
