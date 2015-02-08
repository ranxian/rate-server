package com.rate.engine.view;

import com.rate.engine.clazz.Clazz;
import com.rate.engine.benchmark.Benchmark;
import com.rate.engine.view.strategy.BasicStrategy;
import com.rate.utils.DBUtils;
import com.rate.engine.RateBeanProcessor;
import com.rate.engine.sample.Sample;
import lombok.Data;
import lombok.ToString;
import net.sf.json.JSONObject;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by Ran Xian on 3/12/14.
 */
@Data
@ToString
public class View {
    private static Logger logger = Logger.getLogger(View.class);

    private String uuid;
    private String strategy;
    private Timestamp created;
    private Integer numOfClasses;
    private Integer numOfSamples;
    public static final BeanHandler<View> handler = new BeanHandler<View>(View.class, new BasicRowProcessor(new RateBeanProcessor()));
    public static final BeanListHandler<View> listHandler = new BeanListHandler<View>(View.class, new BasicRowProcessor(new RateBeanProcessor()));

    public View() {
        this.uuid = UUID.randomUUID().toString();
    }

    public void save() throws Exception {
        DBUtils.executeSQL("REPLACE INTO view (uuid, strategy) VALUES (?,?)", this.uuid, this.strategy);
    }

    public void destroy() throws Exception {
        // Delete benchmarks
        List<Benchmark> benchmarks = DBUtils.executeSQL(Benchmark.listHandler,
                "SELECT * FROM benchmark WHERE view_uuid=?", this.uuid);
        for (Benchmark benchmark : benchmarks)
            benchmark.destroy();

        Connection conn = DBUtils.getConnection();
        QueryRunner runner = new QueryRunner();

        runner.update(conn, "DELETE FROM view_sample where view_uuid=?", this.uuid);
        runner.update(conn, "DELETE FROM view where uuid=?", this.uuid);
        logger.info("view " + this.uuid + " deleted");

        conn.close();
    }

    // Output view's classes and samples to a string
    public String getExportString() {
        String str = "";
        List<Clazz> clazzs = this.getClazzs();

        for (Clazz clazz : clazzs) {
            str += "#" + clazz.getUuid() + "\n";
            for (Sample sample : getSamples(clazz)) {
                str += sample.getUuid() + "\n";
            }
        }
        return str;
    }
    // Get all clazzes in a view
    public List<Clazz> getClazzs() {
        List<Clazz> clazzs = DBUtils.executeSQL(Clazz.listHandler,
                "SELECT class_uuid as uuid, count(class_uuid) as sample_count from view_sample where view_uuid=? group by class_uuid", this.uuid);
        Collections.shuffle(clazzs);
        return clazzs;
    }
    // Get all samples in a view
    public List<Sample> getSamples(Clazz clazz) {
        List<Sample> samples = DBUtils.executeSQL(Sample.listHandler,
                "SELECT uuid,file FROM sample INNER JOIN view_sample as vs where " +
                        "vs.view_uuid=? and vs.class_uuid=? and vs.sample_uuid=sample.uuid and sample.classified='VALID' ORDER BY RAND()", this.uuid, clazz.getUuid());
        Collections.shuffle(samples);
        return samples;
    }
    // Get all clazz sample pairs in a view
    public List<Pair<Clazz, List<Sample>>> getClazzsSamples() {
        List<Clazz> clazzs = getClazzs();
        List<Pair<Clazz, List<Sample>>> clazzsSamples = new ArrayList<Pair<Clazz, List<Sample>>>();
        for (Clazz clazz : clazzs) {
            List<Sample> samples = getSamples(clazz);
            if (samples.isEmpty())
                continue;
            Pair<Clazz, List<Sample>> pair = new ImmutablePair<Clazz, List<Sample>>(clazz, samples);
            clazzsSamples.add(pair);
        }
        return clazzsSamples;
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        object.put("uuid", this.uuid);
        object.put("created", this.created.toString());
        object.put("strategy", this.strategy);
        object.put("class_count", this.numOfClasses);
        object.put("sample_count", this.numOfSamples);
        return object;
    }

    ///// Class Methods /////
    public static List<View> all() throws Exception {
        return DBUtils.executeSQL(listHandler, "SELECT * FROM VIEW");
    }
    public static View find(String uuid) {
        return DBUtils.executeSQL(handler, "SELECT * FROM view WHERE uuid=?", uuid);
    }
    // Generate a view according to strategy, output progress to std out
    public static View generate(BasicStrategy generateStrategy) throws Exception {
        return generate(generateStrategy, new PrintWriter(System.out));
    }
    // Generate a view according to strategy, output progress to @writer
    public static View generate(BasicStrategy generateStrategy, PrintWriter writer) throws Exception {
        View view = new View();
        Connection conn = DBUtils.getConnection();
        QueryRunner runner = DBUtils.getRunner();

        Statement statement = conn.createStatement();

        // Write progress
        writer.println("PROGRESS");
        writer.println("0.00");
        writer.flush();

        generateStrategy.prepare();
        view.setStrategy(generateStrategy.getStrategyName());

        view.save();

        long sampleSize = generateStrategy.getTotal();

        if (sampleSize == 0)
            throw new Exception("Generate strategy generate 0 samples");

        try {
            statement.execute("START TRANSACTION");

            int count = 0;

            // Fetch samples from view generate strategies
            while (!generateStrategy.isNoSamples()) {
                List<Sample> samples = generateStrategy.getNextSamples();
                for (Sample sample: samples) {
                    runner.update(conn, "INSERT INTO view_sample (view_uuid, sample_uuid, class_uuid) VALUES (?,?,?)",
                            view.getUuid(), sample.getUuid(), sample.getClassUuid());
                    count++;

                    if (count % 10 == 0) {
                        writer.println((double)count/sampleSize);
                        writer.flush();
                    }
                }
            }

            statement.execute("COMMIT");
            statement.close();
            logger.debug("commited");

            int numOfClasses = runner.query(conn, "SELECT COUNT(DISTINCT class_uuid) FROM view_sample WHERE view_uuid=?", new ScalarHandler<Long>(), view.getUuid()).intValue();
            int numOfSamples = runner.query(conn, "SELECT COUNT(*) FROM view_sample WHERE view_uuid=?", new ScalarHandler<Long>(), view.getUuid()).intValue();
            logger.debug("count info added");
            view.setNumOfClasses(numOfClasses);
            view.setNumOfSamples(numOfSamples);
            runner.update(conn, "UPDATE view SET num_of_classes=?, num_of_samples=? WHERE uuid=?", view.getNumOfClasses(),
                    view.getNumOfSamples(), view.getUuid());
        } catch (Exception e) {
            e.printStackTrace();
            view.destroy();
        } finally {
            conn.close();
            writer.println("DONE");
            writer.flush();
        }

        view = View.find(view.getUuid());

        System.out.println(view.getCreated());

        return view;
    }
}
