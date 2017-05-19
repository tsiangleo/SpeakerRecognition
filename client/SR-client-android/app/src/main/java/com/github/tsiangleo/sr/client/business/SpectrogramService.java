package com.github.tsiangleo.sr.client.business;

import android.graphics.Bitmap;
import android.util.Log;

import com.github.tsiangleo.sr.client.spectrogram.ArraysUtil;
import com.github.tsiangleo.sr.client.spectrogram.STFT;
import com.github.tsiangleo.sr.client.spectrogram.SpectrogramOnAndroid;
import com.github.tsiangleo.sr.client.spectrogram.SpectrogramUtil;
import com.github.tsiangleo.sr.client.spectrogram.WavFile;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

/**
 * Created by tsiang on 2016/11/26.
 */

public class SpectrogramService {

    public static final String TAG = "SpectrogramService";

    /**
     *
     * @param rawFile
     * @return
     * @throws IOException
     */
    private static short[] readData(File rawFile) throws IOException{

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

        short[] shorts = new short[rawData.length / 2];
        ByteBuffer.wrap(rawData).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(shorts);

        return shorts;
    }

    public static Bitmap getBitmap(File rawFile,int fftlen, int hopSize) throws Exception{

        List<float[]> pixelList = producePixelList(rawFile,fftlen,hopSize);
        return SpectrogramOnAndroid.createBitmap(pixelList);
    }

    public static void compare(File rawFile,File wavFile) throws Exception{

        int[] wavSamples = WavFile.loadWav(wavFile);
        float[] wavFloatSamples = WavFile.intTofloats(wavSamples);

        short[] rawSamples = readData(rawFile);
        float[] rawFloatSamples = WavFile.shortTofloats(rawSamples);

        if(wavFloatSamples.length == rawFloatSamples.length){
            Log.i("SpectrogramProduce","wavFile 的采样点和rawFile的采样点长度相同");
        }

        for(int i = 0;i<wavSamples.length;i++){
            if(i < 60 * 128){
                Log.i("SpectrogramProduce","wav["+i+"]="+wavSamples[i]+",raw["+i+"]="+rawSamples[i]);
            }
            if( wavSamples[i] !=rawSamples[i] ){
                Log.i("SpectrogramProduce","wav["+i+"]="+wavSamples[i]+",raw["+i+"]="+rawSamples[i]);
            }
        }
        Log.i("SpectrogramProduce","wavFile 和 rawFile比较完毕");

    }


    private static List<float[]> producePixelList(File rawFile, int fftlen, int hopSize) throws Exception{
        //load samples.
        short[] samples = readData(rawFile);
        short[] minMax = ArraysUtil.minAndMax(samples);
        Log.i(TAG,"short samples min："+minMax[0]+",max:"+minMax[1]);

        float[] floatSamples = WavFile.shortTofloats(samples);
        Log.i(TAG,"rawFile short samples length："+floatSamples.length+",rawFile short samples data："+ Arrays.toString(floatSamples));

        //STFT Transform.
        STFT stft = new STFT(fftlen,hopSize,floatSamples.length,STFT.WINDOW_FUNCTION_HANNING);
        List<float[]> energylist = stft.forwardTransform(floatSamples);
//        Log.i(TAG,"rawFile energylist[100]："+ Arrays.toString(energylist.get(100)));

        //Energy to DB.
        List<float[]> logEnergylist = SpectrogramUtil.energyToDB(energylist);
//        Log.i(TAG,"rawFile logEnergylist[100]："+ Arrays.toString(logEnergylist.get(100)));

        //DB to pixel.
        List<float[]> pixelList = SpectrogramUtil.dBToPixel(logEnergylist);
//        Log.i(TAG,"rawFile pixelList："+ Arrays.deepToString(pixelList.toArray()));

        return pixelList;
    }
}
