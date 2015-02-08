package com.rate.test;

import com.rate.engine.benchmark.strategy.GeneralStrategy;
import com.rate.engine.benchmark.Benchmark;
import com.rate.engine.view.View;

/**
 * Created by Ran Xian on 3/13/14.
 */
public class TestGeneralGenerator {
    public static void main(String[] args) throws Exception {
        // 869f3520-2a64-49f3-87a5-6967599b4b82

        View view = View.find("a92a8762-e398-4389-b102-5ce4cf1d3dfd");
        GeneralStrategy generator = new GeneralStrategy();
        generator.setViewUuid(view.getUuid());
        generator.setClassCount(10);
        generator.setSampleCount(3);
        Benchmark benchmark = Benchmark.generate(generator);
        benchmark.destroy();
    }
}
