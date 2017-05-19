package com.github.tsiangleo.sr.client.spectrogram;

//https://github.com/bewantbe/audio-analyzer-for-android/blob/master/audioSpectrumAnalyzer/src/main/java/github/bewantbe/audio_analyzer_for_android/STFT.java

import java.util.ArrayList;
import java.util.List;


//Short Time Fourier Transform
// http://zone.ni.com/reference/en-XX/help/371361E-01/lvanls/stft_spectrogram_core/#details
public class STFT {
	
	//窗函数
	public static final String WINDOW_FUNCTION_HANNING = "Hanning";
	public static final String WINDOW_FUNCTION_HAMMING = "Hamming";
	
	private double[] windFunction;
	private int fftLen;
	private int hopSize;

	//总的帧数
	private int totalFrame;

	private RealDoubleFFT fft;

	/**
	 * 
	 * @param fftlen 帧长
	 * @param hopsize 帧移
	 * @param sampleLength 音频数据数组的长度
	 * @param windowFunction 窗函数，STFT.WINDOW_FUNCTION_HANNING或者STFT.WINDOW_FUNCTION_HAMMING。
	 */
	public STFT(int fftlen, int hopsize, int sampleLength,String windowFunction) {
		if (hopsize <= 0) {
			throw new IllegalArgumentException("STFT::init(): should hopSize > 0.");
		}
		if (((-fftlen) & fftlen) != fftlen) {
			throw new IllegalArgumentException("STFT::init(): Currently, only power of 2 are supported in fftlen");
		}
		
		fftLen = fftlen;
		hopSize = hopsize;
		fft = new RealDoubleFFT(fftLen);

		int numFrame = (int) ((sampleLength - fftlen) / (float)hopsize + 1);
		//math.ceil(x)返回大于参数x的最小整数,即对浮点数向上取整
		totalFrame = (int) Math.ceil(numFrame);
		
		initWindowFunction(fftlen, windowFunction);
	}

	/**
	 * 初始化窗函数
	 * @param fftlen
	 * @param wndName
     */
	private void initWindowFunction(int fftlen, String wndName) {
		windFunction = new double[fftlen];
		if (WINDOW_FUNCTION_HAMMING.equalsIgnoreCase(wndName)) {
			for (int i = 0; i < windFunction.length; i++) {
				windFunction[i] =  0.54 - 0.46* Math.cos(2 * Math.PI * i / (windFunction.length - 1));
			}
		} 
		else if (WINDOW_FUNCTION_HANNING.equalsIgnoreCase(wndName)) {
			for (int i = 0; i < windFunction.length; i++) {
				windFunction[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (windFunction.length - 1.)));
			}
		} 
		else {
			for (int i = 0; i < windFunction.length; i++) {
				windFunction[i] = 1;
			}
		}
	}
	
	/**
	 * 对每一帧语音数据进行FFT。
	 * @param samples。每个采样点的值，0-1之间的浮点数， 每个采样点的原始格式为16-bit PCM。
	 * @return list of each frame's energy。the size of each float[] in the list is fftLen / 2 + 1.
	 */
	public List<float[]> forwardTransform(float[] samples) {
		int sampleLength = samples.length;
		//存放每一帧语音。
		float[] oneFrameSample = new float[fftLen];
		int oneFrameSamplePt = 0;	// Pointer for oneFrameSample
		//存放每一帧加窗后的语音。
		double[] oneFrameSampleTmp = new double[fftLen];
		//临时存放一帧语音对应的Energy
		float[] oneFramesEnergyTmp = new float[fftLen / 2 + 1];
		//存放每一帧的Energy
		List<float[]> eachFramesEnergyList = new ArrayList<float[]>();

		// let's go
		int inLen = oneFrameSample.length;
		int samplesPt = 0; // input fftOut point to be read
		while (samplesPt < sampleLength) {
			// 复制一帧语音到oneFrameSample数组中处理。
			while (oneFrameSamplePt < inLen && samplesPt < sampleLength) {
				oneFrameSample[oneFrameSamplePt++] = samples[samplesPt++];
			}
			if (oneFrameSamplePt == inLen) { // enough fftOut for one FFT
				// 乘以窗函数。
				for (int i = 0; i < inLen; i++) {
					oneFrameSampleTmp[i] = (float) (oneFrameSample[i] * windFunction[i]);
				}
				//快速傅里叶变换
				fft.ft(oneFrameSampleTmp);

				// FFT后的复数转为Energy
				fftOutToEnergy(oneFramesEnergyTmp, oneFrameSampleTmp);

				//复制到eachFramesEnergyList
				float[] oneFrameEnergy = new float[fftLen / 2 + 1];
				System.arraycopy(oneFramesEnergyTmp, 0,oneFrameEnergy, 0,oneFramesEnergyTmp.length);
				eachFramesEnergyList.add(oneFrameEnergy);

				//帧移
				System.arraycopy(oneFrameSample, hopSize, oneFrameSample, 0, oneFrameSample.length - hopSize);
				oneFrameSamplePt = oneFrameSample.length - hopSize;
			}
		}
		
		return eachFramesEnergyList;
	}

	/**
	 * fft的输出序列转为Energy
	 * @param energy
	 * @param fftOut
	 */
	private void fftOutToEnergy(float[] energy, double[] fftOut) {
		energy[0] = oneComplexToEnergy(fftOut[0],fftOut[0]);
		int j = 1;
		for (int i = 1; i < fftOut.length - 1; i += 2, j++) {
			energy[j] = oneComplexToEnergy(fftOut[i],fftOut[i+1]);
		}
		energy[j] = oneComplexToEnergy(fftOut[fftOut.length - 1],fftOut[fftOut.length - 1]);
	}

	/**
	 * 将一个复数（实部+虚部）转为Energy。
	 * @param real
	 * @param im
	 * @return
	 */
	private float oneComplexToEnergy(double real,double im){
		double energy = real * real + im * im;
		return (float) energy;
	}
}
