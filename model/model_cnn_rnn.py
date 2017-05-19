#!/usr/bin/env python
#coding: utf-8

import tensorflow as tf
import os
import input_data
from tensorflow.python.framework import graph_util



#添加全连接层
def add_fc_layer(inputs, in_size, out_size, activation_function=None):
    """
    入参：
        in_size：上一层神经元的个数。
        out_size：本层神经元的个数。
        input：None个长度为in_size一维向量（None行in_size列，shape(None,in_size)），即用一行来存储一个特征值输入。
    
    返回值：
        输入一个长度为in_size的一维向量，则返回一个长度为out_size的向量。
        输入batch_size个长度为in_size的一维向量，则返回batch_size个长度为out_size的向量。
    """
    #保存上一层到本层之间的连接的权重
    weight = tf.Variable(tf.random_normal([in_size, out_size])) # in_size行out_size列，shape:(in_size,out_size)
    #保存本层的所有神经元的偏置
    biases = tf.Variable(tf.zeros([1, out_size]) + 0.1)#1行out_size列，shape:(1,out_size)
    XW_plus_b = tf.matmul(inputs, weight) + biases
    if activation_function is None:
        outputs = XW_plus_b
    else:
        outputs = activation_function(XW_plus_b)
    return outputs



#添加RNN层
def add_dynamic_rnn_layer(inputs,out_size,batch_size,Xt_size,time_step,num_layer=1,keep_prob=0.5):
      
    """
    设网络在t时刻的输入为Xt，Xt是一个n维向量。
    s个时刻的输入X组成一整个序列，也就是[X0,...,Xs−1,Xs,]，s为time step。
    
    入参：
        batch_size:
        out_size：RNN层自身的神经元的个数。
        Xt_size：X网络在t时刻的输入为Xt，Xt是一个n维向量。Xt_size等于n。
        inputs：这里input代表一次输入序列。不是t时刻的Xt，而是time_step个Xt组成的输入序列。flatted为一维向量。
                    如果有batch_size个这样的序列，则有batch_size个这样的序列。
                    shape:(batch_size,Xt_size*time_step)。
    返回值：
        一共有time_step个时刻，这里返回的是最后一个时刻的该RNN输出。shape为(batch_size, out_size)
    
    疑问？！
        这里一个sequence对应一个label，而非一个Xt对应一个label。这里的time_step个Xt的label值是相同的。
        即组成这个sequence的每个Xt的label都相同。
        
        既然功能是添加一层网络，那么输入到该层的所有input表征的是同一个类别。即一个（x，y）样本点。
        
    """
    # Reshaping to (batch_size, time_step, Xt_size)
    inputs = tf.reshape(inputs, [-1, time_step,Xt_size])

    cell = tf.nn.rnn_cell.LSTMCell(out_size, forget_bias=1.0)
    cell =  tf.nn.rnn_cell.DropoutWrapper(cell, output_keep_prob=keep_prob)
    cell = tf.nn.rnn_cell.MultiRNNCell([cell] * num_layer)
#     cell =  tf.nn.rnn_cell.DropoutWrapper(cell,  input_keep_prob=keep_prob)


    init_state = cell.zero_state(batch_size, tf.float32)
    rnn_outputs, final_state = tf.nn.dynamic_rnn(cell, inputs, initial_state=init_state,time_major=False)
    
    """
    rnn_outputs的shape是:[batch_size, time_step, out_size]
    tf.transpose(rnn_outputs, [1, 0, 2])后rnn_outputs的shape变为：(time_step,batch_size, out_size)
    然后再tf.unpack()，得到一个长度为time_step的list，其中的每个元素的shape为(batch_size, out_size)
    """
    rnn_outputs = tf.unpack(tf.transpose(rnn_outputs, [1, 0, 2])) 
    
    #返回最后一个time_step的输出。
    return rnn_outputs[-1]


def build_net_cnn_rnn(time_step=10,
              spectrum_hight=256,#代表每列语谱图的高度
              spectrum_width = 128,#语谱图的宽度
              num_classes=10,
              rnn_out_size = 128, #RNN层的内部的神经元的个数
              rnn_layers = 2,
              batch_size = 16,
              ):
    
    """
    同build_net_v4，只是cnn的输出直接给rnn，不经过fc层。
    
    参数：
        spectrum_hight：代表每列语谱图的高度
        spectrum_width：语谱图的宽度
            一张spectrum_hight*spectrum_width的语谱图为一个t时刻的输入Xt。
            time_step个连续这样的语谱图作为一个sequence输入到RNN，输出一个label值。
        
        
    网络结构：
    
    input层         num_of_units：spectrum_hight*spectrum_width     即将spectrum_hight*spectrum_width的图像看做一个特征输入。
        |
    CNN网络
        |
    RNN层          num_of_units：rnn_out_size
        |
    output层      num_of_units：num_classes
    """
    
    #输入层神经元的个数
    input_size = spectrum_hight * spectrum_width
    
    x_placeholder = tf.placeholder(tf.float32, [None, time_step, input_size], name='input')
    y_placeholder = tf.placeholder(tf.float32, [None, num_classes], name='labels_placeholder')
    
    #学习率,可以在训练过程中动态变化
    learning_rate = tf.Variable(0.0, dtype=tf.float32, trainable=False)
    keep_prob_placeholder = tf.placeholder(tf.float32, name='keep_prob_placeholder')

    
    #输入层,先reshape为(batch_size,time_step,spectrum_height,spectrum_width)
    X = tf.reshape(x_placeholder, [-1, time_step,spectrum_hight,spectrum_width])
    
    
    #CNN网络

    X = add_cnn_layers_updated_v12(X)
    cnn_out_H_plus_Channels = X.get_shape().as_list()[2] #获取feature_size的值
    cnn_out_W  = X.get_shape().as_list()[1] #获取feature_size的值
    print "cnn_out_H_plus_Channels:",cnn_out_H_plus_Channels
    print "cnn_out_W:",cnn_out_W
    X = tf.reshape(X,[-1,cnn_out_H_plus_Channels*cnn_out_W])
        
    #RNN层
    rnn_output = add_dynamic_rnn_layer(X, rnn_out_size, batch_size,cnn_out_H_plus_Channels, cnn_out_W,num_layer=rnn_layers,keep_prob=keep_prob_placeholder)
    print "RNN num_of_units:",rnn_out_size
    
    #output层
    logits = add_fc_layer(rnn_output, rnn_output.get_shape().as_list()[1], num_classes)
    
    
    predictions = tf.nn.softmax(logits,name="out_softmax")
    
    #平均的cost
    #tf.nn.softmax_cross_entropy_with_logits(logits, y_placeholder)
    #tf.nn.sigmoid_cross_entropy_with_logits
    #http://www.tuicool.com/articles/n22m2az
    
    cost = tf.reduce_mean(tf.nn.softmax_cross_entropy_with_logits(logits, y_placeholder))
    #在TIMIT数据集上使用GradientDescentOptimizer效果不好，数据量太小
    optimize = tf.train.AdamOptimizer(learning_rate).minimize(cost)
    
    #prediction_labels和real_labels都是一个numpy.ndarray，shape: (batch_size,)
    #每一个值是一个下标，指示取值最大的值所在的下标。简单点就是预测的标签值。
    prediction_labels = tf.argmax(predictions, axis=1,name="output")
    real_labels= tf.argmax(y_placeholder, axis=1)
    
    #correct_prediction是一个numpy.ndarray，shape: (batch_size,)，指示那些预测对了。
    correct_prediction = tf.equal(prediction_labels, real_labels)
    accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32))
    #一个Batch中预测正确的次数
    correct_times_in_batch = tf.reduce_sum(tf.cast(correct_prediction, tf.int32))
    
    
    return dict(
                keep_prob_placeholder = keep_prob_placeholder,
                learning_rate = learning_rate,
                x_placeholder = x_placeholder,
                y_placeholder = y_placeholder,
                optimize = optimize,
                logits = logits,
                prediction_labels = prediction_labels,
                real_labels = real_labels,
                correct_prediction = correct_prediction,
                correct_times_in_batch = correct_times_in_batch,
                cost = cost,
                accuracy = accuracy,
    )
