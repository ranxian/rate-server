package com.rate.engine.database;

import com.rate.engine.clazz.Clazz;
import com.rate.engine.sample.Sample;
import com.rate.utils.RateConfig;

/**
 * Created by xianran on 5/6/15.
 */
public class Importer {
    // 给予一个压缩包的地址，按照其中 class 和 sample 的文件结构，将样本文件拷贝到 RATE_ROOT 对应的地方
    // 并且在数据库中加入新的记录
    public void addSamples(String zipFilePath, String importTag) {
        // 解压缩文件到特定位置
        // 创建一个 class
        // 加入新的 sample
    }
}
