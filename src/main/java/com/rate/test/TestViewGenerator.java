package com.rate.test;

import com.rate.engine.view.strategy.ImportTagStrategy;
import com.rate.engine.view.View;

/**
 * Created by Ran Xian on 3/13/14.
 */
public class TestViewGenerator {
    public static void main(String[] args) throws Exception {
        ImportTagStrategy strategy = new ImportTagStrategy("2011summer");
        strategy.setImportTag("2011summer");
        View view = View.generate(strategy, null);
        System.out.println(view.toString());
        view.destroy();
    }
}
