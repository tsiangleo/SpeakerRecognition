#!/usr/bin/env python
#coding: utf-8

# https://zhuanlan.zhihu.com/p/24952180

import numpy as np
import sys
reload(sys)

sys.setdefaultencoding('utf-8')

import matplotlib.pyplot as plt

#%matplotlib inline #使图片内嵌交互环境显示
import matplotlib
simSunfont = matplotlib.font_manager.FontProperties(fname='C:/Python27/simsun/simsun.ttc')

plt.rcParams['font.sans-serif']=['SimHei'] #用来正常显示中文标签
plt.rcParams['axes.unicode_minus']=False #用来正常显示负号

LSTM       = (77.54,79.14,81.30)
DNN_LSTM   = (81.02,85.79,87.23)
CNN_LSTM   = (81.37,86.18,88.15)

plt.figure(figsize=(4, 3))

bar_width = 0.16       # the width of the bars

Node128_index = [0.2,1.0,1.8]
Node256_index =[i+bar_width for i in Node128_index]
Node512_index =[i+bar_width for i in Node256_index]

plt.bar(Node128_index,LSTM,color='limegreen',width = bar_width,alpha=1.0,label='输入原始语谱图：RNN')
plt.bar(Node256_index,DNN_LSTM,color='darkorange',width = bar_width,alpha=1.0,label='用DNN提取特征：DNN+RNN')
plt.bar(Node512_index,CNN_LSTM,color='deepskyblue',width = bar_width,alpha=1.0,label='用CNN提取特征：CNN+RNN')

plt.xlim(0,3.5)
plt.ylim(70,92)
plt.yticks(fontsize=20,fontproperties=simSunfont)
plt.xlabel('不同的后端结构',fontsize=20,fontproperties=simSunfont)
plt.ylabel('准确率(百分比)',fontsize=20,fontproperties=simSunfont)
plt.title('三个深度模型不同的说话人特征提取方式下的识别率对比',fontsize=20,fontproperties=simSunfont)
plt.legend(loc='upper right',fontsize=20,prop=simSunfont)
leg = plt.gca().get_legend()
ltext  = leg.get_texts()
plt.setp(ltext, fontsize=20)

plt.grid(True)
plt.xticks([i+ bar_width/2 for i in Node256_index],['1层RNN','3层RNN','5层RNN'],fontsize=20,fontproperties=simSunfont)
plt.show()
