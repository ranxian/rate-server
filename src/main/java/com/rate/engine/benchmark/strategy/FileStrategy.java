package com.rate.engine.benchmark.strategy;

import com.rate.engine.sample.Sample;
import com.rate.utils.BXXUtils;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.util.List;

/**
 * Created by Ran Xian on 4/27/14.
 */
// Generate Benchmark given a file. Format of the file is like
// SampleID1, SampleID2
// SampleID2, SampleID3
// ...
public class FileStrategy extends BasicStrategy {
    private File file;

    public FileStrategy(File file) {
        super();
        this.file = file;
    }

    @Override
    public void apply() throws Exception {
        // 准备
        setNextProgress(0.3);
        prepare();
        // Generate
        setNextProgress(1.0);
        List<String> lines = FileUtils.readLines(file);
        long total = lines.size();
        double delta = (nextProgress - progress) / total;

        int count = 0;
        for (String line : lines) {
            if (line.startsWith("#")|| line.equals(""))
                continue;
            String[] sp = line.split(" ");
            Sample sample1 = Sample.find(sp[0]);
            Sample sample2 = Sample.find(sp[1]);

            if (sample1.getClassUuid().equals(sample2.getClassUuid())) {
                printInner(sample1, sample2);
            } else {
                printInter(sample1, sample2);
            }

            count += 1;

            if (count % 30 == 0) {
                progress += delta * 30;
                writeProgress();
            }
        }

        finish();
    }

    @Override protected void buildUuidTable() throws Exception {
        List<String> lines = FileUtils.readLines(file);
        long total = lines.size();

        double delta = (nextProgress - progress) / total;
        long count = 0;
        for (String line : lines) {
            if (line.startsWith("#")|| line.equals(""))
                continue;
            String[] sp = line.split(" ");
            Sample sample1 = Sample.find(sp[0]);
            Sample sample2 = Sample.find(sp[1]);

            if (!uuidTable.containsKey(sp[0])) {
                uuidTable.put(sp[0], BXXUtils.parse(uuidTable.size() + 1));
                enrollMap.put(sp[0], sample1.getFile());
            }
            if (!uuidTable.containsKey(sp[1])) {
                uuidTable.put(sp[1], BXXUtils.parse(uuidTable.size()+1));
                enrollMap.put(sp[1], sample2.getFile());
            }

            count += 1;
            if (count % 30 == 0) {
                progress += delta * 30;
                writeProgress();
            }
        }
    }

    @Override protected void prepareSelectedMap() throws Exception {
        // Do nothing
    }

    @Override public String getStrategyName() {
        return "FileStrategy";
    }
}
