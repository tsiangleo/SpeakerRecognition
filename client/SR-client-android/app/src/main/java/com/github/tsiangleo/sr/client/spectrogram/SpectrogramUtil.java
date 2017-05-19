package com.github.tsiangleo.sr.client.spectrogram;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SpectrogramUtil {

	private static final double epsilon = 1e-9;
	
	/**
	 * 生成语音文件的语谱图的每列像素list。
	 * 
	 * @param wavFile 语音文件
	 * @param fftlen FFT点数
	 * @param hopSize 帧移
	 * @return 语谱图从左至右的每一列组成的list。每一列是一个float[]，存放了该列的像素值。
	 * @throws Exception
	 */
	public static List<float[]> getPixelList(File wavFile,int fftlen,int hopSize) throws Exception{
		//load wav samples.
		int[] samples = WavFile.loadWav(wavFile);
		float[] floatSamples = WavFile.intTofloats(samples);
		Log.i("SpectrogramProduce","wavFile samples length："+ floatSamples.length+",wavFile samples data："+ Arrays.toString(floatSamples));

		//STFT Transform.
		STFT stft = new STFT(fftlen,hopSize,floatSamples.length,STFT.WINDOW_FUNCTION_HANNING);
		List<float[]> energylist = stft.forwardTransform(floatSamples);
//		Log.i("SpectrogramProduce","wavFile energylist[100]："+ Arrays.toString(energylist.get(100)));

		//Energy to DB.
		List<float[]> logEnergylist = energyToDB(energylist);
//		Log.i("SpectrogramProduce","wavFile logEnergylist[100]："+ Arrays.toString(logEnergylist.get(100)));

		//DB to pixel.
		List<float[]> pixelList = dBToPixel(logEnergylist);
//		Log.i("SpectrogramProduce","wavFile pixelList："+Arrays.deepToString(pixelList.toArray()));
		return pixelList;
	}


	/**
	 * 生成语音文件的语谱图的Energy List。
	 *
	 * @param wavFile 语音文件
	 * @param fftlen FFT点数
	 * @param hopSize 帧移
	 * @return 语谱图从左至右的每一列组成的list。每一列是一个float[]，存放了该列的energy。
	 * @throws Exception
	 */
	public static List<float[]> getEnergyList(File wavFile,int fftlen,int hopSize) throws Exception{
		//load wav samples.
		int[] samples = WavFile.loadWav(wavFile);
		float[] floatSamples = WavFile.intTofloats(samples);

		//STFT Transform.
		STFT stft = new STFT(fftlen,hopSize,floatSamples.length,STFT.WINDOW_FUNCTION_HANNING);
		List<float[]> energylist = stft.forwardTransform(floatSamples);

		return energylist;
	}
	
	/**
	 * 由于wav文件生成的语谱图一般比较大，所以需要对原始的大语谱图进行切分。
	 * 
	 * @param orignalPixelList 原始的大语谱图。该list保存了语谱图从左至右的每一列像素值。
	 * @param width  切分成的小语谱图的宽度。
	 * @param height 切分成的小语谱图的高度。
	 * @return list of 切分后的语谱图。每个float[]中保存了小语谱图的像素点矩阵，按行优先存储，每个像素值已归一化到0-1之间。
	 */
	public static List<float[]> sliceSpectrogram(List<float[]> orignalPixelList,int width,int height) {
		if(orignalPixelList == null || orignalPixelList.isEmpty())
			return null;
		if(width > orignalPixelList.size() || width <= 0){
			throw new IllegalArgumentException("切分后的语谱图的宽度必须在1~"+orignalPixelList.size()+"之间");
		}
		if(height > orignalPixelList.get(0).length || height <= 0){
			throw new IllegalArgumentException("切分后的语谱图的高度必须在1~"+orignalPixelList.get(0).length+"之间");
		}
		
		//切分高度
		if(width != orignalPixelList.get(0).length){
			orignalPixelList = sliceHeight(orignalPixelList,height);
		}		
		
		//切分宽度
		return sliceWidth(orignalPixelList,width);
	}
	
	/**
	 * 每width列切分为一张小的语谱图。
	 * 
	 * @param orignalPixelList
	 * @param width
	 * @return list of 小语谱图的像素点矩阵，按行优先存储，每个像素值归一化到0-1之间。
	 */
	public static List<float[]> sliceWidth(List<float[]> orignalPixelList,int width){

		List<float[]> results = new ArrayList<>();

		int rows = orignalPixelList.get(0).length; //height
		int cols = orignalPixelList.size(); //width
		
		int numOfSlice = cols / width;

		for (int c = 0;c<numOfSlice;c++) {

			//存放一张切分后的小语谱图的像素点矩阵,按行优先存储。
			float[] oneSpectrogram = new float[rows*width];
			int k = 0;
			
			for (int i = 0; i < rows; i++) {
				for (int j = 0; j < width; j++) {
					
					float d = orignalPixelList.get(j + c * width)[i];
					oneSpectrogram[k++] = d/255.0f;
				}
			}
			results.add(oneSpectrogram);
		}
		return results;
	}
	
	
	/**
	 * 每一列只取指定的高度。
	 * @param orignalPixelList
	 * @param height
	 * @return
	 */
	public static List<float[]> sliceHeight(List<float[]> orignalPixelList,int height){
		if(orignalPixelList == null || orignalPixelList.isEmpty())
			return null;
		if(height > orignalPixelList.get(0).length || height <= 0){
			throw new IllegalArgumentException("切分后的语谱图的高度必须在1~"+orignalPixelList.get(0).length+"之间");
		}
		
		List<float[]> slicedPixelList = new ArrayList<>(orignalPixelList.size());
		
		for(float[] colPixels :orignalPixelList){
			float[] newCol = new float[height];
			for (int i = 0; i < newCol.length; i++) {
				newCol[i] = colPixels[i];
			}
			slicedPixelList.add(newCol);
		}
		return slicedPixelList;
	}
	
	
	
	/**
	 * 将Energy做log操作，转为DB形式。
	 * @param energyList
	 * @return
	 */
	public static List<float[]> energyToDB(List<float[]> energyList) {
		List<float[]> logEnergyList = new ArrayList<>();
		for(float[] energy : energyList){
			float[] logEnergy = new float[energy.length];
			for (int i = 0; i < energy.length; i++) {
				//energy的值可能为0。这种情况下再做log操作就是负无穷了。加上epsilon就是为了避免得到负无穷。
				logEnergy[i] = (float) (10 * Math.log10(energy[i] + epsilon));
			}
			logEnergyList.add(logEnergy);
		}
		return logEnergyList;
	}
	
	/**
	 * 将DB形式的energy转为0-255之间的灰度像素值。
	 * @param logEnergyList
	 * @return
	 */
	public static List<float[]> dBToPixel(List<float[]> logEnergyList) {
		
		float[] minMax = ArraysUtil.minAndMax(logEnergyList);
		
		float minDB = minMax[0];
		float maxDB = minMax[1];
		float gapDB = maxDB - minDB;
		
		System.out.println("minDB:"+minDB+",maxDB:"+maxDB+",gapDB:"+gapDB);
		
		List<float[]> pixelList = new ArrayList<>();
		for(float[] logEnergy : logEnergyList){
			float[] pixel = new float[logEnergy.length];
			for (int i = 0; i < logEnergy.length; i++) {
				pixel[i] = (float) ((logEnergy[i] - minDB ) / gapDB * 255.0);
			}
			pixelList.add(pixel);
		}
		return pixelList;
	}
	
}
