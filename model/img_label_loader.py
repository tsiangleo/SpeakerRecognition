#!/usr/bin/env python
#coding: utf-8


import os
import numpy as np
from sklearn.model_selection import StratifiedKFold
from sklearn.model_selection import StratifiedShuffleSplit

import img_read_and_write
  


def load_img(img_path,
             spectr_rows,
             spectr_cols,
             num_of_speaker,
             hop_size = None, 
             ):
    
    """
    参数：
        spectr_rows: 语谱图的高度
        spectr_cols：把16列看成一张语谱图
        hop_size：代表每两张语谱图之间的帧移，若hop_size=spectr_cols，则没有重叠了。默认hop_size=spectr_cols。
        num_of_speaker：要获取多少个speaker的语谱图，该值设置过大容易撑爆内存。
        
          
    若一帧的左边和右边分别为left，right，假设timestep个帧作为一个input sequence输入到RNN。
    则一个RNN input sequence需要消耗语谱图的长度为left+right+timestep，即每left+right+timestep列打上一个label。
    一个长度为N的语谱图能生成N-spectr_cols+1=N-left-right-timestep+1张这样重叠的小的语谱图（input sequence）。

        
    返回值：
        长度为num_of_speaker的一个list，每个元素是(filename,data)。data是numy array，shape是(num_of_spectrum_per_speaker,spectr_rows,spectr_cols)。

    """
    
    #默认hop_size=spectr_cols
    if hop_size is None:
        hop_size = spectr_cols
    
    root_dir = os.path.abspath(img_path)

    
    png_files = [x for x in os.listdir(root_dir) 
                        if os.path.isfile(os.path.join(root_dir, x)) 
                        and os.path.splitext(os.path.join(root_dir, x))[1]=='.png']
    num_of_imgs = len(png_files)
    
    if num_of_speaker > num_of_imgs:
        raise ValueError(
        'num_of_speaker should be between 0 and {}. Received: {}.'
        .format(num_of_imgs, num_of_speaker))
    
    #一个list，每个元素是一个speaker的语谱图，每个speaker的语谱图存放在一个numpy array中.
    speakers_spectrum_list = []
    
    #记录一共能产生多少张语谱图
    total_spec_num = 0
    
    for index in range(num_of_speaker):
        file = png_files[index]
        abs_path = os.path.join(root_dir, file)
        img = img_read_and_write.read_img(abs_path)
        #获取图片的列数
        png_cols = img.shape[1]
          
        #记录一张图片能得到多少张256*spectr_cols大小的语谱图
        spec_num_per_img = 0
        
        #记录一整张提取后的spectrum的数据
        one_big_spec = []
        
       
        for indx in xrange(0,png_cols,hop_size):
            start = indx
            end = indx + spectr_cols    #每spectr_cols列看成一张语谱图
            if end <= png_cols:
                piece = img[:,start:end]
                one_big_spec.append(piece)
            
                #print "已处理第",start,"列~",end,"列";
                spec_num_per_img += 1
            
        print "已经处理完第",str(index + 1),"张图片:",abs_path,"shape:", img.shape
        
        #提取后的一个speak的所有语谱图
        one_speaker_img_data = np.array(one_big_spec)
        speakers_spectrum_list.append((file,one_speaker_img_data))
        
        print "one_speaker_img_data.shape:",one_speaker_img_data.shape
        
        assert spec_num_per_img == one_speaker_img_data.shape[0]
        
        print "第",str(index + 1),"张图片共获得",str(spec_num_per_img),"张",str(spectr_rows),"*",str(spectr_cols),"语谱图"
        total_spec_num += spec_num_per_img
        
    print "一共获得",total_spec_num,"张",str(spectr_rows),"*",str(spectr_cols),"语谱图"
    
    assert len(speakers_spectrum_list) == num_of_speaker
    
    return speakers_spectrum_list

def _dense_to_one_hot(labels_dense, num_classes):
    """Convert class labels from scalars to one-hot vectors."""
    num_labels = labels_dense.shape[0]
    index_offset = np.arange(num_labels) * num_classes
    labels_one_hot = np.zeros((num_labels, num_classes))
    labels_one_hot.flat[index_offset + labels_dense.ravel()] = 1
    return labels_one_hot


