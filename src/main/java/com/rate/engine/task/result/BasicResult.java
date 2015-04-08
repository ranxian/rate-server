package com.rate.engine.task.result;

import com.rate.engine.task.Task;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

/**
 * Created by Ran Xian on 3/18/14.
 */

// TaskResult is responsible to gather useful information of the results of tasks.
// It is a representation of task results.
// BasicResult is the Basic class for all XXTaskResult. Provides general interfaces and helpers methods.
@Data
public class BasicResult {
    private final Logger logger = Logger.getLogger(BasicResult.class);

    // Task represents a task entity in database
    @Getter
    protected Task task;
    protected String enrollExeFilePath;
    protected String matchExeFilePath;
    protected String resultFilePath;
    protected String enrollResultFilePath;
    protected String FTEFilePath;
    protected String FTMFilePath;
    protected int FTE;
    protected int FTM;

    public BasicResult(Task task) {
        this.task = task;
        // Prepare file paths
        enrollExeFilePath = FilenameUtils.concat(task.getAlgorithm().dirPath(), "enroll.exe");
        matchExeFilePath = FilenameUtils.concat(task.getAlgorithm().dirPath(), "match.exe");
        resultFilePath = FilenameUtils.concat(task.getDirPath(), "enroll_result.txt");
        FTEFilePath = FilenameUtils.concat(task.getDirPath(), "FTE.txt");
        FTMFilePath = FilenameUtils.concat(task.getDirPath(), "FTM.txt");
    }
}
