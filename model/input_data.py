#!/usr/bin/env python
#coding: utf-8

import numpy as np
import img_label_loader
import collections



# Dataset = collections.namedtuple('Dataset', ['data', 'target'])
Datasets = collections.namedtuple('Datasets', ['train',  'test'])



class DataSet(object):
 
  def __init__(self,
               images,
               labels):

    self._num_examples = images.shape[0]
    self._images = images
    self._labels = labels
    self._epochs_completed = 0
    self._index_in_epoch = 0
 
  @property
  def images(self):
    return self._images
 
  @property
  def labels(self):
    return self._labels
 
  @property
  def num_examples(self):
    return self._num_examples
 
  @property
  def epochs_completed(self):
    return self._epochs_completed
  

  def mini_batches(self,mini_batch_size):
    """
      return: list of tuple(x,y)
    """
    # Shuffle the data
    perm = np.arange(self._num_examples)
    np.random.shuffle(perm)
    self._images = self._images[perm]
    self._labels = self._labels[perm]
    
    n = self.images.shape[0]
    
    mini_batches = [(self._images[k:k+mini_batch_size],self._labels[k:k+mini_batch_size])
                    for k in xrange(0, n, mini_batch_size)]
    
    if len(mini_batches[-1]) != mini_batch_size:
        return mini_batches[:-1]
    else:
        return mini_batches


  def _next_batch(self, batch_size, fake_data=False):
    """Return the next `batch_size` examples from this data set."""
    
    start = self._index_in_epoch
    self._index_in_epoch += batch_size
    if self._index_in_epoch > self._num_examples:
        # Finished epoch
        self._epochs_completed += 1
        # Shuffle the data
        perm = np.arange(self._num_examples)
        np.random.shuffle(perm)
        self._images = self._images[perm]
        self._labels = self._labels[perm]
        # Start next epoch
        start = 0
        self._index_in_epoch = batch_size
        assert batch_size <= self._num_examples
    end = self._index_in_epoch
    return self._images[start:end], self._labels[start:end]


def read_data_sets(img_path="/media/tsiangleo/0FD7048F0FD7048F/DeepLearning/data/TIMIT/png_data/big_png_update_20170107",
                   num_of_speaker=4, 
                   spectr_cols=100, 
                   spectr_rows = 128,
                   num_of_folds = 1,
                   test_size = 0.2,#测试集占的比重
                   hop_size=None,#生成语谱图时两张语谱图之间的帧移，若为None，则默认等于spectr_cols，即不重叠。
                   ):
    
    """
    img_train或imgs_test.shape（batch_size,height,width）
    """
    imgs_train, imgs_test, labels_train, labels_test  =  img_label_loader.get_cross_validation_data(img_path, spectr_rows, spectr_cols, num_of_speaker, num_of_folds, test_size, hop_size)
    train = DataSet(imgs_train, labels_train)
    test = DataSet(imgs_test, labels_test)
    return Datasets(train=train, test=test)


def _test(path):
    dataset = read_data_sets(img_path=path,hop_size=10)
    
    print "dataset.train.images.shape:",dataset.train.images.shape
    print "dataset.train.labels.shape:",dataset.train.labels.shape
    
    print dataset.test.images[0]
    print dataset.test.labels[0]
    
#     for (batch_xs,batch_ys) in dataset.train.mini_batches(2):
#         print "batch_xs.shape:",batch_xs.shape
#     #     print "batch_xs:",batch_xs
#         print "batch_ys.shape:",batch_ys.shape
# #         print "batch_ys:",batch_ys


# _test("/media/tsiangleo/0FD7048F0FD7048F/DeepLearning/data/TIMIT/png_data/big_png_update_20170107")