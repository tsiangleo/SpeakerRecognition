#!/usr/bin/env python
#coding: utf-8



import input_data
import tensorflow as tf
import os.path
import  img_read_and_write
import  numpy as np
from sklearn.ensemble._gradient_boosting import np_bool
from scipy.stats.vonmises_cython import numpy

#配置全局变量
time_step = 1
spectrum_width = 100
spectrum_hight = 128
num_classes=5
batch_size=16
#这个batch_size必须和在训练是的bath_size一致。可以训练好网络后从模型文件再运行一次，讲batch_size设置为1。保存到模型文件。

#要识别一张图片，则图片的大小至少是(spectrum_hight,spectrum_width*time_step)
#
            
def read_png(img_path="zujsb.png",
                 spectrum_width = 100,
                 spectrum_hight = 128,):
    
    """
    读取要测试的语音对应的语谱图。
    返回值：numpy array，shape是(n,spectrum_hight,spectrum_width*time_step)。
    n是img_path这张图片能产生符合要求的小图片的张数。
    """

    img_data = img_read_and_write.read_img(img_path)
    #要识别一张图片，则图片的大小至少是(spectrum_hight,spectrum_width*time_step)
    print img_data.shape
    img_height,png_cols =  img_data.shape
    if png_cols < spectrum_width:
        raise Exception("图片太小，图片宽度至少是："+spectrum_width)
    if img_height != spectrum_hight:
        raise Exception("图片的高度必须是："+spectrum_hight)
    
    spectr_cols = spectrum_width 
    
    #记录一整张提取后的spectrum的数据
    one_big_spec = []
    #每spectr_cols列看成一张语谱图
    for indx in xrange(0,png_cols,spectr_cols):
        start = indx
        end = indx + spectr_cols
        if end <= png_cols:
            for r in range(spectrum_hight): 
                #依次读取start列到end（不含）列的数据
                for c in range(start,end):
                    one_big_spec.append(img_data[r][c])
                    
    #提取的所有语谱图
    result = np.array(one_big_spec).reshape((-1,spectrum_hight,spectr_cols))
    print result.shape
    return result
            

#适配成batch_size，不足的补０．
def adpat_to_batch_size(img_datas,batch_size):
    length,H,W = img_datas.shape
    if length % batch_size == 0:
        return length,img_datas
    
    new_lenght = ( length / batch_size +1 )*batch_size
    
    new_data = np.zeros((new_lenght,H,W))
    new_data[:length,:,:] = img_datas
    
    return length,new_data
 

def read_key(map_file="speaker_recognition_label_strings.txt"):
    f = open(map_file,'r')
    lines = f.readlines()
    result = [e[:-1] for e in lines] #去掉末尾的换行符
    return result

def recognize(png_path="output/cnnv14_1rnn_512/png/zhy.png"):
    """
    png_path：要预测的语谱图所在路径
    
    """
    with tf.Graph().as_default():
        output_graph_def = tf.GraphDef()
#         output_graph_path = "output/cnnv14_1rnn_512/const_graph/graph.pb"
        output_graph_path = "output/cnnv14_1rnn_512/const_graphgraph.pb"
        #sess.graph.add_to_collection("input", mnist.test.images)
    
        with open(output_graph_path, "rb") as f:
            output_graph_def.ParseFromString(f.read()) #rb
    #         text_format.Merge(f.read(), output_graph_def) #r
            _ = tf.import_graph_def(output_graph_def, name="")
    
        with tf.Session() as sess:
    
            tf.global_variables_initializer().run()

            
    #         for node in tf.get_default_graph().node:
            for node in output_graph_def.node:
                print node.name
            
            input_x = sess.graph.get_tensor_by_name("input:0")
            print input_x
            out_softmax = sess.graph.get_tensor_by_name("out_softmax:0")
            print out_softmax
            keep_prob = sess.graph.get_tensor_by_name("keep_prob_placeholder:0")
            print keep_prob
#             out_label = sess.graph.get_tensor_by_name("output:0")
#             print out_label
             
            #用户传进来的图片应该是 spectrum_hight×spectrum_width×time_step大小的
            # threshold
            img_datas = read_png(png_path)
            print "实际数据大小：",img_datas.shape
            #包含多少张图片
            real_data_length,img_data= adpat_to_batch_size(img_datas,batch_size)
            print "adapt后数据大小：",img_data.shape
            
            img_data_padding_length = len(img_data)
            left_data_length = real_data_length
            #累计img_data_padding_length张图片属于各个标签的概率
            softmax_sum_maxtrix = np.zeros((batch_size,num_classes))
            for indx in xrange(0,img_data_padding_length,batch_size):
                start = indx
                end = indx + batch_size
                if end <= img_data_padding_length:
                        test_batch_xs = img_data[start:end].reshape([batch_size, time_step, spectrum_width*spectrum_hight])
                        out_softmax_in_batch = sess.run(out_softmax, feed_dict={
                            input_x: test_batch_xs,
                            keep_prob: 1.0,
                        })
