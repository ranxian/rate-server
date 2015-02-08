package com.rate.engine.benchmark.strategy;

import com.rate.engine.clazz.Clazz;
import com.rate.engine.sample.Sample;
import com.rate.engine.exception.BenchmarkGeneratorException;
import lombok.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by Ran Xian on 3/13/14.
 */
// Generate Benchmark given @classCount and @sampleCount
// The strategy will randomly choose @classCount Class with @sampleCount samples each Class
// Then generate all inner matches possible, and equal number of inter matches.
//
public class GeneralStrategy extends BasicStrategy {
    private static final Logger logger = Logger.getLogger(GeneralStrategy.class);

    @Setter @Getter private int classCount = 0;
    @Setter @Getter private int sampleCount = 0;

    @Override
    public void apply() throws Exception {
        if (classCount==0 || sampleCount==0 || viewUuid == null) {
            throw new BenchmarkGeneratorException("No classCount or sampleCount or view or strategy name specified");
        }
        // 准备
        setNextProgress(0.5);
        prepare();
        // 生成类内匹配
        setNextProgress(0.6);
        generateInnerClazz();
        // 生成类间匹配
        setNextProgress(1.0);
        generateInterClazzOne(this.innerCount);
        // 结束
        finish();
    }

    protected void prepareSelectedMap() throws Exception {
        logger.debug("prepare selected map");

        List<Clazz> clazzs = view.getClazzs();

        double delta = (nextProgress - progress) / (classCount * sampleCount);

        for (Clazz clazz : clazzs) {
            if (selectedMap.size() >= this.classCount)
                break;

            if (clazz.getSampleCount() < this.sampleCount)
                continue;

            List<Sample> samples = view.getSamples(clazz);

            Pair<Clazz, List<Sample>> newPair = new ImmutablePair<Clazz, List<Sample>>(clazz, samples.subList(0, sampleCount));
            selectedMap.add(newPair);
            logger.debug("Add class " + clazz.getUuid());
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
        return "GeneralStrategy";
    }
}
