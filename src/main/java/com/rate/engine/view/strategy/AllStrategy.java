package com.rate.engine.view.strategy;

import com.rate.engine.sample.Sample;
import com.rate.utils.DBUtils;

import java.util.List;

/**
 * Created by Ran Xian on 3/13/14.
 */
public class AllStrategy extends BasicStrategy {
    @Override
    public void prepare() throws Exception {
        this.total = DBUtils.count("SELECT COUNT(*) FROM sample WHERE classified='VALID'");
    }

    @Override
    public String getStrategyName() {
        return "AllStrategy";
    }

    @Override
    public List<Sample> getNextSamples() {
        List<Sample> samples = DBUtils.executeSQL(Sample.listHandler, "SELECT uuid,class_uuid FROM sample WHERE classified='VALID' LIMIT ?,?", skip, limit);
        skip += limit;
        return samples;
    }
}
