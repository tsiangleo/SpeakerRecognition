#!/usr/bin/env python
#coding: utf-8

import numpy as np
import os
import PIL.Image as Image

def read_img(img_path):
    abs_path = os.path.abspath(img_path)
#     img = np.array(Image.open(abs_path))
    img = np.array(Image.open(abs_path).convert('L'))
    return img

def write_img(data_array,img_path):
    imges = Image.fromarray(data_array).convert('L')
    imges.save(img_path,'png')
    
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
            img = read_img(abs_path)
            cols += img.shape[1]
            
        big_img = np.zeros((128,cols))
                
        index = 0
        for png_file in png_files:
            abs_path = os.path.join(speaker_dir, png_file)
            img = read_img(abs_path)
            col = img.shape[1]
            big_img[:,index:index+col] = img
            index += col
        
        big_img_path = os.path.join(os.path.abspath(targetRoot), speaker+'.png')
        write_img(big_img, big_img_path)
        print "successfuly create:"+big_img_path

#produce_one_big_png_for_each_speaker("E:/DeepLearning/data/china-celebrity-speech/0406/png/java_png", "E:/DeepLearning/data/china-celebrity-speech/0406/png/java_all_in_one")
