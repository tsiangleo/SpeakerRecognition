#!/usr/bin/env python
#coding: utf-8
'''
Created on 2017年2月10日

@author: tsiang
'''
from numpy.lib import stride_tricks
import numpy as np
import os
import scipy.io.wavfile as wav
import img_read_and_write


def stft(signal, frameSize, hopSize, window=np.hanning):
    """
    功能：短时傅里叶变换
    参数：
    frameSize：帧长
    hopSize：帧移
  返回值：
    np.ndarray [shape=(1 + frameSize/2, t), dtype=dtype]
    """
    win = window(frameSize)
    # zeros at beginning (thus center of 1st window should be for sample nr. 0)
    samples = np.append(np.zeros(np.floor(frameSize/2.0)), signal)    
    # cols for windowing
    cols = np.ceil( (len(samples) - frameSize) / float(hopSize)) + 1
    # zeros at end (thus samples can be fully covered by frames)
    samples = np.append(samples, np.zeros(frameSize))
     
    frames = stride_tricks.as_strided(samples, shape=(cols, frameSize), strides=(samples.strides[0]*hopSize, samples.strides[0])).copy()
    frames *= win
    return np.fft.rfft(frames)

    
#将TIMIT中的每个wav文件转为一张png图片
def produce_spectrogram_for_each_wav(sourceRoot = "E:/DeepLearning/data/TIMIT/wav_data/all_in_one",
                targetRoot = "E:/DeepLearning/data/TIMIT/png_data/all_in_one_update_20170107"):

    for root,dirs,files in os.walk(os.path.abspath(sourceRoot)):
        print "root:",root,",dirs:",dirs,",files:",files
    
        for fn in files:
            if os.path.splitext(os.path.join(root, fn))[1]=='.wav':
                sourceFile = os.path.join(root, fn)
                print "sourceFile:",sourceFile
                
                suffix = sourceFile[len(os.path.abspath(sourceRoot))+1:]
                
                targetFile = os.path.join(os.path.abspath(targetRoot), suffix[:-4]+".png")
                print "targetFile:",targetFile
    
                dirs = os.path.split(targetFile)[0]
                if os.path.exists(dirs) == False:
                    os.makedirs(dirs)
            
                produce_spectrogram(sourceFile,targetFile)
    
    print "finish.."


#将每个人的多张小图片合并为一张大图片
def produce_one_big_png_for_each_speaker(sourceRoot="E:/DeepLearning/data/TIMIT/png_data/all_in_one_update_20170107",
                                         targetRoot="E:/DeepLearning/data/TIMIT/png_data/big_png_update_20170107"):
    root_path = os.path.abspath(sourceRoot)
    speakers = [x for x in os.listdir(root_path) if os.path.isdir(os.path.join(root_path, x))]
    
    for index,speaker in enumerate(speakers):
        
        speaker_dir = os.path.join(root_path, speaker)
        
        png_files = [x for x in os.listdir(speaker_dir) 
                    if os.path.isfile(os.path.join(speaker_dir, x)) 
                    and os.path.splitext(os.path.join(speaker_dir, x))[1]=='.png']
        
        print "file in:",speaker_dir
        
        cols = 0
        for png_file in png_files:
            abs_path = os.path.join(speaker_dir, png_file)
            img = img_read_and_write.read_img(abs_path)
            cols += img.shape[1]
            
        big_img = np.zeros((128,cols))
                
        index = 0
        for png_file in png_files:
            abs_path = os.path.join(speaker_dir, png_file)
            img = img_read_and_write.read_img(abs_path)
            col = img.shape[1]
            big_img[:,index:index+col] = img
            index += col
        
        big_img_path = os.path.join(os.path.abspath(targetRoot), speaker+'.png')
        img_read_and_write.write_img(big_img, big_img_path)
        print "successfuly create:"+big_img_path
        

def produce_spectrogram(audiopath="c:/axhev.wav",plotpath="c:/sa1.wav.128.sd22.png",n_fft=512,hop_length=160,use_librosa = True):

    samplerate, samples = wav.read(audiopath)
    
    if use_librosa:  #采用librosa库进行STFT
        import librosa
        fft_complex = librosa.stft(samples,n_fft=n_fft,hop_length=hop_length,center=False)
        fft_abs = np.abs(fft_complex)
    else:       
        fft_complex = stft(samples, n_fft, hop_length)
        fft_abs = np.abs(fft_complex)
        fft_abs = np.transpose(fft_abs)

    epsilon = 1e-9
    fft_db = 20.*np.log10(fft_abs+epsilon) 
    
    max_val = np.max(fft_db)
    min_val = np.min(fft_db)
    gap = max_val - min_val
    print "maxDB:{},minDB:{},gap:{}".format(max_val,min_val,gap)
    
    pixel = (fft_db - min_val ) * 255.0 / float(gap)
    print "pixel:",pixel
    print "pixel.shape:",pixel.shape
    
    pixel = pixel[0:128,:]
    
    img_read_and_write.write_img(pixel, plotpath)
    
    
    
# produce_spectrogram_for_each_wav("E:/DeepLearning/data/china-celebrity-speech/0406/png/wav",
#                         "E:/DeepLearning/data/china-celebrity-speech/0406/png/png")
# produce_one_big_png_for_each_speaker("E:/DeepLearning/data/china-celebrity-speech/0406/png/png",
#                                      "E:/DeepLearning/data/china-celebrity-speech/0406/png/all_in_one")
# produce_spectrogram("c:/sa1_1s.wav","c:mm.png");