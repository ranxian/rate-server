package com.rate.server;

import com.rate.engine.benchmark.Benchmark;
import com.rate.engine.benchmark.strategy.*;
import com.rate.engine.exception.InvalidArgumentException;
import com.rate.engine.view.View;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * Created by Ran Xian on 3/14/14.
 * <create_benchmark> := create benchmark name:<string> view:<uuid> desc:<string> <allinter:true|false allinner:true|false all:true|false
 * classcount:<number> samplecount:<number>  >
 */
public class BenchmarkUtils {

    public static Benchmark create(HashMap<String, String> args) throws Exception {
        PrintWriter writer = new PrintWriter(System.out);
        Benchmark benchmark = create(args, writer);
        writer.close();
        return benchmark;
    }

    public static Benchmark create(HashMap<String, String> args, PrintWriter writer) throws Exception {
        Benchmark benchmark;

        // View uuid, benchmark name must be given
        if (args.get("view_uuid") == null)
            throw new InvalidArgumentException("Not enough argument to create benchmark");

        // Parameters to generate benchmark
        String stragetyType = args.get("strategy");
        if (stragetyType == null)
            throw new InvalidArgumentException("Not enough argument to create benchmark");

        String viewUuid = args.get("view_uuid");
        BasicStrategy strategy;

        if (stragetyType.equals("allinter")) {
            strategy = new AllInterStrategy();
        } else if (stragetyType.equals("allinner")) {
            strategy = new AllInnerStrategy();
        } else if (stragetyType.equals("all")) {
            strategy = new AllStrategy();
        } else if (stragetyType.equals("allInnerOneInter")) {
            strategy= new AllInnerOneInterStrategy();
        } else if (stragetyType.equals("general")) {
            if (args.get("class_count") == null || args.get("sample_count") == null)
                throw new InvalidArgumentException("need class_count and sample_count to create normal benchmark");
            int classCount = Integer.parseInt(args.get("class_count"));
            int sampleCount = Integer.parseInt(args.get("sample_count"));


            if (classCount == 0 || sampleCount == 0) {
                throw new InvalidArgumentException("class_count or sample_count can't be 0");
            }

            GeneralStrategy generalStrategy = new GeneralStrategy();
            generalStrategy.setClassCount(classCount);
            generalStrategy.setSampleCount(sampleCount);

            strategy = generalStrategy;
        } else if (stragetyType.equals("file")) {
            File file = new File(args.get("filePath"));
            strategy = new FileStrategy(file);
        } else if (stragetyType.equals("allN")) {
            if (args.get("class_count") == null || args.get("sample_count") == null)
                throw new InvalidArgumentException("need class_count and sample_count to create normal benchmark");
            int classCount = Integer.parseInt(args.get("class_count"));
            int sampleCount = Integer.parseInt(args.get("sample_count"));


            if (classCount == 0 || sampleCount == 0) {
                throw new InvalidArgumentException("class_count or sample_count can't be 0");
            }
            AllNStrategy allNStrategy = new AllNStrategy();
            allNStrategy.setClassCount(classCount);
            allNStrategy.setSampleCount(sampleCount);

            strategy = allNStrategy;
        } else {
            throw new InvalidArgumentException("can't use " + stragetyType + " as strategy");
        }

        View view = View.find(viewUuid);
        if (view == null) {
            throw new InvalidArgumentException("No view with uuid " + viewUuid);
        }
        strategy.setProgressWriter(writer);
        strategy.setViewUuid(viewUuid);
        strategy.setAnalyzer("General");

        benchmark = Benchmark.generate(strategy, writer);
        benchmark.save();
        benchmark = Benchmark.find(benchmark.getUuid());
        return benchmark;
    }

    public static boolean delete(String uuid) throws Exception {
        Benchmark.find(uuid).destroy();
        return true;
    }
}
