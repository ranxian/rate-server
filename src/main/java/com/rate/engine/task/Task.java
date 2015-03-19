package com.rate.engine.task;

import com.rate.engine.algorithm.Algorithm;
import com.rate.engine.RateBeanProcessor;
import com.rate.engine.benchmark.Benchmark;
import com.rate.utils.DBUtils;
import com.rate.utils.RateConfig;
import lombok.Data;
import net.sf.json.JSONObject;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Created by Ran Xian on 3/18/14.
 */
// Task DAO
@Data
public class Task {
    private String uuid;
    private String benchmarkUuid;
    private Timestamp created;
    private Timestamp finished;
    private String algorithmUuid;
    private Double score;
    private Double progress;

    public static final BeanHandler<Task> handler = new BeanHandler<Task>(Task.class, new BasicRowProcessor(new RateBeanProcessor()));
    public static final BeanListHandler<Task> listHandler = new BeanListHandler<Task>(Task.class, new BasicRowProcessor(new RateBeanProcessor()));

    public Task() {
        created = DBUtils.getCurrentTimestamp();
        uuid = UUID.randomUUID().toString();
        score = 0.0;
    }

    public Benchmark getBenchmark() {
        return Benchmark.find(benchmarkUuid);
    }

    public Algorithm getAlgorithm() {
        return Algorithm.find(algorithmUuid);
    }

    public String getLogPath() {
        return FilenameUtils.concat(getDirPath(), "log.txt");
    }

    public String getDirPath() {
        String p = FilenameUtils.concat(RateConfig.getTaskRootDir(), this.getUuid());
        return FilenameUtils.separatorsToUnix(p);
    }

    public String getResultFilePath() {
        String p = FilenameUtils.concat(getDirPath(), "match_result_bxx.txt");
        return FilenameUtils.separatorsToUnix(p);
    }

    public String getTempDirPath() {
        String p = FilenameUtils.concat(FilenameUtils.concat(RateConfig.getTempRootDir(), "tasks"), this.getUuid());
        String r = FilenameUtils.separatorsToUnix(p);
        return r;
    }

    public Task save() {
        DBUtils.executeSQL("REPLACE INTO task (uuid, benchmark_uuid, created, finished, algorithm_uuid, score) " +
                "VALUES(?,?,?,?,?,?)", this.uuid, this.benchmarkUuid, this.created, this.finished, this.algorithmUuid, this.score);
        return Task.find(uuid);
    }

    public static Task find(String uuid) {
        return DBUtils.executeSQL(Task.handler, "SELECT * FROM task WHERE uuid=?", uuid);
    }

    public void killSelf() throws Exception {
        Integer pid = this.getTaskPid();
        if (pid == null)
            throw new Exception("No running instance");
        Process process = Runtime.getRuntime().exec("kill -9 " + pid);
        process.waitFor();
    }

    public void destroy() {
        try {
            DBUtils.executeSQL("DELETE FROM task where uuid=?", this.uuid);
            File file = new File(getDirPath());
            if (file.exists())
                FileUtils.deleteDirectory(file);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            // ignore IOException
        }
    }

    public void prepare() {
        File file = new File(this.getTempDirPath());
        try {
            if (!file.exists())
                FileUtils.forceMkdir(file);
            file = new File(this.getDirPath());
            if (!file.exists())
                FileUtils.forceMkdir(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getTaskState() {
        try {
            BufferedReader stateReader = new BufferedReader(new FileReader(getTaskStatePath()));
            // time
            String line = StringUtils.strip(stateReader.readLine());
            this.progress = Double.parseDouble(line);

            stateReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getTaskStatePath() {
        return FilenameUtils.concat(this.getDirPath(), "state.txt");
    }

    public String getTaskPidPath() {
        return FilenameUtils.concat(this.getDirPath(), "task.pid");
    }

    public Integer getTaskPid() {
        try {
            return Integer.parseInt(FileUtils.readLines(new File(this.getTaskPidPath())).get(0));
        } catch (IOException e ){
            e.printStackTrace();
            return null;
        }
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();

        object.put("uuid", this.uuid);
        object.put("buuid", this.benchmarkUuid);
        object.put("auuid", this.algorithmUuid);
        object.put("created", this.created.toString());
        object.put("score", this.score);
        if (this.finished != null)
            object.put("finished", this.finished.toString());

        if (new File(this.getTaskStatePath()).exists()) {
            getTaskState();
        }
        object.put("progress", this.progress);
        return object;
    }
}
