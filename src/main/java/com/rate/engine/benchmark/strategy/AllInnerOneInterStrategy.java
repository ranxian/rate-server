package com.rate.engine.benchmark.strategy;

/**
 * Created by Ran Xian on 5/18/14.
 */
// This strategy generates all inner matches in a view. Inter matches consist of every one sample in one class
// to match with one class in remaining classes.
// Bad Doc....
public class AllInnerOneInterStrategy extends BasicStrategy {
    @Override
    protected void prepareSelectedMap() throws Exception {
        this.selectedMap = view.getClazzsSamples();
    }

    @Override
    public void apply() throws Exception {
        setNextProgress(0.3);
        prepare();

        setNextProgress(0.5);
        generateInnerClazz();

        setNextProgress(1.0);
        generateInterClazzOne(-1);

        finish();
    }

    @Override
    public String getStrategyName() {
        return "AllInnerOneInterStrategy";
    }
}
