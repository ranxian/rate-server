package com.rate.engine.task.analyzer;

import com.rate.engine.task.Task;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.*;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Ran Xian on 3/18/14.
 */

// Basic class for all analyzers. Analysers use raw results of running a task (similarity scores) and
// calculate useful metrics such as FMR, FNMR.
// All Analyzer should implement setTask and analyze methods.
abstract public class BasicAnalyzer {
    protected HashMap<String, String> uuidTable = new HashMap<String, String>();
    protected HashMap<String, String> enrollMap = new HashMap<String, String>();
    protected Logger logger = Logger.getLogger("Analyzer");

    protected int getCountOfLines(String filePath) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        int count = 0;
        while ((reader.readLine())!=null) {
            count++;
        }
        return count;
    }

    // Put results of file into X and Y axis
    protected XYSeries getXYSeries(String xySeriesName, String filePath) {
        double x, y;
        XYSeries xySeries = new XYSeries(xySeriesName);
        List<String> lines = null;
        try {
            lines = FileUtils.readLines(new File(filePath));
        } catch (IOException e) {
            logger.fatal("can't read data file");
        }

        if (lines == null) {
            logger.debug("Task result file is empty");
        } else {
            for (String line: lines) {
                line = StringUtils.strip(line);
                String rs[] = line.split(" ");
                x = Double.parseDouble(rs[0]);
                y = Double.parseDouble(rs[1]);
                xySeries.add(y, x);
            }
        }
        return xySeries;
    }

    // Write a JFreeChart chart to @path as a PNG file
    protected void writePNG(JFreeChart chart, String path) {
        try {
            OutputStream outputStream = new FileOutputStream(path);
            ChartUtilities.writeChartAsPNG(outputStream, chart, 420, 300);
        } catch (IOException e) {
            logger.fatal("can't open file " + path + " for write");
            e.printStackTrace();
        }
    }

    ///// Chart generation /////
    // Generate charts and write to file as PNGs

    protected void addDistribution(XYSeriesCollection xySeriesCollection,
                                 String distributionName, String filePath, double interval) {

        XYSeries xySeries = new XYSeries(distributionName);
        List<String> lines = null;
        try {
            lines = FileUtils.readLines(new File(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (lines == null) {
            logger.debug("Distribution file is empty");
            return;
        }

        double top = interval;
        int count = 0;
        int totalCount = lines.size();
        for (String line: lines) {
            line = StringUtils.strip(line);
            String rs[] = line.split(" ");
            double score = Double.parseDouble(rs[rs.length-1]);

            while (score > top) {
                xySeries.add(top - interval/2, (double)count/totalCount);
                count = 0;
                top += interval;
            }
            count++;
        }
        xySeries.add(top - interval/2, (double)count/totalCount);

        xySeriesCollection.addSeries(xySeries);
    }

    public abstract void setTask(Task task);

    public abstract void analyze() throws Exception;
}
