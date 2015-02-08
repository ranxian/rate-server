package com.rate.server;

import com.rate.engine.task.Runner;
import com.rate.engine.task.Task;
import com.rate.utils.DBUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;

/**
 * Created by Ran Xian on 3/18/14.
 */
public class RunnerUtils {
    private static final Logger logger = Logger.getLogger(RunnerUtils.class);

    public static String run(HashMap<String, String> args) throws Exception {
        if (args.get("auuid") == null || args.get("buuid") == null) {
            logger.fatal("algorithm uuid and benchmark uuid must be given");
            throw new Exception();
        }
        logger.info(args.toString());
        Task task = null;
        Task task2 = DBUtils.executeSQL(Task.handler,
                "SELECT * FROM task WHERE algorithm_uuid=? and benchmark_uuid=?",
                args.get("auuid"), args.get("buuid"));

        // Start a new task
        if (task2 == null) {
            task = new Task();
            task.setAlgorithmUuid(args.get("auuid"));
            task.setBenchmarkUuid(args.get("buuid"));
            task = task.save();
        // Re-run the task
        } else {
            logger.debug("rerun task");
            task = task2;
        }
        // Kill previous process if pid file exist
        if (new File(task.getTaskPidPath()).exists()) {
            task.killSelf();
            logger.debug("killed previous process");
        }

        logger.info("Running task [" + task.getUuid() + "]");

        Runner runner = new Runner(task);
        Thread thread = new Thread(runner);
        thread.start();

        return task.getUuid();
    }

    public static String info(HashMap<String, String> args) throws Exception {
        String uuid = args.get("uuid");
        Task task = Task.find(uuid);
        File file = new File(task.getLogPath());
        return FileUtils.readFileToString(file);
    }

    public static boolean delete(String uuid) throws Exception {
        Task task = Task.find(uuid);
        if (new File(task.getTaskPidPath()).exists())
            task.killSelf();
        task.destroy();
        return true;
    }
}
