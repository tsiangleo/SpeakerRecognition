package com.github.tsiangleo.sr.client.activity;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.tsiangleo.sr.client.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *  Reference：https://yq.aliyun.com/articles/8637
 *
 * Created by tsiang on 2016/11/23.
 */

public class AudioRecorderActivity  extends AppCompatActivity implements View.OnClickListener{
    public static final String KEY_SERVER_IP = "com.github.tisnagleo.sr.server.ip";
    public static final String KEY_SERVER_PORT = "com.github.tisnagleo.sr.server.port";

    private Button startButton,stopButton;
    private TextView statusTextView;

    private File recordingFile;
    private File wavFile;
    private RecordAudioTask recordTask;
    private PlayAudioTask playTask;
    private UploadAudioTask uploadTask;
    private ConvertToWAVTask convertTask;

    //录音源
    private static int audioSource = MediaRecorder.AudioSource.MIC;
    //录音的采样频率
    private static int sampleRateInHz = 44100; //44100, 22050, 11025, 8000
    //录音的声道:单声道（Mono）和双声道（Stereo）
    private static int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    //AudioTrack播放时的配置，和channelConfig应保持一致
    private static int outChannelConfig = AudioFormat.CHANNEL_OUT_MONO;
    //用于wav转换，声道数： 1 for mono, 2 for stereo
    private static final int numOfChannels = 1;
    //量化的深度
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    //用于wav转换，每次采用的位宽。与audioFormat的取值有关。
    private static final int bitsPerSample = 16;
    //缓存的大小
    private static int bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,channelConfig,audioFormat);

    private boolean isRecording = false;

    private String host;
    private int port;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_recorder);

        statusTextView = (TextView) findViewById(R.id.statusTextView);
        startButton = (Button) findViewById(R.id.startButton);
        stopButton = (Button) findViewById(R.id.stopButton);

        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);

        statusTextView.setVisibility(View.INVISIBLE);
        stopButton.setVisibility(View.GONE);

        Intent intent = getIntent();
        host = intent.getStringExtra(KEY_SERVER_IP);
        port = intent.getIntExtra(KEY_SERVER_PORT,0);
        Toast.makeText(this,"server address:"+host+":"+port,Toast.LENGTH_SHORT).show();

        File path = new File( Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Android/data/com.github.tsiangleo.sr.client/files/");
        path.mkdirs();
        try {
            recordingFile = File.createTempFile("recording", ".pcm", path);
            wavFile = File.createTempFile("recording", ".wav", path);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create file on SD card", e);
        }
    }

    @Override
    public void onClick(View v) {
        if(v == startButton){
            record();
        }else if(v == stopButton){
            stopRecord();
        }
    }

    private void record() {
        startButton.setVisibility(View.GONE);
        stopButton.setVisibility(View.VISIBLE);
        statusTextView.setVisibility(View.VISIBLE);

        recordTask = new RecordAudioTask();
        recordTask.execute();


    }
    private void stopRecord() {
        isRecording = false;
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
    private class RecordAudioTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            isRecording = true;

            try {
                DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(recordingFile)));

                AudioRecord audioRecord = new AudioRecord(
                        audioSource, sampleRateInHz,
                        channelConfig, audioFormat, bufferSizeInBytes);

                short[] buffer = new short[bufferSizeInBytes];
                audioRecord.startRecording();

                int count = 0;
                while (isRecording) {
                    int bufferReadResult = audioRecord.read(buffer, 0,bufferSizeInBytes);
                    for (int i = 0; i < bufferReadResult; i++) {
                        dos.writeShort(buffer[i]);
                    }

                    publishProgress(new Integer(count));
                    count++;
                }
                audioRecord.stop();
                dos.close();
            } catch (Throwable t) {
                Log.e("AudioRecord", "Recording Failed");
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
            statusTextView.setText("正在录音:"+progress[0].toString());
        }

        // 点击停止录音后由UI线程执行
        protected void onPostExecute(Void result) {
            statusTextView.setText("完成录音，文件是:"+recordingFile.getAbsolutePath());
            //停止按钮不可见
            stopButton.setVisibility(View.GONE);
            //开始按钮可见，但不出于enable状态，得等到文件上传完成后才能出于enable状态。
            startButton.setVisibility(View.VISIBLE);
            startButton.setEnabled(false);

            // 可以播放了、可以上传了,以下两个任务可以同时执行
            convert();
            play();
        }


    }

    private class PlayAudioTask extends AsyncTask<Void, Long, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(recordingFile)));

                AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC, sampleRateInHz,
                        outChannelConfig,audioFormat, bufferSizeInBytes,
                        AudioTrack.MODE_STREAM);

                audioTrack.play();

                short[] audiodata = new short[bufferSizeInBytes / 4];

                long count = 0;
                while (dis.available() > 0) {
                    int i = 0;
                    while (dis.available() > 0 && i < audiodata.length) {
                        audiodata[i] = dis.readShort();
                        i++;
                    }
                    audioTrack.write(audiodata, 0, audiodata.length);

                    count += i;
                    publishProgress(new Long(count));
                }

                dis.close();

            } catch (Exception e) {
                Log.e("AudioTrack", "Playback Failed");
                return e.getMessage();
            }

            return null;
        }

        protected void onProgressUpdate(Long... progress) {
            long total = recordingFile.length() / 2;  //以short为单位，2个字节。

            if(total != 0) {
                int percent = (int)((progress[0] / (double)total) * 100);
                statusTextView.setText("正在播放录音:"+percent+"%");
            }
        }

        // 点击停止录音后由UI线程执行
        protected void onPostExecute(String result) {
            if(result != null){
                Toast.makeText(AudioRecorderActivity.this,"播放录音出错:"+result,Toast.LENGTH_SHORT).show();
                return;
            }

            statusTextView.setText("录音播放完成，文件是:"+recordingFile.getAbsolutePath());
        }
    }

    private class UploadAudioTask extends AsyncTask<Void, Long, Integer> {

        /**
         *
         * @param params
         * @return 返回值说明：0成功; 1服务器地址和端口号不符合规范; 2文件上传出错
         */
        @Override
        protected Integer doInBackground(Void... params) {

            try {

                if(host == null || port <= 0){
                    return 1;
                }

                Socket socket = new Socket(host,port);
                OutputStream os = socket.getOutputStream();
                FileInputStream fis = new FileInputStream(wavFile);

                String filename = wavFile.getName();
                //1、发送文件名的长度
                os.write(filename.getBytes("utf-8").length);
                //2、发送文件名
                os.write(filename.getBytes("utf-8"));

                long count = 0;
                byte[] b = new byte[1024];
                int length;
                while((length = fis.read(b)) > 0){
                    //2、把文件写入socket输出流
                    os.write(b, 0, length);

                    count += length;
                    publishProgress(new Long(count));
                }
                os.close();
                fis.close();
                socket.close();

            } catch (Exception e) {
                return 2;
            }
            return 0;
        }
        protected void onProgressUpdate(Long... progress) {
            long total = wavFile.length();
            if(total != 0) {
                int percent = (int)((progress[0] / (double)total) * 100);
                statusTextView.setText("正在上传文件:"+percent+"%");
            }

        }

        // 点击停止录音后由UI线程执行
        protected void onPostExecute(Integer result) {
            if(result == 1){
                Toast.makeText(AudioRecorderActivity.this,"请先设置服务器地址和端口号",Toast.LENGTH_SHORT).show();
                return;
            }
            if(result == 2){
                Toast.makeText(AudioRecorderActivity.this,"上传文件出错",Toast.LENGTH_SHORT).show();
                return;
            }

            statusTextView.setText("文件上传完成，文件是:"+wavFile.getAbsolutePath());
            startButton.setEnabled(true);
        }
    }

    /**
     *
     * @param out
     * @param rawAudioLength
     * @throws IOException
     */
    private void writeWaveFileHeader(OutputStream out,long rawAudioLength)
            throws IOException {
        byte[] header = new byte[44];
        long byteRate = sampleRateInHz * numOfChannels * bitsPerSample / 8;

        // RIFF标记占据四个字节
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        //数据大小表示，由于原始数据为long型，通过四次计算得到长度
        header[4] = (byte) ((rawAudioLength + 36) & 0xff);
        header[5] = (byte) (((rawAudioLength + 36) >> 8) & 0xff);
        header[6] = (byte) (((rawAudioLength + 36) >> 16) & 0xff);
        header[7] = (byte) (((rawAudioLength + 36) >> 24) & 0xff);
        //WAVE标记占据四个字节
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        // 'fmt '标记符占据四个字节
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) numOfChannels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (sampleRateInHz & 0xff);
        header[25] = (byte) ((sampleRateInHz >> 8) & 0xff);
        header[26] = (byte) ((sampleRateInHz >> 16) & 0xff);
        header[27] = (byte) ((sampleRateInHz >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (numOfChannels * bitsPerSample / 8); // block align
        header[33] = 0;
        //每个样本的数据位数
        header[34] = bitsPerSample;
        header[35] = 0;
        //data标记符
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        //数据长度
        header[40] = (byte) (rawAudioLength & 0xff);
        header[41] = (byte) ((rawAudioLength >> 8) & 0xff);
        header[42] = (byte) ((rawAudioLength >> 16) & 0xff);
        header[43] = (byte) ((rawAudioLength >> 24) & 0xff);
        out.write(header, 0, 44);
    }


    private class ConvertToWAVTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {

                byte[] rawData = new byte[(int) recordingFile.length()];
                DataInputStream input = null;
                try {
                    input = new DataInputStream(new FileInputStream(recordingFile));
                    input.read(rawData);
                } finally {
                    if (input != null) {
                        input.close();
                    }
                }

                DataOutputStream output = null;
                try {
                    output = new DataOutputStream(new FileOutputStream(wavFile));

                    writeWaveFileHeader(output, rawData.length);

                    // Audio data (conversion big endian -> little endian)
                    short[] shorts = new short[rawData.length / 2];
                    ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                    ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
                    for (short s : shorts) {
                        bytes.putShort(s);
                    }

                    output.write(bytes.array());
                } finally {
                    if (output != null) {
                        output.close();
                    }
                }

            } catch (IOException e) {
                return e.getMessage();
            }
            return null;
        }

        // 点击停止录音后由UI线程执行
        protected void onPostExecute(String result) {
            if(result != null){
                Toast.makeText(AudioRecorderActivity.this,"转换出错:"+result,Toast.LENGTH_SHORT).show();
                return;
            }
            statusTextView.setText("完成转换，文件是:"+wavFile.getAbsolutePath());
            // 可以上传了
            upload();
        }
    }
}
