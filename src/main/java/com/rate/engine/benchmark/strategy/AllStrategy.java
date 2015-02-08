package com.rate.engine.benchmark.strategy;

/**
 * Created by Ran Xian on 4/26/14.
 * Generate a benchmark matching all samples with all samples in the benchmark's view
 */
// Generate all inner and inter matches in a view.
public class AllStrategy extends BasicStrategy {
    @Override public void apply() throws Exception {
        setNextProgress(0.5);
        prepare();

        setNextProgress(0.6);
        generateInnerClazz();
        setNextProgress(1.0);
        generateInterClazzAll();

        finish();
    }

    @Override protected void prepareSelectedMap() throws Exception {
        this.selectedMap = view.getClazzsSamples();
    }

    @Override public String getStrategyName() {
        return "AllStrategy";
    }
}