def train_net_v2_tensorboard(graph,
                 dataset,
                 time_step,
                 spectrum_hight,
                 spectrum_width,
                 batch_size,
                 num_epochs,
                 init_learning_rate = 0.001,
                 checkpoints_dir="checkpoint",
                 tensorboard_dir ="tensorboard"):
    
    #tensorboard相关       
    cost_tensorboard_placeholder = tf.placeholder(tf.float32,1)
    accuracy_tensorboard_placeholder = tf.placeholder(tf.float32,1)
    tf.summary.scalar('cost', cost_tensorboard_placeholder[0])
    tf.summary.scalar('accuracy', accuracy_tensorboard_placeholder[0])
    merged = tf.summary.merge_all()
    
    
    with tf.Session() as sess:
        
        train_writer = tf.summary.FileWriter(tensorboard_dir+"/train", sess.graph) 
        test_writer  = tf.summary.FileWriter(tensorboard_dir+"/test", sess.graph)
         
        saver = tf.train.Saver()
        
        sess.run(tf.global_variables_initializer())
        
        #从模型读读取session继续训练
        checkpoint_state = tf.train.get_checkpoint_state(checkpoints_dir)  
        if checkpoint_state and checkpoint_state.model_checkpoint_path:  
            saver.restore(sess, checkpoint_state.model_checkpoint_path)  
            print "从模型文件中读取session继续训练..."
         
         
        print "batch size:",batch_size
         
        #用于控制每epoch_delta轮在train set和test set上计算一下accuracy和cost
        epoch_delta = 2
        for epoch_index in range(num_epochs):
            
            if epoch_index % 5  == 0:
                    #计算学习率
                    learnrate = init_learning_rate * (0.99 ** epoch_index) 
                    sess.run(tf.assign(graph['learning_rate'], learnrate))
                    
            #################################
            #    获取TRAIN set，开始训练网络
            #################################
            for (batch_xs,batch_ys) in dataset.train.mini_batches(batch_size):
                batch_xs = batch_xs.reshape([batch_size, time_step, spectrum_width*spectrum_hight])
                   
                sess.run([graph['optimize']], feed_dict={
                    graph['x_placeholder']: batch_xs,
                    graph['y_placeholder']: batch_ys,
                    graph['keep_prob_placeholder']:0.5,
                })
      
            
            #每epoch_delta轮在train set和test set上计算一下accuracy和cost
            if epoch_index % epoch_delta  == 0:
                #################################
                #    开始在 train set上计算一下accuracy和cost
                #################################
                #记录训练集中有多少个batch
                total_batches_in_train_set = 0
                #记录在训练集中预测正确的次数
                total_correct_times_in_train_set = 0
                #记录在训练集中的总cost
                total_cost_in_train_set = 0.
                for (train_batch_xs,train_batch_ys) in dataset.train.mini_batches(batch_size):
                    train_batch_xs = train_batch_xs.reshape([batch_size, time_step, spectrum_width*spectrum_hight])
                    return_correct_times_in_batch = sess.run(graph['correct_times_in_batch'], feed_dict={
                        graph['x_placeholder']: train_batch_xs,
                        graph['y_placeholder']: train_batch_ys,
                        graph['keep_prob_placeholder']:1.0,
                    })
                    mean_cost_in_batch = sess.run(graph['cost'], feed_dict={
                        graph['x_placeholder']: train_batch_xs,
                        graph['y_placeholder']: train_batch_ys,
                        graph['keep_prob_placeholder']:1.0,
                    })
                      
                    total_batches_in_train_set += 1
                    total_correct_times_in_train_set += return_correct_times_in_batch
                    total_cost_in_train_set  += (mean_cost_in_batch*batch_size)
                      
                    #tensorboard相关
                    train_acy_tensor = total_correct_times_in_train_set / float(total_batches_in_train_set * batch_size) * 100.0
                    train_mean_cost = total_cost_in_train_set / float(total_batches_in_train_set * batch_size)
                    train_result = sess.run(merged,feed_dict={ 
                                                                graph['x_placeholder']: train_batch_xs,
                                                                graph['y_placeholder']: train_batch_ys,
                                                                cost_tensorboard_placeholder:[train_mean_cost],
                                                                accuracy_tensorboard_placeholder:[train_acy_tensor]
                                                        })
                  
                print "Epoch -",epoch_index,", train_mean_cost:",train_mean_cost,",train_acy_tensor:",train_acy_tensor
                train_writer.add_summary(train_result, epoch_index)
                
                #################################
                #    开始在 test set上计算一下accuracy和cost
                #################################
                #记录测试集中有多少个batch
                total_batches_in_test_set = 0
                #记录在测试集中预测正确的次数
                total_correct_times_in_test_set = 0
                #记录在测试集中的总cost
                total_cost_in_test_set = 0.
                for (test_batch_xs,test_batch_ys) in dataset.test.mini_batches(batch_size):
                    test_batch_xs = test_batch_xs.reshape([batch_size, time_step, spectrum_width*spectrum_hight])
                    return_correct_times_in_batch = sess.run(graph['correct_times_in_batch'], feed_dict={
                        graph['x_placeholder']: test_batch_xs,
                        graph['y_placeholder']: test_batch_ys,
                        graph['keep_prob_placeholder']:1.0,
                    })
                    mean_cost_in_batch = sess.run(graph['cost'], feed_dict={
                        graph['x_placeholder']: test_batch_xs,
                        graph['y_placeholder']: test_batch_ys,
                        graph['keep_prob_placeholder']:1.0,
                    })
                      
                    total_batches_in_test_set += 1
                    total_correct_times_in_test_set += return_correct_times_in_batch
                    total_cost_in_test_set  += (mean_cost_in_batch*batch_size)
                      
                    #tensorboard相关
                    test_acy_tensor = total_correct_times_in_test_set / float(total_batches_in_test_set * batch_size) * 100.0
                    test_mean_cost = total_cost_in_test_set / float(total_batches_in_test_set * batch_size)
                    test_result = sess.run(merged,feed_dict={ 
                                                            graph['x_placeholder']: test_batch_xs,
                                                            graph['y_placeholder']: test_batch_ys,
                                                            graph['keep_prob_placeholder']:1.0,
                                                            cost_tensorboard_placeholder:[test_mean_cost],
                                                            accuracy_tensorboard_placeholder:[test_acy_tensor]
                                                        })
                  
                print "Epoch -",epoch_index,", test_mean_cost:",test_mean_cost,",test_acy_tensor:",test_acy_tensor
                test_writer.add_summary(test_result, epoch_index)   
                  
                ### summary and print
                acy_on_test = total_correct_times_in_test_set / float(total_batches_in_test_set * batch_size)
                acy_on_train = total_correct_times_in_train_set / float(total_batches_in_train_set * batch_size)
                print('Epoch - {:2d} , acy_on_test:{:6.2f}%({}/{}),loss_on_test:{:6.2f}, acy_on_train:{:6.2f}%({}/{}),loss_on_train:{:6.2f}'.
                      format(epoch_index, acy_on_test*100.0,total_correct_times_in_test_set,
                             total_batches_in_test_set * batch_size,total_cost_in_test_set, acy_on_train*100.0,
                             total_correct_times_in_train_set,total_batches_in_train_set * batch_size,total_cost_in_train_set))    
                
                
                
            #每次epoch训练结束后，保存参数    
            path_to_checkpoint = os.path.join(checkpoints_dir, 'model.ckpt')
            print "Epoch -",epoch_index,", learning rate:",learnrate
            try:
                os.mkdir(checkpoints_dir)
            except OSError:
                pass
            save_path = saver.save(sess, path_to_checkpoint) 
            print "save checkpoint to path: ", save_path


