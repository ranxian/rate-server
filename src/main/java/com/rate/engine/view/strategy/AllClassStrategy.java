package com.rate.engine.view.strategy;

import com.rate.engine.sample.Sample;

import java.util.List;

/**
 * Created by Ran Xian on 3/13/14.
 */
public class AllClassStrategy extends BasicStrategy {
    @Override
    public void prepare() throws Exception {

    }
    @Override
    public List<Sample> getNextSamples() {
        return null;
    }

    @Override
    public String getStrategyName() {
        return "AllClassStrategy";
    }
}
