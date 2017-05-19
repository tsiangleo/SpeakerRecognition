package com.github.tsiangleo.sr.client.spectrogram;

import java.util.List;


public class ArraysUtil {
	
	public static float[] minAndMax(List<float[]> dataArrayList){
		float max = dataArrayList.get(0)[0];
		float min = max;
		
		for (float[] dataArray: dataArrayList) {
			for(int i = 0;i<dataArray.length;i++){
				if(dataArray[i] > max){
					max = dataArray[i];
				}
				if(dataArray[i] < min){
					min = dataArray[i];
				}
			}
		}
		return new float[]{min,max};
	}

	public static short[] minAndMax(short[] data){
		short max = data[0];
		short min = max;

		for(int i = 0;i<data.length;i++){
			if(data[i] > max){
				max = data[i];
			}
			if(data[i] < min){
				min = data[i];
			}
		}
		return new short[]{min,max};
	}

	public static int argmax(float[] prob){
		int result = 0;
		for(int i=1;i<prob.length;i++) {
			if (prob[result] < prob[i]) {
				result = i;
			}
		}
		return result;
	}


	public static int argmax(int[] prob){
		int result = 0;
		for(int i=1;i<prob.length;i++) {
			if (prob[result] < prob[i]) {
				result = i;
			}
		}
		return result;
	}
}
