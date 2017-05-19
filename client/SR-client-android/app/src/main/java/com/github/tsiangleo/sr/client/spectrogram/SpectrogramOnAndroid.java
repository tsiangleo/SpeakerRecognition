package com.github.tsiangleo.sr.client.spectrogram;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;


public class SpectrogramOnAndroid {


	private static final String TAG = "SpectrogramOnAndroid" ;
	
	/**
	 * 获取一个wav文件对应的语谱图的Bitmap.
     * @param fftlen FFT点数
     * @param hopsize 帧移
     * @param wavFile 语音文件
	 * @return
     * @throws Exception
     */
	public static Bitmap getBitmap(int fftlen, int hopsize, File wavFile) throws Exception {
		List<float[]> pixelList = SpectrogramUtil.getPixelList(wavFile, fftlen, hopsize);
        return createBitmap(pixelList);
    }

    /**
     * 根据语谱图的pixel List，生成对应的Bitmap。
     * @param pixelList
     * @return
     */
    public static Bitmap createBitmap(List<float[]> pixelList) {
        int rows = pixelList.get(0).length; //height
        int cols = pixelList.size();	//width

        Bitmap bitmap = Bitmap.createBitmap (cols,rows, Bitmap.Config.ARGB_8888);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int d = (int) pixelList.get(j)[i];
                //（24-31 位表示 alpha，16-23 位表示红色，8-15 位表示绿色，0-7 位表示蓝色）。
                // ARGB
                int argb = Color.argb(255,d,d,d);
                bitmap.setPixel(j,i,argb);
            }
        }
        return bitmap;
    }


    /**
     *  获取一个wav文件对应的降噪后的语谱图的Bitmap
     * @param fftlen FFT点数
     * @param hopsize 帧移
     * @param wavFile 语音文件
     * @param bgFile 背景噪声文件，用于去噪。
     * @return
     * @throws Exception
     */
    public static Bitmap getDenoisedBitmap(int fftlen, int hopsize, File wavFile,File bgFile) throws Exception {
        List<float[]> pixelList = getDenoisedPixelList(fftlen,hopsize,wavFile,bgFile);
        return createBitmap(pixelList);
    }

    /**
     *  获取一个wav文件对应的降噪后的Pixel List.
     * @param fftlen FFT点数
     * @param hopsize 帧移
     * @param wavFile 语音文件
     * @param bgFile 背景噪声文件，用于去噪。
     * @return  降噪后的 Pixel List。
     * @throws Exception
     */
    private static List<float[]> getDenoisedPixelList(int fftlen, int hopsize, File wavFile,File bgFile) throws Exception {
        //环境背景的EnergyList
        List<float[]> bgEnergyList = SpectrogramUtil.getEnergyList(bgFile,fftlen, hopsize);
        //原始的wav语音的EnergyList。
        List<float[]> originalWavEnergyList = SpectrogramUtil.getEnergyList(wavFile,fftlen, hopsize);
        //降噪后的wav语音的EnergyList
        List<float[]> denoisedWavEnergyList = noiseReduction(originalWavEnergyList,bgEnergyList,4.0f,0.002f);
        //Energy to DB.
        List<float[]> logEnergylist = SpectrogramUtil.energyToDB(denoisedWavEnergyList);
        //DB to pixel.
        List<float[]> pixelList = SpectrogramUtil.dBToPixel(logEnergylist);

        return pixelList;
    }

    /**
     * 基于谱减法的语音降噪（Noise reduction）
     *
     *  @param originalWavEnergyList 原始的wav语音的EnergyList
     * @param bgEnergyList 环境背景的EnergyList
     * @param a 过减因子
     * @param b 增益补偿因子。信噪比高，则ａ取小一点，ｂ取大一点；反之，ａ取大一点，ｂ取小一点。
     * @return 降噪后的wav语音的EnergyList
     */
    private static List<float[]> noiseReduction(List<float[]> originalWavEnergyList, List<float[]> bgEnergyList,float a,float b) {

        //计算bgEnergyList的平均值
        float[] meanBGEnergy = new float[bgEnergyList.get(0).length];
        for (float[]  bgEnergy : bgEnergyList){
            for (int j = 0; j < bgEnergy.length; j++) {
                meanBGEnergy[j] += bgEnergy[j];
            }
        }
        for (int j = 0; j < meanBGEnergy.length; j++) {
            meanBGEnergy[j] /= bgEnergyList.size();
        }
        Log.i(TAG,"meanBGEnergy:"+ Arrays.toString(meanBGEnergy));


        //进行降噪
        List<float[]> denoisedWavEnergyList = new ArrayList<>();

        for (float[] originalWavEnergy : originalWavEnergyList){
            float[] denoisedEnergy = new float[originalWavEnergy.length];

            for (int j = 0; j < originalWavEnergy.length; j++) {
                float energy = originalWavEnergy[j];
                if((energy - a*meanBGEnergy[j]) >0){
                    energy = energy - a*meanBGEnergy[j];
                }else{
                    energy = b*meanBGEnergy[j];
                }
                denoisedEnergy[j] = energy;
            }
            denoisedWavEnergyList.add(denoisedEnergy);
        }
        return denoisedWavEnergyList;
    }

    /**
     * 由于wav文件生成的语谱图一般比较大，所以需要对原始的大语谱图进行切分。
     *
     * 获取一个语音文件的切分后的 Pixel List。
     *
     * @param fftlen FFT点数
     * @param hopsize 帧移
     * @param wavFile 语音文件
     * @param width  切分成的小语谱图的宽度。
     * @param height 切分成的小语谱图的高度。
     * @return 语音文件的切分后的 Pixel List，每个float[]中保存了小语谱图的像素点矩阵，按行优先存储，每个像素值已归一化到0-1之间。
     * @throws Exception
     */
    public static List<float[]> getSlicedPixelList(int fftlen, int hopsize, File wavFile,int width,int height) throws Exception {
        List<float[]> listPixel = SpectrogramUtil.getPixelList(wavFile,fftlen,hopsize);
        List<float[]>  slicedPixelList = SpectrogramUtil.sliceSpectrogram(listPixel,width,height);
        return slicedPixelList;
    }

    /**
     * 由于wav文件生成的语谱图一般比较大，所以需要对原始的大语谱图进行切分。
     *
     * 获取一个语音文件降噪后的切分后的 Pixel List。
     *
     * @param fftlen FFT点数
     * @param hopsize 帧移
     * @param wavFile 语音文件
     * @param bgFile 背景噪音文件，用于降噪。
     * @param width  切分成的小语谱图的宽度。
     * @param height 切分成的小语谱图的高度。
     * @return 语音文件的切分后的 Pixel List，每个float[]中保存了小语谱图的像素点矩阵，按行优先存储，每个像素值已归一化到0-1之间。
     * @throws Exception
     */
    public static List<float[]> getSlicedAndDenoisedPixelList(int fftlen, int hopsize, File wavFile,File bgFile,int width,int height) throws Exception {
        List<float[]> listPixel = getDenoisedPixelList(fftlen,hopsize,wavFile,bgFile);
        List<float[]>  slicedPixelList = SpectrogramUtil.sliceSpectrogram(listPixel,width,height);
        return slicedPixelList;
    }
}