def train_for_one_batch_size(graph,
                 dataset,
                 time_step,
                 spectrum_hight,
                 spectrum_width,
                 batch_size=1,
                 num_epochs=1,
                 init_learning_rate = 0.001,
                 old_checkpoints_dir="checkpoint", #这个是以batch_size > 1时训练保存的.
                 new_checkpoints_dir="new_checkpoints_dir",#这个是以batch_size ＝1时训练保存的.
                 ):
    
    with tf.Session() as sess:
        
#         train_writer = tf.summary.FileWriter(tensorboard_dir+"/train", sess.graph) 
#         test_writer  = tf.summary.FileWriter(tensorboard_dir+"/test", sess.graph)
         
        saver = tf.train.Saver()
        
        sess.run(tf.global_variables_initializer())
        
        #从模型读读取session继续训练
        checkpoint_state = tf.train.get_checkpoint_state(old_checkpoints_dir)  
        if checkpoint_state and checkpoint_state.model_checkpoint_path:  
            saver.restore(sess, checkpoint_state.model_checkpoint_path)  
            print "从模型文件中读取session继续训练..."
         
         
        print "batch size:",batch_size
         
        #用于控制每epoch_delta轮在train set和test set上计算一下accuracy和cost
        epoch_delta = 2
        for epoch_index in range(num_epochs):
            
            if epoch_index % 5  == 0:
                    #计算学习率
                    learnrate = init_learning_rate * (0.99 ** epoch_index) 
                    sess.run(tf.assign(graph['learning_rate'], learnrate))
                    
            #################################
            #    获取TRAIN set，开始训练网络
            #################################
            for (batch_xs,batch_ys) in dataset.train.mini_batches(batch_size):
                batch_xs = batch_xs.reshape([batch_size, time_step, spectrum_width*spectrum_hight])
                   
                sess.run([graph['optimize']], feed_dict={
                    graph['x_placeholder']: batch_xs,
                    graph['y_placeholder']: batch_ys,
                    graph['keep_prob_placeholder']:0.5,
                })
      
            
            #每epoch_delta轮在train set和test set上计算一下accuracy和cost
            if epoch_index % epoch_delta  == 0:
                #################################
                #    开始在 train set上计算一下accuracy和cost
                #################################
                #记录训练集中有多少个batch
                total_batches_in_train_set = 0
                #记录在训练集中预测正确的次数
                total_correct_times_in_train_set = 0
                #记录在训练集中的总cost
                total_cost_in_train_set = 0.
                for (train_batch_xs,train_batch_ys) in dataset.train.mini_batches(batch_size):
                    train_batch_xs = train_batch_xs.reshape([batch_size, time_step, spectrum_width*spectrum_hight])
                    return_correct_times_in_batch = sess.run(graph['correct_times_in_batch'], feed_dict={
                        graph['x_placeholder']: train_batch_xs,
                        graph['y_placeholder']: train_batch_ys,
                        graph['keep_prob_placeholder']:1.0,
                    })
                    mean_cost_in_batch = sess.run(graph['cost'], feed_dict={
                        graph['x_placeholder']: train_batch_xs,
                        graph['y_placeholder']: train_batch_ys,
                        graph['keep_prob_placeholder']:1.0,
                    })
                      
                    total_batches_in_train_set += 1
                    total_correct_times_in_train_set += return_correct_times_in_batch
                    total_cost_in_train_set  += (mean_cost_in_batch*batch_size)
                      
                    #准确率
                    train_acy_tensor = total_correct_times_in_train_set / float(total_batches_in_train_set * batch_size) * 100.0
                    train_mean_cost = total_cost_in_train_set / float(total_batches_in_train_set * batch_size)
                  
                print "Epoch -",epoch_index,", train_mean_cost:",train_mean_cost,",train_acy_tensor:",train_acy_tensor
                
                #################################
                #    开始在 test set上计算一下accuracy和cost
                #################################
                #记录测试集中有多少个batch
                total_batches_in_test_set = 0
                #记录在测试集中预测正确的次数
                total_correct_times_in_test_set = 0
                #记录在测试集中的总cost
                total_cost_in_test_set = 0.
                for (test_batch_xs,test_batch_ys) in dataset.test.mini_batches(batch_size):
                    test_batch_xs = test_batch_xs.reshape([batch_size, time_step, spectrum_width*spectrum_hight])
                    return_correct_times_in_batch = sess.run(graph['correct_times_in_batch'], feed_dict={
                        graph['x_placeholder']: test_batch_xs,
                        graph['y_placeholder']: test_batch_ys,
                        graph['keep_prob_placeholder']:1.0,
                    })
                    mean_cost_in_batch = sess.run(graph['cost'], feed_dict={
                        graph['x_placeholder']: test_batch_xs,
                        graph['y_placeholder']: test_batch_ys,
                        graph['keep_prob_placeholder']:1.0,
                    })
                      
                    total_batches_in_test_set += 1
                    total_correct_times_in_test_set += return_correct_times_in_batch
                    total_cost_in_test_set  += (mean_cost_in_batch*batch_size)
                      
                    #准确率
                    test_acy_tensor = total_correct_times_in_test_set / float(total_batches_in_test_set * batch_size) * 100.0
                    test_mean_cost = total_cost_in_test_set / float(total_batches_in_test_set * batch_size)
                  
                print "Epoch -",epoch_index,", test_mean_cost:",test_mean_cost,",test_acy_tensor:",test_acy_tensor
                  
                ### summary and print
                acy_on_test = total_correct_times_in_test_set / float(total_batches_in_test_set * batch_size)
                acy_on_train = total_correct_times_in_train_set / float(total_batches_in_train_set * batch_size)
                print('Epoch - {:2d} , acy_on_test:{:6.2f}%({}/{}),loss_on_test:{:6.2f}, acy_on_train:{:6.2f}%({}/{}),loss_on_train:{:6.2f}'.
                      format(epoch_index, acy_on_test*100.0,total_correct_times_in_test_set,
                             total_batches_in_test_set * batch_size,total_cost_in_test_set, acy_on_train*100.0,
                             total_correct_times_in_train_set,total_batches_in_train_set * batch_size,total_cost_in_train_set))    
                
                
            #每次epoch训练结束后，保存参数    
            path_to_checkpoint = os.path.join(new_checkpoints_dir, 'model.ckpt')
            print "Epoch -",epoch_index,", learning rate:",learnrate
            try:
                os.mkdir(new_checkpoints_dir)
            except OSError:
                pass
            save_path = saver.save(sess, path_to_checkpoint) 
            print "save checkpoint to path: ", save_path


