package com.rate.engine.benchmark.strategy;

import com.rate.engine.clazz.Clazz;
import com.rate.engine.sample.Sample;
import com.rate.engine.benchmark.Benchmark;
import com.rate.engine.view.View;
import com.rate.utils.BXXUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by Ran Xian on 3/13/14.
 */

/* Abstract class for all benchmark strategy, provide consistent interfaces and helper methods.
 * Generator extends this class needs to implement the generate() method.
 * The generation of a benchmark is to write benchmark content to benchmark_bxx.txt and write a
 * bxx_id -> sample_uuid, sample_file dictionary to uuid_table.txt.
 */
abstract public class BasicStrategy {
    private static Logger logger = Logger.getLogger(BasicStrategy.class);
    @Setter @Getter protected Benchmark benchmark;
    @Setter @Getter protected PrintWriter uuidTableWriter;
    @Setter @Getter protected PrintWriter benchmarkWriter;
    @Getter protected String viewUuid;
    @Setter @Getter protected String analyzer;
    @Getter protected View view;
    // Controll process printing, subclass could change it in constructor
    protected double nextProgress = 0.0;
    protected double progress = 0.0;
    // Write progress by a given writer
    @Setter protected PrintWriter progressWriter;
    // Sample's uuid to bxx map
    protected HashMap<String, String> uuidTable = new HashMap<String, String>();
    // Sample's uuid to sample's file path map
    protected HashMap<String, String> enrollMap = new HashMap<String, String>();
    // Selected sample for this benchmark, in a list of <Class, [Sample1, Sample2, ...]>
    // Further methods, like buildUuidTable, printInter, printInner, are based on it
    protected List<Pair<Clazz, List<Sample>>> selectedMap = new ArrayList<Pair<Clazz, List<Sample>>>();
    @Getter long innerCount = 0;
    @Getter long interCount = 0;

