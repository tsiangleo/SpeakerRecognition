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

plt.rcParams['font.sans-serif']=['SimSun'] #用来正常显示中文标签
plt.rcParams['axes.unicode_minus']=False #用来正常显示负号

CDRNN       = (90.01,89.43,89.18)
GMM_UBM   = (80.02,75.39,72.53)


x1=[i+1 for i in range(0,len(CDRNN))]
x2=x1

plt.figure(figsize=(10, 6))
plt.plot(x1,CDRNN,'-D',label="本文方法CDRNN",color="deepskyblue",linewidth=3)
plt.plot(x2,GMM_UBM,'-o',label="经典方法GMM-UBM",color="darkorange",linewidth=3)

plt.xlabel('说话者人数',fontsize=20,fontproperties=simSunfont)
plt.ylabel('准确率(百分比)',fontsize=20,fontproperties=simSunfont)
plt.xlim(0,4)
plt.ylim(50,100)
plt.yticks(fontsize=20,fontproperties=simSunfont)
plt.title('本文方法和经典方法的对比',fontsize=20,fontproperties=simSunfont)
plt.legend(loc='upper right',fontsize=20,prop=simSunfont)
leg = plt.gca().get_legend()
ltext  = leg.get_texts()
plt.setp(ltext, fontsize=20)
plt.grid(True)
plt.xticks((0,1,2,3,4),['0','10','20','30','40'],fontsize=20,fontproperties=simSunfont)
# plt.grid(x1)

plt.show()
