package com.rate.engine.benchmark;

import com.rate.engine.task.Task;
import com.rate.engine.view.View;
import com.rate.engine.benchmark.strategy.BasicStrategy;
import com.rate.utils.DBUtils;
import com.rate.engine.RateBeanProcessor;
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
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Ran Xian on 3/12/14.
 */
// Benchmark DAO
@ToString @Data
public class Benchmark {
    private static final Logger logger = Logger.getLogger(Benchmark.class.getName());
    private String uuid;
    private String viewUuid;
    private Timestamp created;
    // Generator indicates how this benchmark is generated
    private String strategy;
    private String analyzer;
    private long numOfGenuine;
    private long numOfImposter;
    public static BeanHandler<Benchmark> handler = new BeanHandler<Benchmark>(Benchmark.class, new BasicRowProcessor(new RateBeanProcessor()));
    public static BeanListHandler<Benchmark> listHandler = new BeanListHandler<Benchmark>(Benchmark.class, new BasicRowProcessor(new RateBeanProcessor()));
    // Init a new private benchmark
    public Benchmark() {
        this.uuid = UUID.randomUUID().toString();
    }
    // Get View the benchmark created on
    public View getView() {
        return View.find(viewUuid);
    }
    // Get benchmark's storage path in file system
    public String dirPath() {
        return FilenameUtils.concat(RateConfig.getBenchmarkRootDir(), this.getUuid());
    }
    // Get benchmark's uuid_table.txt file path
    public String getUuidTableFilePath() {
        return FilenameUtils.concat(this.dirPath(), "uuid_table.txt");
    }
    // Get benchmark's bxx file path
    public String getHexFilePath() {
        return FilenameUtils.concat(this.dirPath(), "benchmark_bxx.txt");
    }
    // Get a list of paths of samples needed to be enrolled for this benchmark
    public List<String> enrolledSamplePaths() {
        List<String> paths = new ArrayList<String>();
        File table = new File(getUuidTableFilePath());
        try {
            List<String> lines = FileUtils.readLines(table);
            for (String line : lines) {
                String[] sp = line.split(" ");
                paths.add(sp[2]);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }

        return paths;
    }
    // Destroy a benchmark, delete files and tasks related to it
    public void destroy() {
        // Delete task
        List<Task> tasks = DBUtils.executeSQL(Task.listHandler, "SELECT * FROM task WHERE benchmark_uuid=?", this.uuid);

        for (Task task : tasks) {
            task.destroy();
        }

        try {
            File dir = new File(dirPath());
            if (dir.exists())
                FileUtils.forceDelete(new File(dirPath()));
            DBUtils.executeSQL("DELETE FROM benchmark where uuid=?", this.uuid);
        } catch (Exception e) {
            System.out.println("can't delete benchmark file, abort");
            return;
        }
        logger.info("benchmark " + this.uuid + " deleted");
    }
    // Save a benchmark to database
    public void save() throws Exception {
        DBUtils.executeSQL("REPLACE INTO benchmark (uuid, view_uuid, strategy, analyzer, num_of_genuine, num_of_imposter) " +
                "VALUES(?,?,?,?,?,?)", this.uuid, this.viewUuid, this.strategy, this.analyzer, this.numOfGenuine, this.numOfImposter);
    }
    // Serialize a benchmark to a json object
    public JSONObject toJSON() {
        JSONObject object = new JSONObject();

        object.put("uuid", this.uuid);
        object.put("created", this.created.toString());
        object.put("view_uuid", this.viewUuid);
        object.put("strategy", this.strategy);
        object.put("genuine_count", this.numOfGenuine);
        object.put("imposter_count", this.numOfImposter);

        return object;
    }
    // Class Methods
    // Generate Benchmark given a strategy
    public static Benchmark generate(BasicStrategy strategy) throws Exception {
        return generate(strategy, new PrintWriter(System.out));
    }
    public static Benchmark generate(BasicStrategy strategy, PrintWriter progressWriter) throws Exception {
        progressWriter.println("PROGRESS");
        progressWriter.println("0.00");
        progressWriter.flush();

        // Initialize a benchmark
        Benchmark benchmark = new Benchmark();
        // Set properties
        benchmark.setViewUuid(strategy.getViewUuid());
        benchmark.setStrategy(strategy.getStrategyName());
        benchmark.setAnalyzer(strategy.getAnalyzer());
        // Prepare benchmark dir
        File dir = new File(benchmark.dirPath());
        if (!dir.mkdirs()) {
            throw new IOException("can't create benchmark dir");
        }
        strategy.setBenchmark(benchmark);
        // Generate benchmark, benchmark_bxx.txt is ready
        strategy.apply();
        benchmark.setNumOfGenuine(strategy.getInnerCount());
        benchmark.setNumOfImposter(strategy.getInterCount());
        logger.trace(String.format("Benchmark [%s], view [%s]", benchmark.getUuid(),
                benchmark.getViewUuid()));
        // Save the benchmark
        benchmark.save();

        progressWriter.println("DONE");
        progressWriter.flush();

        return Benchmark.find(benchmark.getUuid());
    }
    // Find a benchmark by uuid
    public static Benchmark find(String uuid) {
        return DBUtils.executeSQL(handler, "SELECT * FROM benchmark WHERE uuid=?", uuid);
    }
    // Get all benchmarks
    public static List<Benchmark> all() throws Exception {
        return DBUtils.getRunner().query("select * from benchmark", listHandler);
    }
}