def add_cnn_layers_updated_v10(inputs,batch_norm=True):
    """
    入参：
        inputs：每张图片的大小是spectrum_hight,spectrum_width。
                    shape:(batch_size,time_step,spectrum_hight,spectrum_width)
    返回值：
        shape:(batch_size,time_step,feature_size)     
    """
    def conv2d(x, W, b, strides=1):
        """
        x的shape：[batch, in_height, in_width, in_channels]
        W：就是filter，它的shape：[filter_height, filter_width, in_channels, out_channels]
        """
        # Conv2D wrapper, with bias and relu activation
        x = tf.nn.conv2d(x, filter=W, strides=[1, strides, strides, 1], padding='VALID')
        x = tf.nn.bias_add(x, b)
        return tf.nn.relu(x)

    def maxpool2d(x, ksize,stride,):
        # MaxPool2D wrapper
        #tf.nn.max_pool
        return tf.nn.max_pool(x, ksize, stride, padding='VALID')

    shapeList = inputs.get_shape().as_list()
    print "cnn_input：[batch,time,H,W] =",shapeList
    
    time_step = shapeList[1]
    spectrum_hight = shapeList[2]
    spectrum_width = shapeList[3]
    
    weights = {
        # 9x9 conv, 1 input, 32 outputs
        'filter1': tf.Variable(tf.random_normal([3, 3, 1, 32])),
        # 5x5 conv, 32 inputs, 64 outputs
        'filter2': tf.Variable(tf.random_normal([3, 3, 32, 32])),
        'filter3': tf.Variable(tf.random_normal([3, 3, 32, 32])),
#         'filter4': tf.Variable(tf.random_normal([5, 5, 64, 64])),
#         'filter5': tf.Variable(tf.random_normal([3, 3, 32, 32])),
#         'filter6': tf.Variable(tf.random_normal([3, 3, 32, 32])),
#         'filter7': tf.Variable(tf.random_normal([3, 3, 32, 32])),
    }
    
    biases = {
        'filter1': tf.Variable(tf.random_normal([32])),
        'filter2': tf.Variable(tf.random_normal([32])),
        'filter3': tf.Variable(tf.random_normal([32])),
#         'filter4': tf.Variable(tf.random_normal([64])),
#         'filter5': tf.Variable(tf.random_normal([32])),
#         'filter6': tf.Variable(tf.random_normal([32])),
#         'filter7': tf.Variable(tf.random_normal([32])),
    }
    

    
    print "params: [batch*time,H,W,out_channels]"
    #开始定义CNN
    in_x = tf.reshape(inputs, shape=[-1, spectrum_hight, spectrum_width, 1])
    # Convolution Layer
    conv1 = conv2d(in_x, weights['filter1'], biases['filter1'])
    print 'conv1: ',conv1.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv1 = maxpool2d(conv1,ksize=[1,2,2,1],stride=[1,2,2,1])
    print 'pooling1: ',conv1.get_shape().as_list()
    if batch_norm:
        conv1 = batch_normalization(conv1,biases['filter1'].get_shape().as_list()[0])

    # Convolution Layer
    conv2 = conv2d(conv1, weights['filter2'], biases['filter2'])
    print 'conv2: ',conv2.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv2 = maxpool2d(conv2,ksize=[1,2,2,1],stride=[1,2,2,1])
    print 'pooling2: ',conv2.get_shape().as_list()
    if batch_norm:
        conv2 = batch_normalization(conv2,biases['filter2'].get_shape().as_list()[0])
     
    # Convolution Layer
    conv3 = conv2d(conv2, weights['filter3'], biases['filter3'])
    print 'conv3: ',conv3.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv3 = maxpool2d(conv3,ksize=[1,2,2,1],stride=[1,2,2,1])
    print 'pooling3: ',conv3.get_shape().as_list()
    if batch_norm:
        conv3 = batch_normalization(conv3,biases['filter3'].get_shape().as_list()[0])
    
    
    #conv3.get_shape()是(bath_size*time_step,feature_height,feature_width,out_channels]
    conv_shape_list = conv3.get_shape().as_list()
    feature_height = conv_shape_list[1]
    feature_width = conv_shape_list[2]
    out_channels = conv_shape_list[3]
    
