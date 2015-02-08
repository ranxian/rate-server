package com.rate.engine.benchmark.strategy;

import com.rate.engine.clazz.Clazz;
import com.rate.engine.exception.InvalidArgumentException;
import com.rate.engine.sample.Sample;
import com.rate.engine.exception.BenchmarkGeneratorException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import java.util.List;

/**
 * Created by Ran Xian on 12/3/14.
 */
// This strategy is very similar to GeneralStrategy except that it generates all inter matches instead the
// same number as inner matches.
public class AllNStrategy extends BasicStrategy {
    @Setter @Getter private int sampleCount;
    @Setter @Getter private int classCount;

    @Override public void apply() throws Exception {
        if (sampleCount==0 || classCount==0) {
            throw new InvalidArgumentException("Need sampleCount or classCount");
        }
        setNextProgress(0.5);
        prepare();

        setNextProgress(0.9);
        generateInnerClazz();
        setNextProgress(1.0);
        generateInterClazzAll();

        finish();
    }

    @Override protected void prepareSelectedMap() throws Exception {
        long total = sampleCount * classCount;
        double delta = (nextProgress - progress) / total;

        List<Clazz> clazzs = view.getClazzs();

        for (Clazz clazz : clazzs) {
            if (selectedMap.size() >= this.classCount)
                break;

            if (clazz.getSampleCount() < this.sampleCount)
                continue;

            List<Sample> samples = view.getSamples(clazz);

            Pair<Clazz, List<Sample>> newPair = new ImmutablePair<Clazz, List<Sample>>(clazz, samples.subList(0, sampleCount));
            selectedMap.add(newPair);
            if (selectedMap.size() % 30 == 0) {
                progress += delta * 30;
                writeProgress();
            }
        }
        if (selectedMap.size() < this.classCount) {
            throw new BenchmarkGeneratorException("Not enough classes");
        }
    }

    @Override public String getStrategyName() {
        return "AllNStrategy";
    }
}
