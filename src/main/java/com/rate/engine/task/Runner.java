package com.rate.engine.task;

import com.rate.engine.algorithm.Algorithm;
import com.rate.engine.benchmark.Benchmark;
import com.rate.engine.task.analyzer.BasicAnalyzer;
import com.rate.engine.task.analyzer.GeneralAnalyzer;
import com.rate.utils.DBUtils;
import com.rate.utils.RateConfig;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ran Xian on 3/18/14.
 */
// Given a benchmark and algorithm, the runner creates a new task, and execute the task.
public class Runner implements Runnable {
    private final Logger logger = Logger.getLogger(Runner.class);
    private Benchmark benchmark;
    private Algorithm algorithm;
    @Getter private Task task;

    public Runner(Task task) {
        this.task = task;
        this.benchmark = task.getBenchmark();
        this.algorithm = task.getAlgorithm();
    }

    // Run it!
    public void run() {
        logger.info("start to run task");
        try {
            if (RateConfig.isDistRun()) {
                String cmd = buildDistCommand();
                logger.trace("Run with command " + cmd);
                System.out.println("Run with command " + cmd);
                Process process = Runtime.getRuntime().exec(cmd);

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                task.prepare();

                // Write pid of this running task.
                // Notice! Just for UNIX
                if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
                    Field field = process.getClass().getDeclaredField("pid");
                    field.setAccessible(true);
                    Integer pid = field.getInt(process);
                    File file = new File(task.getTaskPidPath());
                    PrintWriter pw = new PrintWriter(file);
                    pw.println(pid);
                    pw.close();
                }
                logger.debug(process.getClass().getName());

                // Write executing log for task
                File logFile = new File(task.getLogPath());
                PrintWriter writer = new PrintWriter(new FileWriter(logFile));

                while (true) {
                    String line = reader.readLine();
                    if (line == null) break;
                    writer.println(line);
                    writer.flush();
                }
                writer.close();
                reader.close();

                // Wait for finish
                try {
                    process.waitFor();
                    if (process.exitValue() != 0)
                        return;
                } catch (InterruptedException e) {
                    logger.info("process interrupted");
                    process.destroy();
                    return;
                }
                logger.trace("finished");
            } else {
                throw new Exception("Must run with DIST option.");
            }

            // Get an analyzer and analyze
            Class<?> analyzerClass;
            if (benchmark.getAnalyzer().equals("SLSB")) {
                throw new Exception("not implemented");
            } else {
                // As we only have GeneralBenchmark now in this system
                analyzerClass = GeneralAnalyzer.class;
            }

            // Copy uuidTable for researchers' convenient
            FileUtils.copyFileToDirectory(new File(task.getBenchmark().getUuidTableFilePath()), new File(task.getDirPath()));

            BasicAnalyzer analyzer = (BasicAnalyzer)analyzerClass.newInstance();
            analyzer.setTask(task);
            System.out.println(String.format("Attempt to analyze task [%s] with analyzer [%s]", task.getUuid(), analyzerClass.getName()));
            analyzer.analyze();
            System.out.println(String.format("Analyze task [%s] with analyzer [%s] finished", task.getUuid(), analyzerClass.getName()));

            // Update task state
            task.setFinished(DBUtils.getCurrentTimestamp());
            task.save();

            logger.info("task done.");
        } catch (Exception e) {
            logger.error("", e);
        } finally {
            // Delete pid file. Only running tasks have a pid file.
            if (new File(task.getTaskPidPath()).exists()) {
                try {
                    FileUtils.forceDelete(new File(task.getTaskPidPath()));
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error("can't delete task [" + task.getUuid() + "] pid file");
                }
            }
        }
    }
    // Generate command to execute task by distributed system
    private String buildDistCommand() {
        List<String> list = new ArrayList<String>();
        list.add(RateConfig.getPythonPath());
        list.add("-u");
        list.add(RateConfig.getDistProducerPath());
        list.add(benchmark.getUuid());
        list.add(algorithm.getUuid());

        list.add(task.getUuid());

        list.add("10000");
        list.add("50000000");
        return StringUtils.join(list, " ");
    }
}
