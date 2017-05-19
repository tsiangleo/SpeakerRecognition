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

Node128 = (80.15,83.90,85.12,86.69)
Node256 = (81.37,86.18,88.15,90.01)
Node512 = (82.34,86.59,88.17,88.97)

plt.figure(figsize=(4, 3))

bar_width = 0.16       # the width of the bars

Node128_index = [0.2,1.0,1.8,2.6]
Node256_index =[i+bar_width for i in Node128_index]
Node512_index =[i+bar_width for i in Node256_index]

plt.bar(Node128_index,Node128,color='limegreen',width = bar_width,alpha=1.0,label='层节点数为128')
plt.bar(Node256_index,Node256,color='darkorange',width = bar_width,alpha=1.0,label='层节点数为256')
plt.bar(Node512_index,Node512,color='deepskyblue',width = bar_width,alpha=1.0,label='层节点数为512')

plt.xlim(0,4.0)
plt.ylim(70,92)
plt.xlabel('不同的RNN层数',fontsize=20,fontproperties=simSunfont)
plt.ylabel('准确率(百分比)',fontsize=20,fontproperties=simSunfont)
plt.yticks(fontsize=20,fontproperties=simSunfont)
plt.title('不同RNN层数和层节点数时系统的识别率',fontsize=20,fontproperties=simSunfont)
plt.legend(loc='upper right',fontsize=20,prop=simSunfont)
leg = plt.gca().get_legend()
ltext  = leg.get_texts()
plt.setp(ltext, fontsize=20)

plt.grid(True)
plt.xticks([i+ bar_width/2 for i in Node256_index],['1','3','5','7'],fontsize=20,fontproperties=simSunfont)
plt.show()
