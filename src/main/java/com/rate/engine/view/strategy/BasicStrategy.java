package com.rate.engine.view.strategy;

import com.rate.engine.sample.Sample;
import com.rate.utils.RateConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Created by Ran Xian on 3/13/14.
 */
// A view strategy mainly tells which samples should consist a view. Besides, it also tells
// Default name of view, description of view and strategy name of view. BasicStrategy is a
// basic class for all strategies, every class extended from BasicStrategy should implement
// prepare() and getNextSamples() and getGenerator().
abstract public class BasicStrategy {
    // skip and limit is used to control increasingly fetch samples from database,
    // See @getNextSamples()
    protected long skip = 0;
    protected final int limit = RateConfig.getFetchLimit();
    // Total number of samples by this strategy
    @Getter protected long total = 0;

    public boolean isNoSamples() {
        return skip >= total;
    }
    // Name of the strategy
    abstract public String getStrategyName();
    // Preparation
    abstract public void prepare() throws Exception;
    // 不可能把数据库完全装进内存，所以要一点一点的取，有可能造成不同步！不要同时建 View 和修改 Sample
    // 所以有了 getNextSamples() 这个方法
    abstract public List<Sample> getNextSamples() throws Exception;
}