#     #计算这张图片的feature总数
#     total_feature_size = feature_height * feature_width * out_channels
#     print "total_feature_size",total_feature_size
#     #返回值shape:(batch_size,time_step,feature_size)     

    result = tf.reshape(conv3, [-1, feature_width, feature_height*out_channels]) 
    return result


def add_cnn_layers_updated_v11(inputs,batch_norm=True):
    """
    入参：
        inputs：每张图片的大小是spectrum_hight,spectrum_width。
                    shape:(batch_size,time_step,spectrum_hight,spectrum_width)
    返回值：
        shape:(batch_size,time_step,feature_size)     
    """
    def conv2d(x, W, b, strides=1):
        """
        x的shape：[batch, in_height, in_width, in_channels]
        W：就是filter，它的shape：[filter_height, filter_width, in_channels, out_channels]
        """
        # Conv2D wrapper, with bias and relu activation
        x = tf.nn.conv2d(x, filter=W, strides=[1, strides, strides, 1], padding='VALID')
        x = tf.nn.bias_add(x, b)
        return tf.nn.relu(x)

    def maxpool2d(x, ksize,stride,):
        # MaxPool2D wrapper
        #tf.nn.max_pool
        return tf.nn.max_pool(x, ksize, stride, padding='VALID')

    shapeList = inputs.get_shape().as_list()
    print "cnn_input：[batch,time,H,W] =",shapeList
    
    time_step = shapeList[1]
    spectrum_hight = shapeList[2]
    spectrum_width = shapeList[3]
    
    weights = {
        # 9x9 conv, 1 input, 32 outputs
        'filter1': tf.Variable(tf.random_normal([5, 5, 1, 32])),
        # 5x5 conv, 32 inputs, 64 outputs
        'filter2': tf.Variable(tf.random_normal([5, 5, 32, 64])),
        'filter3': tf.Variable(tf.random_normal([5, 5, 64, 128])),
#         'filter4': tf.Variable(tf.random_normal([5, 5, 64, 64])),
#         'filter5': tf.Variable(tf.random_normal([3, 3, 32, 32])),
#         'filter6': tf.Variable(tf.random_normal([3, 3, 32, 32])),
#         'filter7': tf.Variable(tf.random_normal([3, 3, 32, 32])),
    }
    
    biases = {
        'filter1': tf.Variable(tf.random_normal([32])),
        'filter2': tf.Variable(tf.random_normal([64])),
        'filter3': tf.Variable(tf.random_normal([128])),
#         'filter4': tf.Variable(tf.random_normal([64])),
#         'filter5': tf.Variable(tf.random_normal([32])),
#         'filter6': tf.Variable(tf.random_normal([32])),
#         'filter7': tf.Variable(tf.random_normal([32])),
    }
    

    
    print "params: [batch*time,H,W,out_channels]"
    #开始定义CNN
    in_x = tf.reshape(inputs, shape=[-1, spectrum_hight, spectrum_width, 1])
    # Convolution Layer
    conv1 = conv2d(in_x, weights['filter1'], biases['filter1'])
    print 'conv1: ',conv1.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv1 = maxpool2d(conv1,ksize=[1,4,2,1],stride=[1,4,2,1])
    print 'pooling1: ',conv1.get_shape().as_list()
    if batch_norm:
        conv1 = batch_normalization(conv1,biases['filter1'].get_shape().as_list()[0])

    # Convolution Layer
    conv2 = conv2d(conv1, weights['filter2'], biases['filter2'])
    print 'conv2: ',conv2.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv2 = maxpool2d(conv2,ksize=[1,2,2,1],stride=[1,2,2,1])
    print 'pooling2: ',conv2.get_shape().as_list()
    if batch_norm:
        conv2 = batch_normalization(conv2,biases['filter2'].get_shape().as_list()[0])
     
    # Convolution Layer
    conv3 = conv2d(conv2, weights['filter3'], biases['filter3'])
    print 'conv3: ',conv3.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv3 = maxpool2d(conv3,ksize=[1,2,1,1],stride=[1,2,1,1])
    print 'pooling3: ',conv3.get_shape().as_list()
    if batch_norm:
        conv3 = batch_normalization(conv3,biases['filter3'].get_shape().as_list()[0])
    
    
    #conv3.get_shape()是(bath_size*time_step,feature_height,feature_width,out_channels]
    conv_shape_list = conv3.get_shape().as_list()
    feature_height = conv_shape_list[1]
    feature_width = conv_shape_list[2]
    out_channels = conv_shape_list[3]
    
#     #计算这张图片的feature总数
#     total_feature_size = feature_height * feature_width * out_channels
#     print "total_feature_size",total_feature_size
#     #返回值shape:(batch_size,time_step,feature_size)     

    result = tf.reshape(conv3, [-1, feature_width, feature_height*out_channels]) 
    return result