    ///// uuidTable related /////
    // Build uuid table by selectedMap
    protected void buildUuidTable() throws Exception {
        for (Pair<Clazz, List<Sample>> clazzSamples : selectedMap) {
            List<Sample> samples = clazzSamples.getValue();

            for (Sample sample : samples) {
                if (!uuidTable.containsKey(sample.getUuid()))
                    uuidTable.put(sample.getUuid(), BXXUtils.parse(uuidTable.size() + 1));
                enrollMap.put(sample.getUuid(), sample.getFile());
            }
        }
    }
    // Print uuid table to benchmark's uuid_table.txt
    protected void printUuidTable() throws Exception {
        PrintWriter writer = new PrintWriter(new FileWriter(benchmark.getUuidTableFilePath()));
        ArrayList<Map.Entry<String, String>> list = new ArrayList<Map.Entry<String, String>>(uuidTable.entrySet());
        for (Map.Entry<String, String> entry : list) {
            writer.println(entry.getValue() + " " + entry.getKey() + " " + enrollMap.get(entry.getKey()));
        }
        writer.close();
    }
    ///// Helpers for generating benchmark /////
    // Write progress to progress writer
    protected void writeProgress() {
        progressWriter.println(progress);
        progressWriter.flush();
    }
    // Set goal for @nextProgress, and update @progress
    protected void setNextProgress(double nextProgress) {
        this.progress = this.nextProgress;
        if (nextProgress > this.nextProgress)
            this.nextProgress = progress;
    }
    protected void printInter(Sample sample1, Sample sample2) throws Exception {
        this.interCount += 1;
        benchmarkWriter.println(String.format("%s %s I", uuidTable.get(sample1.getUuid()), uuidTable.get(sample2.getUuid())));
    }
    protected void printInner(Sample sample1, Sample sample2) throws Exception {
        this.innerCount += 1;
        benchmarkWriter.println(String.format("%s %s G", uuidTable.get(sample1.getUuid()), uuidTable.get(sample2.getUuid())));
    }
    // Generate inter class matches using selectedClazz. Every clazz chooses one clazz to be compared with,
    // with maximum @max matches to be generated.
    protected long generateInterClazzOne(long max) throws Exception {
        logger.debug("generate inter class");
        long count = 0;
        if (max < 0)
            max = selectedMap.size() * (selectedMap.size()-1) / 2;
        double delta = (nextProgress-progress) / max;

        for (int i=0; i<selectedMap.size()-1; i++) {
            Pair<Clazz, List<Sample>> pair1 = selectedMap.get(i);
            Sample sample1 = pair1.getValue().get(0);
            // Match with remaining
            for (int j=i+1; j<selectedMap.size(); j++) {
                Pair<Clazz, List<Sample>> pair2 = selectedMap.get(j);
                Sample sample2 = pair2.getValue().get(0);
                printInter(sample1, sample2);
                count++;
                if (count % 100 == 0) {
                    progress += delta * 100;
                    writeProgress();
                }
                if (count == max && max >= 0) {
                    return count;
                }
            }
        }
        return count;
    }
    // Generate inter class matches using selectedClazz. Every sample matches with all samples from different
    // classes.
    protected long generateInterClazzAll() throws Exception {
        logger.debug("generate inter class");
        long count = 0;
        long total = 0;
        for (int i = 0; i < selectedMap.size()-1; i++) {
            for (int j = i+1; j < selectedMap.size(); j++) {
                total += selectedMap.get(i).getRight().size() *
                        selectedMap.get(j).getRight().size();
            }
        }

        double delta = (nextProgress - progress) / total;

        for (int i=0; i<selectedMap.size()-1; i++) {
            Pair<Clazz, List<Sample>> pair1 = selectedMap.get(i);
            List<Sample> samples1 = pair1.getValue();
            // Match with remaining
            for (int j=i+1; j<selectedMap.size(); j++) {
                Pair<Clazz, List<Sample>> pair2 = selectedMap.get(j);
                List<Sample> samples2 = pair2.getRight();
                for (Sample sample1 : samples1) {
                    for (Sample sample2: samples2) {
                        count++;
                        printInter(sample1, sample2);
                        if (count % 100 == 0) {
                            progress += 100 * delta;
                            writeProgress();
                        }
                    }
                }
            }
        }
        return count;
    }
    // Generate all inner class matches.
    protected long generateInnerClazz() throws Exception {
        logger.debug("generate inner class");
        long count = 0;
        long total = 0;

        for (int i = 0; i < selectedMap.size(); i++) {
            List<Sample> samples = selectedMap.get(i).getValue();
            total += samples.size() * (samples.size()-1) / 2;
        }

        double delta = (nextProgress - progress) / total;

        // inner class
        for (int i = 0; i < selectedMap.size(); i++) {
            List<Sample> samples = selectedMap.get(i).getValue();

            for (int j = 0; j < samples.size()-1; j++) {
                Sample sample1 = samples.get(j);
                for (int k = j + 1; k < samples.size(); k++) {
                    Sample sample2 = samples.get(k);
                    if (sample1.getUuid().equals(sample2.getUuid())) {
                        continue;
                    }
                    printInner(sample1, sample2);
                    count++;
                    if (count % 100 == 0) {
                        progress += 100 * delta;
                        writeProgress();
                    }
                }
            }
        }
        return count;
    }
    // Set this.view when setting this.viewUuid
    public void setViewUuid(String viewUuid) {
        this.viewUuid = viewUuid;
        this.view = View.find(viewUuid);
    }
    public void setView(View view) {
        this.view = view;
        this.viewUuid = view.getUuid();
    }
    // Subclass should define how to generate @selectedMap
    abstract protected void prepareSelectedMap() throws Exception;
    // Preparation for strategy. prepare() should prepare selectedMap and uuid_table.txt
    protected void prepare() throws Exception {
        this.uuidTableWriter = new PrintWriter(benchmark.getUuidTableFilePath());
        this.benchmarkWriter = new PrintWriter(benchmark.getHexFilePath());
        prepareSelectedMap();
        buildUuidTable();
        printUuidTable();
    }
    // Close open file writer, this must be called after calling apply()
    protected void finish() {
        this.uuidTableWriter.close();
        this.benchmarkWriter.close();
    }
    // Apply strategies, generate benchmark_bxx.txt, apply() first calls prepare(), then finish();
    abstract public void apply() throws Exception;
    abstract public String getStrategyName();
}