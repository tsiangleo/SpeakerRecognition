#!/usr/bin/env python
#coding: utf-8

"""
功能：读写图片

"""

import os
import numpy as np
import PIL.Image as Image


def read_img(img_path):
#     abs_path = os.path.abspath(img_path)
#     img = np.array(Image.open(abs_path))
# #     print "img.shape and img.dtype:",img.shape,img.dtype
#     #print "read img:",img
#     return img
    abs_path = os.path.abspath(img_path)
    img = np.array(Image.open(abs_path).convert('L')) #java生成的是argb，每个像素点是一个４维的值。
    print "img.shape and img.dtype:",img.shape,img.dtype
    #print "read img:",img
    return img

def write_img(data_array,img_path):
    imges = Image.fromarray(data_array).convert('L')
    imges.save(img_path,'png')
    