#时间轴不做池化
def add_cnn_layers_updated_v12(inputs,batch_norm=True):
    """
    入参：
        inputs：每张图片的大小是spectrum_hight,spectrum_width。
                    shape:(batch_size,time_step,spectrum_hight,spectrum_width)
    返回值：
        shape:(batch_size,time_step,feature_size)     
    """
    def conv2d(x, W, b, strides=1):
        """
        x的shape：[batch, in_height, in_width, in_channels]
        W：就是filter，它的shape：[filter_height, filter_width, in_channels, out_channels]
        """
        # Conv2D wrapper, with bias and relu activation
        x = tf.nn.conv2d(x, filter=W, strides=[1, strides, strides, 1], padding='VALID')
        x = tf.nn.bias_add(x, b)
        return tf.nn.relu(x)

    def maxpool2d(x, ksize,stride,):
        # MaxPool2D wrapper
        #tf.nn.max_pool
        return tf.nn.max_pool(x, ksize, stride, padding='VALID')

    shapeList = inputs.get_shape().as_list()
    print "cnn_input：[batch,time,H,W] =",shapeList
    
    time_step = shapeList[1]
    spectrum_hight = shapeList[2]
    spectrum_width = shapeList[3]
    
    weights = {
        # 9x9 conv, 1 input, 32 outputs
        'filter1': tf.Variable(tf.random_normal([5, 5, 1, 32])),
        # 5x5 conv, 32 inputs, 64 outputs
        'filter2': tf.Variable(tf.random_normal([5, 5, 32, 64])),
        'filter3': tf.Variable(tf.random_normal([5, 5, 64, 64])),
        'filter4': tf.Variable(tf.random_normal([5, 5, 64, 64])),
#         'filter5': tf.Variable(tf.random_normal([3, 3, 32, 32])),
#         'filter6': tf.Variable(tf.random_normal([3, 3, 32, 32])),
#         'filter7': tf.Variable(tf.random_normal([3, 3, 32, 32])),
    }
    
    biases = {
        'filter1': tf.Variable(tf.random_normal([32])),
        'filter2': tf.Variable(tf.random_normal([64])),
        'filter3': tf.Variable(tf.random_normal([64])),
        'filter4': tf.Variable(tf.random_normal([64])),
#         'filter5': tf.Variable(tf.random_normal([32])),
#         'filter6': tf.Variable(tf.random_normal([32])),
#         'filter7': tf.Variable(tf.random_normal([32])),
    }
    

    
    print "params: [batch*time,H,W,out_channels]"
    #开始定义CNN
    in_x = tf.reshape(inputs, shape=[-1, spectrum_hight, spectrum_width, 1])
    # Convolution Layer
    conv1 = conv2d(in_x, weights['filter1'], biases['filter1'])
    print 'conv1: ',conv1.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv1 = maxpool2d(conv1,ksize=[1,2,1,1],stride=[1,2,1,1])
    print 'pooling1: ',conv1.get_shape().as_list()
    if batch_norm:
        conv1 = batch_normalization(conv1,biases['filter1'].get_shape().as_list()[0])

    # Convolution Layer
    conv2 = conv2d(conv1, weights['filter2'], biases['filter2'])
    print 'conv2: ',conv2.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv2 = maxpool2d(conv2,ksize=[1,2,1,1],stride=[1,2,1,1])
    print 'pooling2: ',conv2.get_shape().as_list()
    if batch_norm:
        conv2 = batch_normalization(conv2,biases['filter2'].get_shape().as_list()[0])
     
    # Convolution Layer
    conv3 = conv2d(conv2, weights['filter3'], biases['filter3'])
    print 'conv3: ',conv3.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv3 = maxpool2d(conv3,ksize=[1,2,1,1],stride=[1,2,1,1])
    print 'pooling3: ',conv3.get_shape().as_list()
    if batch_norm:
        conv3 = batch_normalization(conv3,biases['filter3'].get_shape().as_list()[0])
    
        # Convolution Layer
    conv4 = conv2d(conv3, weights['filter4'], biases['filter4'])
    print 'conv4: ',conv4.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv4 = maxpool2d(conv4,ksize=[1,2,1,1],stride=[1,2,1,1])
    print 'pooling4: ',conv4.get_shape().as_list()
    if batch_norm:
        conv4 = batch_normalization(conv4,biases['filter4'].get_shape().as_list()[0])
    
    #conv3.get_shape()是(bath_size*time_step,feature_height,feature_width,out_channels]
    conv_shape_list = conv4.get_shape().as_list()
    feature_height = conv_shape_list[1]
    feature_width = conv_shape_list[2]
    out_channels = conv_shape_list[3]
    
#     #计算这张图片的feature总数
#     total_feature_size = feature_height * feature_width * out_channels
#     print "total_feature_size",total_feature_size
#     #返回值shape:(batch_size,time_step,feature_size)     

    result = tf.reshape(conv4, [-1, feature_width, feature_height*out_channels]) 
    return result


#add_cnn_layers_updated_v12一样，只是在时间上也做池化
def add_cnn_layers_updated_v13(inputs,batch_norm=True):
    """
    入参：
        inputs：每张图片的大小是spectrum_hight,spectrum_width。
                    shape:(batch_size,time_step,spectrum_hight,spectrum_width)
    返回值：
        shape:(batch_size,time_step,feature_size)     
    """
    def conv2d(x, W, b, strides=1):
        """
        x的shape：[batch, in_height, in_width, in_channels]
        W：就是filter，它的shape：[filter_height, filter_width, in_channels, out_channels]
        """
        # Conv2D wrapper, with bias and relu activation
        x = tf.nn.conv2d(x, filter=W, strides=[1, strides, strides, 1], padding='VALID')
        x = tf.nn.bias_add(x, b)
        return tf.nn.relu(x)

    def maxpool2d(x, ksize,stride,):
        # MaxPool2D wrapper
        #tf.nn.max_pool
        return tf.nn.max_pool(x, ksize, stride, padding='VALID')

    shapeList = inputs.get_shape().as_list()
    print "cnn_input：[batch,time,H,W] =",shapeList
    
    time_step = shapeList[1]
    spectrum_hight = shapeList[2]
    spectrum_width = shapeList[3]
    
    weights = {
        # 9x9 conv, 1 input, 32 outputs
        'filter1': tf.Variable(tf.random_normal([5, 5, 1, 32])),
        # 5x5 conv, 32 inputs, 64 outputs
        'filter2': tf.Variable(tf.random_normal([5, 5, 32, 64])),
        'filter3': tf.Variable(tf.random_normal([5, 5, 64, 64])),
        'filter4': tf.Variable(tf.random_normal([5, 5, 64, 64])),
#         'filter5': tf.Variable(tf.random_normal([3, 3, 32, 32])),
#         'filter6': tf.Variable(tf.random_normal([3, 3, 32, 32])),
#         'filter7': tf.Variable(tf.random_normal([3, 3, 32, 32])),
    }
    
    biases = {
        'filter1': tf.Variable(tf.random_normal([32])),
        'filter2': tf.Variable(tf.random_normal([64])),
        'filter3': tf.Variable(tf.random_normal([64])),
        'filter4': tf.Variable(tf.random_normal([64])),
#         'filter5': tf.Variable(tf.random_normal([32])),
#         'filter6': tf.Variable(tf.random_normal([32])),
#         'filter7': tf.Variable(tf.random_normal([32])),
    }
    

    
    print "params: [batch*time,H,W,out_channels]"
    #开始定义CNN
    in_x = tf.reshape(inputs, shape=[-1, spectrum_hight, spectrum_width, 1])
    # Convolution Layer
    conv1 = conv2d(in_x, weights['filter1'], biases['filter1'])
    print 'conv1: ',conv1.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv1 = maxpool2d(conv1,ksize=[1,2,2,1],stride=[1,2,2,1])
    print 'pooling1: ',conv1.get_shape().as_list()
    if batch_norm:
        conv1 = batch_normalization(conv1,biases['filter1'].get_shape().as_list()[0])

    # Convolution Layer
    conv2 = conv2d(conv1, weights['filter2'], biases['filter2'])
    print 'conv2: ',conv2.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv2 = maxpool2d(conv2,ksize=[1,2,2,1],stride=[1,2,2,1])
    print 'pooling2: ',conv2.get_shape().as_list()
    if batch_norm:
        conv2 = batch_normalization(conv2,biases['filter2'].get_shape().as_list()[0])
     
    # Convolution Layer
    conv3 = conv2d(conv2, weights['filter3'], biases['filter3'])
    print 'conv3: ',conv3.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv3 = maxpool2d(conv3,ksize=[1,2,2,1],stride=[1,2,2,1])
    print 'pooling3: ',conv3.get_shape().as_list()
    if batch_norm:
        conv3 = batch_normalization(conv3,biases['filter3'].get_shape().as_list()[0])
    
        # Convolution Layer
    conv4 = conv2d(conv3, weights['filter4'], biases['filter4'])
    print 'conv4: ',conv4.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv4 = maxpool2d(conv4,ksize=[1,2,2,1],stride=[1,2,2,1])
    print 'pooling4: ',conv4.get_shape().as_list()
    if batch_norm:
        conv4 = batch_normalization(conv4,biases['filter4'].get_shape().as_list()[0])
    
    #conv3.get_shape()是(bath_size*time_step,feature_height,feature_width,out_channels]
    conv_shape_list = conv4.get_shape().as_list()
    feature_height = conv_shape_list[1]
    feature_width = conv_shape_list[2]
    out_channels = conv_shape_list[3]
    
