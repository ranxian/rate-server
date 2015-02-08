package com.rate.test;

import com.rate.engine.benchmark.Benchmark;

/**
 * Created by Ran Xian on 3/14/14.
 */
public class TestFind {
    public static void main(String[] args) throws Exception {
        Benchmark benchmark = Benchmark.find("43bdd535-94d0-49d5-9345-17b5f1be4c70");
        System.out.println(benchmark.toString());
    }
}
