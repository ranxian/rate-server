package com.rate.server;

import com.rate.engine.algorithm.Algorithm;
import com.rate.engine.benchmark.Benchmark;
import com.rate.engine.exception.InvalidArgumentException;
import com.rate.engine.task.Runner;
import com.rate.engine.task.Task;
import com.rate.utils.RateConfig;
import com.rate.utils.ZipUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;

/**
 * Created by Ran Xian on 3/14/14.
 * AlgorithmUtils handles server side APIs about algorithms, including creating and
 * deleting algorithms
 */
public class AlgorithmUtils {
    private static Logger logger = Logger.getLogger(AlgorithmUtils.class.getName());

    // Create algorithm, and check the usability of it
    public static Algorithm create(HashMap<String, String> args) throws Exception {
        Algorithm algorithm = new Algorithm();

        try {
            algorithm.mkdir();

            File[] files = ZipUtils.unzip(args.get("filePath"), RateConfig.getTempRootDir(), null);

            for (File file : files) {
                FileUtils.copyFileToDirectory(file, new File(algorithm.dirPath()));
            }
            FileUtils.deleteDirectory(files[0].getParentFile());

            algorithm.save();
            algorithm = Algorithm.find(algorithm.getUuid());
        } catch (Exception e) {
            algorithm.destroy();
            algorithm = null;
            e.printStackTrace();
        }

        return algorithm;
    }

    public static boolean delete(String uuid) throws Exception {
        Algorithm.find(uuid).destroy();
        return true;
    }
}
