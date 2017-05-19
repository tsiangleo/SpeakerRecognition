package com.github.tsiangleo.sr.client.business;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by tsiang on 2016/11/26.
 */

public class AudioRecordService {
    //录音源
    private static int audioSource = MediaRecorder.AudioSource.MIC;
    //录音的采样频率
    public static final int sampleRateInHz = 16000; //44100, 22050, 11025, 8000
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

    private volatile boolean isRecording = false;

    private File rawFile;

    /**
     *
     * @param rawFile 录音文件的存放路径
     */
    public AudioRecordService(File rawFile){
        this.rawFile = rawFile;
    }

    /**
     * 开始录音
     */
    public void startRecording() throws Exception {
        isRecording = true;
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(rawFile)));

            AudioRecord audioRecord = new AudioRecord(
                    audioSource, sampleRateInHz,
                    channelConfig, audioFormat, bufferSizeInBytes);

            if(audioRecord == null || audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED)
                throw new RuntimeException("应用需要录音权限，请先授权！");

            short[] buffer = new short[bufferSizeInBytes];
            audioRecord.startRecording();

            while (isRecording) {
                int bufferReadResult = audioRecord.read(buffer, 0,bufferSizeInBytes);
                for (int i = 0; i < bufferReadResult; i++) {
                    dos.writeShort(buffer[i]);
                }
            }
            audioRecord.stop();
        }finally {
            if (dos != null){
                dos.close();
            }
        }



    }



    /**
     * 开始录音
     * 录制制定长度的语音。update 04-06
     * @param seconds　以秒为单位.
     * @throws Exception
     */
    public void startRecording(float seconds) throws Exception {

        isRecording = true;
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(rawFile)));

            AudioRecord audioRecord = new AudioRecord(
                    audioSource, sampleRateInHz,
                    channelConfig, audioFormat, bufferSizeInBytes);

            if(audioRecord == null || audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED)
                throw new RuntimeException("应用需要录音权限，请先授权！");

            short[] buffer = new short[bufferSizeInBytes];
            audioRecord.startRecording();
            long start = System.currentTimeMillis();
            long now;

            while (isRecording) {
                int bufferReadResult = audioRecord.read(buffer, 0,bufferSizeInBytes);
                for (int i = 0; i < bufferReadResult; i++) {
                    dos.writeShort(buffer[i]);
                }

                now = System.currentTimeMillis();
                if((now - start ) >= (seconds * 1000)){
                    isRecording = false;
                }
            }

            audioRecord.stop();
        }finally {
            if (dos != null){
                dos.close();
            }
        }



    }
    /**
     * 停止录音
     */
    public void stopRecording(){
        isRecording = false;
    }

    /**
     * 播放录制的原始语音文件
     */
    public void startPlaying() throws Exception{

        if(rawFile  == null){
            throw new IllegalArgumentException("待播放的文件不存在");
        }
        if(rawFile.length() <= 0){
            throw new IllegalArgumentException("待播放的文件大小不能为零");
        }
        if(isRecording == true){
            throw new RuntimeException("录音期间不能播放");
        }

        DataInputStream dis  = null;
        try {
            dis= new DataInputStream(new BufferedInputStream(new FileInputStream(rawFile)));

            AudioTrack audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC, sampleRateInHz,
                    outChannelConfig, audioFormat, bufferSizeInBytes,
                    AudioTrack.MODE_STREAM);

            audioTrack.play();

            short[] audiodata = new short[bufferSizeInBytes / 4];

            while (dis.available() > 0) {
                int i = 0;
                while (dis.available() > 0 && i < audiodata.length) {
                    audiodata[i] = dis.readShort();
                    i++;
                }
                audioTrack.write(audiodata, 0, audiodata.length);
            }
        }finally {
            if (dis != null){
                dis.close();
            }
        }
    }

    /**
     * 将原始录音文件转换成wav格式的文件
     * @param wavFile 将要生成的wav格式的文件的路径。
     */
    public void rawToWavFile(File wavFile) throws Exception{

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
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
    }

    private void writeWaveFileHeader(OutputStream out, long rawAudioLength)
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

}