#                         print "predict_val.shape:", out_softmax_in_batch.shape
#                         print "predict_val:", out_softmax_in_batch
                        
                        left_data_length -= batch_size
                        if left_data_length < batch_size:
                                new_data = np.zeros((batch_size,num_classes))
                                new_data[:left_data_length,:] = out_softmax_in_batch[:left_data_length,:]
                                softmax_sum_maxtrix = np.add(softmax_sum_maxtrix,new_data)
                        else:
                                softmax_sum_maxtrix = np.add(softmax_sum_maxtrix,out_softmax_in_batch)
             
            print "softmax_sum_maxtrix:",softmax_sum_maxtrix
            t_sum =  np.sum(softmax_sum_maxtrix, axis=0)
            print "t_sum:",t_sum
            print "real_data_length:",real_data_length
            average =  np.divide(t_sum,float(real_data_length))
            print "属于各个标签的概率:",average
            print "各个标签的概率之和：",np.sum(average)
            predict_label= np.argmax(average)
            print "预测的标签值:",predict_label
            predict_name = read_key()[predict_label]
            print "预测的姓名:",predict_name

#第二种预测方式
def recognize_v2(png_path="output/cnnv14_1rnn_512/png/zhy.png"):
    """
    png_path：要预测的语谱图所在路径
    
    """
    with tf.Graph().as_default():
        output_graph_def = tf.GraphDef()
        output_graph_path = "output/sid.pb"
        #sess.graph.add_to_collection("input", mnist.test.images)
    
        with open(output_graph_path, "rb") as f:
            output_graph_def.ParseFromString(f.read()) #rb
    #         text_format.Merge(f.read(), output_graph_def) #r
            _ = tf.import_graph_def(output_graph_def, name="")
    
        with tf.Session() as sess:
    
            tf.global_variables_initializer().run()

            
    #         for node in tf.get_default_graph().node:
            for node in output_graph_def.node:
                print node.name
            
            input_x = sess.graph.get_tensor_by_name("input:0")
            print input_x
            out_softmax = sess.graph.get_tensor_by_name("out_softmax:0")
            print out_softmax
            keep_prob = sess.graph.get_tensor_by_name("keep_prob_placeholder:0")
            print keep_prob
            out_label = sess.graph.get_tensor_by_name("output:0")
            print out_label
             
            #用户传进来的图片应该是 spectrum_hight×spectrum_width×time_step大小的
            # threshold
            img_datas = read_png(png_path)
            print "实际数据大小：",img_datas.shape
            #包含多少张图片
            real_data_length,img_data= adpat_to_batch_size(img_datas,batch_size)
            print "adapt后数据大小：",img_data.shape
            
            img_data_padding_length = len(img_data)
            left_data_length = real_data_length
            out_label_list = []
            #累计img_data_padding_length张图片属于各个标签的概率
            softmax_sum_maxtrix = np.zeros((batch_size,num_classes))
            for indx in xrange(0,img_data_padding_length,batch_size):
                start = indx
                end = indx + batch_size
                if end <= img_data_padding_length:
                        test_batch_xs = img_data[start:end].reshape([batch_size, time_step, spectrum_width*spectrum_hight])
                        out_label_in_batch = sess.run(out_label, feed_dict={
                            input_x: test_batch_xs,
                            keep_prob: 1.0,
                        })
#                         print "predict_val.shape:", out_label_in_batch.shape
#                         print "predict_val:", out_label_in_batch
                        
                        out_label_list.append(out_label_in_batch)

             
            print "out_label_list:",out_label_list
            
            #统计每张语谱图属于各个标签的次数
            counter = np.zeros(num_classes)
            indx = 0
            for one_batch_label in out_label_list:
                for label in one_batch_label:
                    if indx < real_data_length:
                        counter[label] +=1
                        indx +=1
                    else:
                        print "无效label：",label
            
            print "counter after:",counter
            print "counter之和：",np.sum(counter)
            assert np.sum(counter) == real_data_length
            prob =  np.divide(counter,float(real_data_length))
            print "counter概率：",prob
            predict_label= np.argmax(prob)
            print "预测的标签值:",predict_label
            predict_name = read_key()[predict_label]
            print "预测的姓名:",predict_name
                

recognize("/media/tsiangleo/0FD7048F0FD7048F/DeepLearning/data/china-celebrity-speech/0406/png/png/马云/qrjsysdw.png")
# recognize_v2("output/cnnv14_1rnn_512/png/yd.png")

