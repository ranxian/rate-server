package com.rate.engine.algorithm;

import com.rate.engine.RateBeanProcessor;
import com.rate.engine.task.Task;
import com.rate.utils.DBUtils;
import com.rate.utils.RateConfig;
import lombok.Data;
import lombok.ToString;
import net.sf.json.JSONObject;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Created by Ran Xian on 3/12/14.
 */
// Algorithm DAO
@Data @ToString
public class Algorithm {
    Logger logger = Logger.getLogger(Algorithm.class.getName());
    private String uuid;
    private Timestamp created;

    public Algorithm() {
        this.uuid = UUID.randomUUID().toString();
    }

    public static final BeanHandler<Algorithm> handler = new BeanHandler<Algorithm>(Algorithm.class, new BasicRowProcessor(new RateBeanProcessor()));
    public static final BeanListHandler<Algorithm> listHandler = new BeanListHandler<Algorithm>(Algorithm.class, new BasicRowProcessor(new RateBeanProcessor()));

    public void destroy() throws Exception {
        // delete task
        List<Task> tasks = DBUtils.executeSQL(Task.listHandler, "SELECT * FROM task WHERE algorithm_uuid=?", this.uuid);

        for (Task task : tasks) {
            task.destroy();
        }

        File file = new File(dirPath());
        if (file.exists())
            FileUtils.forceDelete(new File(dirPath()));
        DBUtils.executeSQL("DELETE FROM algorithm where uuid=?", this.uuid);
        logger.info("algorithm " + this.uuid + " deleted");
    }

    public String bareDirPath() {
        return "algorithms" + "/" + this.getUuid();
    }

    public String dirPath() {
        String dir = FilenameUtils.concat(RateConfig.getAlgorithmRootDir(), this.getUuid());
        return FilenameUtils.separatorsToUnix(dir);
    }

    public static Algorithm find(String uuid) {
        return DBUtils.executeSQL(Algorithm.handler, "SELECT * FROM algorithm WHERE uuid=?", uuid);
    }

    public void save() throws Exception {
        DBUtils.executeSQL("REPLACE INTO algorithm (uuid, created) " +
                "VALUES(?,null)", this.uuid);
    }

    public void mkdir() throws Exception {
        FileUtils.forceMkdir(new File(dirPath()));
    }

    public void syncFile(String filePath) throws Exception {
        File src = new File(filePath);
        if (!src.exists() || !src.isDirectory()) {
            logger.fatal(String.format("Unable to sync algorithm files from %s to %s", filePath, dirPath()));
            throw new Exception("CantSyncFile");
        }
        File dst = new File(dirPath());

        if (!dst.exists()) {
            FileUtils.forceMkdir(dst);
        }

        FileUtils.copyDirectory(src, dst);
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();

        object.put("uuid", this.uuid);
        object.put("created", this.created.toString());
        return object;
    }
}
