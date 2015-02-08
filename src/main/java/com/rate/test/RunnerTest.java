package com.rate.test;

import com.rate.engine.task.Runner;
import com.rate.engine.task.Task;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Created by Ran Xian on 3/18/14.
 */
public class RunnerTest {
    public static void main(String args[]) throws Exception {
        Task task = new Task();
        try {
            String buuid = "ce5326aa-0d32-41f0-b1df-37723fee99d1";
            String auuid = "7a0d4668-8423-460d-9ab0-eac23e380599";
            task.setBenchmarkUuid(buuid);
            task.setAlgorithmUuid(auuid);
            task = task.save();
            Runner runner = new Runner(task);

            Thread thread = new Thread(runner);
            thread.start();
            thread.join();
            thread.interrupt();

            String log = FileUtils.readFileToString(new File(task.getLogPath()));
            System.out.println(log);
            System.out.println("-");

        } finally {
            task.destroy();
        }
    }
}