#     #计算这张图片的feature总数
#     total_feature_size = feature_height * feature_width * out_channels
#     print "total_feature_size",total_feature_size
#     #返回值shape:(batch_size,time_step,feature_size)     

    result = tf.reshape(conv4, [-1, feature_width, feature_height*out_channels]) 
    return result

#add_cnn_layers_updated_v12一样，只是在时间上也做池化

def add_cnn_layers_updated_v14(inputs,batch_norm=True):
    """
    入参：
        inputs：每张图片的大小是spectrum_hight,spectrum_width。
                    shape:(batch_size,time_step,spectrum_hight,spectrum_width)
    返回值：
        shape:(batch_size,time_step,feature_size)     
    """
    def conv2d(x, W, b, strides=1):
        """
        x的shape：[batch, in_height, in_width, in_channels]
        W：就是filter，它的shape：[filter_height, filter_width, in_channels, out_channels]
        """
        # Conv2D wrapper, with bias and relu activation
        x = tf.nn.conv2d(x, filter=W, strides=[1, strides, strides, 1], padding='VALID')
        x = tf.nn.bias_add(x, b)
        return tf.nn.relu(x)

    def maxpool2d(x, ksize,stride,):
        # MaxPool2D wrapper
        #tf.nn.max_pool
        return tf.nn.max_pool(x, ksize, stride, padding='VALID')

    shapeList = inputs.get_shape().as_list()
    print "cnn_input：[batch,time,H,W] =",shapeList
    
    time_step = shapeList[1]
    spectrum_hight = shapeList[2]
    spectrum_width = shapeList[3]
    
    weights = {
        # 9x9 conv, 1 input, 32 outputs
        'filter1': tf.Variable(tf.random_normal([5, 5, 1, 32])),
        # 5x5 conv, 32 inputs, 64 outputs
        'filter2': tf.Variable(tf.random_normal([5, 5, 32, 64])),
#         'filter3': tf.Variable(tf.random_normal([5, 5, 64, 64])),
#         'filter4': tf.Variable(tf.random_normal([5, 5, 64, 64])),
#         'filter5': tf.Variable(tf.random_normal([3, 3, 32, 32])),
#         'filter6': tf.Variable(tf.random_normal([3, 3, 32, 32])),
#         'filter7': tf.Variable(tf.random_normal([3, 3, 32, 32])),
    }
    
    biases = {
        'filter1': tf.Variable(tf.random_normal([32])),
        'filter2': tf.Variable(tf.random_normal([64])),
#         'filter3': tf.Variable(tf.random_normal([64])),
#         'filter4': tf.Variable(tf.random_normal([64])),
#         'filter5': tf.Variable(tf.random_normal([32])),
#         'filter6': tf.Variable(tf.random_normal([32])),
#         'filter7': tf.Variable(tf.random_normal([32])),
    }
    

    
    print "params: [batch*time,H,W,out_channels]"
    #开始定义CNN
    in_x = tf.reshape(inputs, shape=[-1, spectrum_hight, spectrum_width, 1])
    # Convolution Layer
    conv1 = conv2d(in_x, weights['filter1'], biases['filter1'])
    print 'conv1: ',conv1.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv1 = maxpool2d(conv1,ksize=[1,4,4,1],stride=[1,4,4,1])
    print 'pooling1: ',conv1.get_shape().as_list()
    if batch_norm:
        conv1 = batch_normalization(conv1,biases['filter1'].get_shape().as_list()[0])

    # Convolution Layer
    conv2 = conv2d(conv1, weights['filter2'], biases['filter2'])
    print 'conv2: ',conv2.get_shape().as_list()
    # Max Pooling (down-sampling)
    conv2 = maxpool2d(conv2,ksize=[1,4,4,1],stride=[1,4,4,1])
    print 'pooling2: ',conv2.get_shape().as_list()
    if batch_norm:
        conv2 = batch_normalization(conv2,biases['filter2'].get_shape().as_list()[0])
     
#     # Convolution Layer
#     conv3 = conv2d(conv2, weights['filter3'], biases['filter3'])
#     print 'conv3: ',conv3.get_shape().as_list()
#     # Max Pooling (down-sampling)
#     conv3 = maxpool2d(conv3,ksize=[1,2,2,1],stride=[1,2,2,1])
#     print 'pooling3: ',conv3.get_shape().as_list()
#     if batch_norm:
#         conv3 = batch_normalization(conv3,biases['filter3'].get_shape().as_list()[0])
#     
#         # Convolution Layer
#     conv4 = conv2d(conv3, weights['filter4'], biases['filter4'])
#     print 'conv4: ',conv4.get_shape().as_list()
#     # Max Pooling (down-sampling)
#     conv4 = maxpool2d(conv4,ksize=[1,2,2,1],stride=[1,2,2,1])
#     print 'pooling4: ',conv4.get_shape().as_list()
#     if batch_norm:
#         conv4 = batch_normalization(conv4,biases['filter4'].get_shape().as_list()[0])
    
    #conv3.get_shape()是(bath_size*time_step,feature_height,feature_width,out_channels]
    conv_shape_list = conv2.get_shape().as_list()
    feature_height = conv_shape_list[1]
    feature_width = conv_shape_list[2]
    out_channels = conv_shape_list[3]
    
#     #计算这张图片的feature总数
#     total_feature_size = feature_height * feature_width * out_channels
#     print "total_feature_size",total_feature_size
#     #返回值shape:(batch_size,time_step,feature_size)     

    result = tf.reshape(conv2, [-1, feature_width, feature_height*out_channels]) 
    return result


