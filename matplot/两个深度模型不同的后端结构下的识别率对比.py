#!/usr/bin/env python
#coding: utf-8

# https://zhuanlan.zhihu.com/p/24952180

import numpy as np
import sys
reload(sys)

sys.setdefaultencoding('utf-8')

import matplotlib.pyplot as plt
import matplotlib
simSunfont = matplotlib.font_manager.FontProperties(fname='C:/Python27/simsun/simsun.ttc')

#%matplotlib inline #使图片内嵌交互环境显示

plt.rcParams['font.sans-serif']=['SimHei'] #用来正常显示中文标签
plt.rcParams['axes.unicode_minus']=False #用来正常显示负号

CNN_DNN_1   = (78.12,79.85,81.18)
CNN_LSTM_1   = (79.87,80.64,82.38)

CNN_DNN_3   = (79.92,80.23,82.46)
CNN_LSTM_3   = (80.92,81.63,83.27)

CNN_DNN_5   = (80.12,81.13,82.62)
CNN_LSTM_5   = (81.22,83.53,84.02)


plt.figure(figsize=(4, 3))

bar_width = 0.1       # the width of the bars


CNN_DNN_1_index = [0.1,0.4,0.7]
CNN_LSTM_1_index =[i+bar_width for i in CNN_DNN_1_index]

plt.subplot(311)  
plt.bar(CNN_DNN_1_index,CNN_DNN_1,color='darkorange',width = bar_width,alpha=1.0,label='单层DNN：CNN+DNN')
plt.bar(CNN_LSTM_1_index,CNN_LSTM_1,color='deepskyblue',width = bar_width,alpha=1.0,label='单层RNN：CNN+RNN')
plt.xlim(0,1.3)
plt.ylim(70,85)
plt.yticks(fontsize=20,fontproperties=simSunfont)
# plt.xlabel('层节点数',fontsize=20)
plt.ylabel('准确率(百分比)',fontsize=20,fontproperties=simSunfont)
plt.title('两个深度模型不同的后端结构下的识别率对比',fontsize=20,fontproperties=simSunfont)
plt.legend(loc='upper right',fontsize=20,prop=simSunfont)
leg = plt.gca().get_legend()
ltext  = leg.get_texts()
plt.setp(ltext, fontsize=20)
plt.grid(True)
plt.xticks([i+ bar_width for i in CNN_DNN_1_index],['128','256','512'],fontsize=20,fontproperties=simSunfont)


plt.subplot(312)  
plt.bar(CNN_DNN_1_index,CNN_DNN_3,color='darkorange',width = bar_width,alpha=1.0,label='3层DNN：CNN+DNN')
plt.bar(CNN_LSTM_1_index,CNN_LSTM_3,color='deepskyblue',width = bar_width,alpha=1.0,label='3层RNN：CNN+RNN')
plt.xlim(0,1.3)
plt.ylim(70,85)
plt.yticks(fontsize=20,fontproperties=simSunfont)
# plt.xlabel('层节点数',fontsize=20)
plt.ylabel('准确率(百分比)',fontsize=20,fontproperties=simSunfont)
# plt.title('3层时的识别率',fontsize=20)
plt.legend(loc='upper right',fontsize=20,prop=simSunfont)
leg = plt.gca().get_legend()
ltext  = leg.get_texts()
plt.setp(ltext, fontsize=20)
plt.grid(True)
plt.xticks([i+ bar_width for i in CNN_DNN_1_index],['128','256','512'],fontsize=20,fontproperties=simSunfont)

plt.subplot(313)  
plt.bar(CNN_DNN_1_index,CNN_DNN_5,color='darkorange',width = bar_width,alpha=1.0,label='5层DNN：CNN+DNN')
plt.bar(CNN_LSTM_1_index,CNN_LSTM_5,color='deepskyblue',width = bar_width,alpha=1.0,label='5层RNN：CNN+RNN')
plt.xlim(0,1.3)
plt.ylim(70,85)
plt.yticks(fontsize=20,fontproperties=simSunfont)
plt.xlabel('每层节点数',fontsize=20,fontproperties=simSunfont)
plt.ylabel('准确率(百分比)',fontsize=20,fontproperties=simSunfont)
# plt.title('5层时的识别率',fontsize=20)
plt.legend(loc='upper right',fontsize=20,prop=simSunfont)
leg = plt.gca().get_legend()
ltext  = leg.get_texts()
plt.setp(ltext, fontsize=20)
plt.grid(True)
plt.xticks([i+ bar_width for i in CNN_DNN_1_index],['128','256','512'],fontsize=20,fontproperties=simSunfont)

plt.show()
