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

CDRNN     = (90.01,89.43,89.18)
GMM_UBM   = (80.02,75.39,72.53)



plt.figure(figsize=(4, 3))

bar_width = 0.1       # the width of the bars


CNN_DNN_1_index = [0.1,0.4,0.7]
CNN_LSTM_1_index =[i+bar_width for i in CNN_DNN_1_index]
plt.bar(CNN_DNN_1_index,GMM_UBM,color='deepskyblue',width = bar_width,alpha=1.0,label='经典方法GMM-UBM')
plt.bar(CNN_LSTM_1_index,CDRNN,color='darkorange',width = bar_width,alpha=1.0,label='本文方法CDRNN')
plt.xlim(0,1.3)
# plt.ylim(0,100)
plt.xlabel('说话者人数',fontsize=20,fontproperties=simSunfont)
plt.ylabel('准确率(百分比)',fontsize=20,fontproperties=simSunfont)
plt.title('两个深度模型不同的后端结构下的识别率对比',fontsize=20,fontproperties=simSunfont)
plt.legend(loc='upper right',fontsize=20,prop=simSunfont)
plt.grid(True)
plt.xticks([i+ bar_width for i in CNN_DNN_1_index],['10','20','30'],fontsize=12)

plt.show()