def batch_normalization(Wx_plus_b,out_size):
    # Batch Normalize
    fc_mean, fc_var = tf.nn.moments(
        Wx_plus_b,
        axes= list(range(len(Wx_plus_b.get_shape()) - 1)),  #[0,1,2],   # the dimension you wanna normalize, here [0] for batch
                    # for image, you wanna do [0, 1, 2] for [batch, height, width] but not channel
    )
    scale = tf.Variable(tf.ones([out_size]))
    shift = tf.Variable(tf.zeros([out_size]))
    epsilon = 0.001
    Wx_plus_b = tf.nn.batch_normalization(Wx_plus_b, fc_mean, fc_var, shift, scale, epsilon)
    return Wx_plus_b


def test_add_cnn_layers(cnn):
    """
    size: 655360
    add_cnn_layers() inputs shape: [10, 2, 256, 128]
    conv_shape_list: [20, 28, 12, 64]
    feature_height 28
    feature_width 12
    out_channels 64
    total_feature_size 21504
    add_cnn_layers返回结果的shape为： [10, 2, 21504]

    """
    
    time_step = 1
    spectrum_width = 100
    spectrum_height = 128
    batch_size = 1
    
    size = batch_size*time_step*spectrum_height*spectrum_width
    
    print "size:",size
    import numpy as np
    
    raw = np.arange(size)
    x = np.reshape(raw,(batch_size,time_step,spectrum_height,spectrum_width))
    
    tensor =  tf.constant(x,tf.float32)
    result = cnn(tensor)
    print "add_cnn_layers返回结果的shape为：",result.get_shape().as_list()


def convert_to_constant_graph(checkpoints_dir, constant_graph_dir, constant_graph_file):
    with tf.Graph().as_default():
        with tf.Session() as session:
            checkpoint = tf.train.get_checkpoint_state(checkpoints_dir)
            if checkpoint and checkpoint.model_checkpoint_path:
                saver = tf.train.import_meta_graph(checkpoint.model_checkpoint_path + '.meta')
            else:
                raise ValueError('No checkpoint file found')
            saver.restore(session, checkpoint.model_checkpoint_path)
            constant_graph = graph_util.convert_variables_to_constants(session, session.graph_def, ["out_softmax"])
            
            with tf.gfile.FastGFile(constant_graph_dir+constant_graph_file,mode='wb') as f:
                f.write(constant_graph.SerializeToString())
    
#             tf.train.write_graph(constant_graph, constant_graph_dir, constant_graph_file, as_text=False)
#             path =  os.path.join(constant_graph_dir,constant_graph_file)
#             print "save graph to path:",path


def resize_bath_size():
    time_step = 1
    spectrum_width = 100
    spectrum_hight = 128
    num_classes = 5
    rnn_out_size = 512
    rnn_layers = 1
    batch_size = 1
    num_epochs = 1
    init_learning_rate = 0.0001
    
#     png_path = "/media/tsiangleo/0FD7048F0FD7048F/DeepLearning/data/china-celebrity-speech/merged_data/final_yanshi/10/png"
    png_path = "/media/tsiangleo/0FD7048F0FD7048F/DeepLearning/data/china-celebrity-speech/0406/png/all_in_one"
   
    
    #配置输出路径
    old_checkpoints_dir = "output/cnnv14_1rnn_512/ckpt"
    new_checkpoints_dir = "output/cnnv14_1rnn_512/batchsized_ckpt"
    graph_dir = "output/cnnv14_1rnn_512/"
    graph_file_name ="one_batchsize_sid.pb"
      
    g = build_net_cnn_rnn(time_step, spectrum_hight, spectrum_width, num_classes, rnn_out_size,rnn_layers, batch_size)
     
    dataset = input_data.read_data_sets(os.path.abspath(png_path),
                                        num_of_speaker=num_classes,spectr_cols=spectrum_width*time_step, 
                                        spectr_rows = spectrum_hight,hop_size=None)     
      
    train_for_one_batch_size(g, dataset, time_step, spectrum_hight, spectrum_width, batch_size, num_epochs, init_learning_rate, old_checkpoints_dir, new_checkpoints_dir)
    convert_to_constant_graph(new_checkpoints_dir, graph_dir, graph_file_name)
    

def main():
    """
    cnnV10+RNN512_2layer
    cnnV11+RNN512_2layer
    cnnv12+rnn256_2layer
    
       cnnv12 
    size: 12800
cnn_input：[batch,time,H,W] = [1, 1, 128, 100]
params: [batch*time,H,W,out_channels]
conv1:  [1, 124, 96, 32]
pooling1:  [1, 62, 96, 32]
conv2:  [1, 58, 92, 64]
pooling2:  [1, 29, 92, 64]
conv3:  [1, 25, 88, 64]
pooling3:  [1, 12, 88, 64]
conv4:  [1, 8, 84, 64]
pooling3:  [1, 4, 84, 64]
add_cnn_layers返回结果的shape为： [1, 84, 256]
    
    Input
        |
    CNN-128
        |
    RNN-128
        |
    Output-10
    """
    time_step = 1
    spectrum_width = 100
    spectrum_hight = 128
    num_classes = 5
    rnn_out_size = 512
    rnn_layers = 1
    batch_size = 10 #16
    num_epochs = 100
    init_learning_rate = 0.0001
    
#     png_path = "/media/tsiangleo/0FD7048F0FD7048F/DeepLearning/data/china-celebrity-speech/merged_data/final_yanshi/10/png"
    png_path = "/media/tsiangleo/0FD7048F0FD7048F/DeepLearning/data/china-celebrity-speech/0406/png/java_all_in_one"
    

    #获取当前源代码文件的文件名
    filepath=str(__file__)
    filename=filepath.split('/')[-1].split('.')[0]
    
    #配置输出路径
    checkpoints_dir = "output/cnnv12_1rnn_512/ckpt"
    graph_dir = "output/cnnv12_1rnn_512/const_graph"
    graph_file_name ="graph.pb"
    tensorboard_dir = "output/cnnv12_1rnn_512/board_"+str(num_classes)
     
    g = build_net_cnn_rnn(time_step, spectrum_hight, spectrum_width, num_classes, rnn_out_size, rnn_layers, batch_size)
    
    dataset = input_data.read_data_sets(os.path.abspath(png_path),
                                        num_of_speaker=num_classes,spectr_cols=spectrum_width*time_step, 
                                        spectr_rows = spectrum_hight,hop_size=None)     
    train_net_v2_tensorboard(g, dataset, time_step, spectrum_hight, spectrum_width, batch_size, num_epochs,
                              init_learning_rate, checkpoints_dir, tensorboard_dir)
    convert_to_constant_graph(checkpoints_dir, graph_dir, graph_file_name)

main()
# resize_bath_size()
# test_add_cnn_layers(add_cnn_layers_updated_v14)