def data_loader_warpper(img_path, spectr_rows, spectr_cols, num_of_speaker,
                        hop_size=None,#生成语谱图时两张语谱图之间的帧移，若为None，则默认等于spectr_cols，即不重叠。
                        one_hot=True,
                        ):
        
    """
    加载图片
        
    返回值:
        二元tuple(img,label)。
        img：归一化后的语谱图，各个像素点取值在0-1之间。numpy array shape: (num_of_total_spectrum, spectr_rows,spectr_cols)
        label：标签值numpy array。若one_hot，则 shape:(num_of_total_spectrum,num_of_speaker), 否则 shape:(num_of_total_spectrum,)
    
    """
    
    speakers_spectrum_list = load_img(img_path, spectr_rows, spectr_cols, num_of_speaker, hop_size)
    
    toatal_img_num = 0
    
    img_list = []
    label_list = []
    
    label_index = 0
    
    file = open("speaker_recognition_label_strings.txt",'w')
    for filename,all_spectrum_of_a_speaker in  speakers_spectrum_list:
        #该speaker的语谱图的数量
        one_speaker_num_spec = all_spectrum_of_a_speaker.shape[0]
        toatal_img_num += one_speaker_num_spec 
        
        #处理一个speaker的所有语谱图
        for each_spectrum in all_spectrum_of_a_speaker:
            img_list.append(each_spectrum)
            label_list.append(label_index)
            
        #保存filename和label的对应关系。
        print str(filename)," --- ",label_index
        file.write(str(filename)+"\n")
        #标签值加1
        label_index += 1
    
    file.close()
    
    assert len(img_list)    == toatal_img_num
    assert len(label_list)   == toatal_img_num
    
    
    #img进行归一化，Convert from [0, 255] -> [0.0, 1.0].
    img_list = np.multiply(img_list, 1.0 / 255.0)
    
    label_list = np.array(label_list)
    #label进行one_hot处理
    if one_hot:
        label_list =  _dense_to_one_hot(np.array(label_list), num_of_speaker)

    print "img_list.shape:",img_list.shape
    print "label_list.shape:",label_list.shape
     
    return img_list,label_list
         
                                      
def get_cross_validation_data(img_path="/media/tsiangleo/0FD7048F0FD7048F/DeepLearning/data/TIMIT/png_data/big_png_update_20170107",
          spectr_rows=128,
           spectr_cols=100,
            num_of_speaker=4,
            num_of_folds = 2, #切分成多少份
            test_size = 0.2,
            hop_size=None,#生成语谱图时两张语谱图之间的帧移，若为None，则默认等于spectr_cols，即不重叠。
            ):
    
    imgs,labels = data_loader_warpper(img_path, spectr_rows, spectr_cols, num_of_speaker, hop_size,one_hot=False)
    length = len(imgs)
    print length

#     skf = StratifiedKFold(n_splits=num_of_folds)
    sss = StratifiedShuffleSplit(n_splits=num_of_folds, test_size=test_size, random_state=0)
    for train, test in sss.split(np.arange(length), labels):
        print "train.shape:",train.shape,",train:",train
        print "test.shape:",test.shape,",test:",test
        
        labels_one_hot = _dense_to_one_hot(labels, num_of_speaker)
        imgs_train, imgs_test, labels_train, labels_test = imgs[train], imgs[test], labels_one_hot[train], labels_one_hot[test]
        print "imgs_train.shape",imgs_train.shape
        print "labels_train.shape",labels_train.shape
        print "imgs_test.shape",imgs_test.shape
        print "labels_test.shape",labels_test.shape   
        
        return imgs_train, imgs_test, labels_train, labels_test


def test():    
    imgs,labels  = data_loader_warpper(img_path="/media/tsiangleo/0FD7048F0FD7048F/DeepLearning/data/TIMIT/png_data/big_png_update_20170107", 
         spectr_rows=128, 
         spectr_cols=100, 
         num_of_speaker=10,
         one_hot=False)
    
    imgs =  np.multiply(imgs, 255.0)
    for index,img in enumerate(imgs):
        img_read_and_write.write_img(img, "/media/tsiangleo/0FD7048F0FD7048F/DeepLearning/data/TIMIT/png_data/"+str(index)+".png")

# img_path="/media/tsiangleo/8A5C046B5C0453FB/sitw_512_160/ABXG"
# spectr_rows=128
# spectr_cols=16
# num_of_speaker=1
# hop_size=1
# load_img(img_path, spectr_rows, spectr_cols, num_of_speaker, hop_size)