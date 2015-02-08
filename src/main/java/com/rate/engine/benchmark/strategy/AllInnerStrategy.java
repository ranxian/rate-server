package com.rate.engine.benchmark.strategy;

/**
 * Created by Ran Xian on 4/26/14.
 */
// This strategy generates all inner matches in a view
public class AllInnerStrategy extends BasicStrategy {
    @Override
    public void apply() throws Exception {
        setNextProgress(0.3);
        prepare();

        setNextProgress(1.0);
        generateInnerClazz();

        finish();
    }

    @Override
    protected void prepareSelectedMap() throws Exception {
        this.selectedMap = view.getClazzsSamples();
    }

    @Override
    public String getStrategyName() {
        return "AllInnerStrategy";
    }
}
