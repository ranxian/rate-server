package com.rate.engine.view.strategy;

import com.rate.engine.sample.Sample;
import java.sql.Timestamp;
import java.util.List;

/**
 * Created by Ran Xian on 3/13/14.
 */
public class TimePeriodStrategy extends BasicStrategy {
    private Timestamp startTimeStamp;
    private Timestamp endTimeStamp;

    @Override
    public String getStrategyName() {
        return null;
    }

    public void setStartTimeStamp(Timestamp startTimeStamp) {
        this.startTimeStamp = startTimeStamp;
    }

    public void setEndTimeStamp(Timestamp endTimeStamp) {
        this.endTimeStamp = endTimeStamp;
    }

    @Override
    public List<Sample> getNextSamples() {
        return null;
    }
    @Override
    public void prepare() throws Exception {}
}
