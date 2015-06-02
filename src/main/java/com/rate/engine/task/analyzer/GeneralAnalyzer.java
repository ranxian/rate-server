package com.rate.engine.task.analyzer;

import com.rate.engine.task.result.GeneralResult;
import com.rate.engine.task.Task;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.Title;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Created by Ran Xian on 3/18/14.
 */
// GeneralAnalyzer outputs metrics like FMR, FNMR, EER, ROC, score distributions etc.
@Data
@EqualsAndHashCode(callSuper = false)
public class GeneralAnalyzer extends BasicAnalyzer {
    private static final double E = 1e-15;
    private static final Logger logger = Logger.getLogger(GeneralAnalyzer.class);
    @Getter
    @Setter
    private GeneralResult taskResult;
    private Task task;
    private int FTE = 0;
    private int FTM = 0;

    @Override
    public void setTask(Task task) {
        taskResult = new GeneralResult(task);
        this.task = task;
    }
    @Override
    public void analyze() throws Exception {
        logger.trace("begin analyze");
        prepare();
        logger.trace("prepare finished");
        logger.trace("analyzeFMR");
        analyzeFMR(taskResult.getImposterFilePath(), taskResult.getFmrFilePath());
        logger.trace("done");
        logger.trace("analyzeFNMR");
        analyzeFNMR(taskResult.getGenuineFilePath(), taskResult.getFnmrFilePath());
        logger.trace("done");
        logger.trace("analyzeErrorRates");
        analyzeErrorRates();
        logger.trace("done");
        logger.trace("analyzeFTEFTM");
        analyzeFTEFTM();
        logger.trace("done");
        logger.trace("analyzeROC");
        analyzeROC(taskResult.getFmrFilePath(), taskResult.getFnmrFilePath(), taskResult.getRocFilePath());
        logger.trace("done");
        logger.trace("generateImage");
        generateImage();
        logger.trace("done");
    }

    private void analyzeFTEFTM() throws Exception {
        JSONObject taskState = this.task.getTaskState();
        this.FTE = (Integer)taskState.get("enroll_failed");
        this.FTM = (Integer)taskState.get("match_failed");
    }

    // Calculate FMR100, FMR1000, zeroFMR, zeroFNMR and EER
    private void analyzeErrorRates() throws Exception {
        double FMR100 = findFNMRonFMR(0.01);
        double FMR1000 = findFNMRonFMR(0.001);
        double zeroFMR = findFNMRonFMR(0);
        double zeroFNMR = findFMRonFNMR(0);
        String EERline = analyzeEER(taskResult.getFmrFilePath(), taskResult.getFnmrFilePath());
        String[] args = EERline.split(" ");
        double EER = Double.parseDouble(args[0]);
        double EER_l = Double.parseDouble(args[1]);
        double EER_h = Double.parseDouble(args[2]);
        logger.trace(String.format("FMR100 %f", FMR100));
        logger.trace(String.format("FMR1000 %f", FMR1000));
        logger.trace(String.format("zeroFMR %f", zeroFMR));
        logger.trace(String.format("zeroFNMR %f", zeroFNMR));
        logger.trace(String.format("EER_l %f", EER_l));
        logger.trace(String.format("EER_h %f", EER_h));

        this.taskResult.getTask().setScore(EER);

        PrintWriter errorRatePw = new PrintWriter(new File(taskResult.getErrorRateFilePath()));
        errorRatePw.println(String.format("%f %f %f", EER, EER_l, EER_h));
        errorRatePw.println(FMR100);
        errorRatePw.println(FMR1000);
        errorRatePw.println(zeroFMR);
        errorRatePw.println(zeroFNMR);
        errorRatePw.println(FTE);
        errorRatePw.println(FTM);
        errorRatePw.close();
    }

    // Read similarity scores of imposter attempts, write threshold-FMR pair to @outputPath
    protected void analyzeFMR(String imposterResultPath, String outputFilePath) throws Exception {
        logger.debug("analyze fmr " + outputFilePath);
        BufferedReader imposterReader = new BufferedReader(new FileReader(imposterResultPath));
        File fmrFile = new File(outputFilePath);
        fmrFile.createNewFile();
        PrintWriter fmrPw = new PrintWriter(fmrFile);

        int countOfLines = getCountOfLines(imposterResultPath);
        int i=0;
        double p=-1, matchScore=0;
        fmrPw.println("0 1");
        while (true) {
            String line = imposterReader.readLine();
            if (line==null) break;

            line = StringUtils.strip(line);
            String rs[] = line.split(" ");
            matchScore = Double.parseDouble(rs[rs.length-1]);

            if (matchScore>p) {
                if (p!=-1)
                    fmrPw.println(String.format("%f %f", p, 1 - (double)i/countOfLines));
                p = matchScore;
            }
            i++;
        }
        fmrPw.println(String.format("%f 0", matchScore));
        fmrPw.println("1 0");
        fmrPw.close();
    }

    // Read similarity scores of genuine attempts, write threshold-FNMR pair to @outputFilePath
    protected void analyzeFNMR(String genuinResultPath, String outputFilePath) throws Exception {
        BufferedReader genuineReader = new BufferedReader(new FileReader(genuinResultPath));
        File fnmrFile = new File(outputFilePath);
        fnmrFile.createNewFile();
        PrintWriter fmrPw = new PrintWriter(fnmrFile);

        int countOfLines = getCountOfLines(genuinResultPath);
        int i=0;
        double p=-1, matchScore=0;
        fmrPw.println("0 0");
        while (true) {
            String line = genuineReader.readLine();
            if (line==null) break;

            line = org.apache.commons.lang3.StringUtils.strip(line);
            String rs[] = line.split(" ");
            matchScore = Double.parseDouble(rs[rs.length-1]);

            if (matchScore>p) {
                if (p!=-1)
                    fmrPw.println(String.format("%f %f", p, (double)i/countOfLines));
                p = matchScore;
            }
            i++;
        }
        fmrPw.println(String.format("%f 1", matchScore));
        fmrPw.println("1 1");
        fmrPw.close();
    }

    // Read FMR and FNMR file and calculate "EER-l EER-h EER"
    protected String analyzeEER(String fmrFilePath, String fnmrFilePath) throws Exception {
        Scanner fIn1 = new Scanner(new FileInputStream(fmrFilePath));
        Scanner fIn2 = new Scanner(new FileInputStream(fnmrFilePath));

        double t1 = 0.0, fmr1 = 1.0, fnmr1 = 0.0;
        double t2 = 0.0, fmr2 = 1.0, fnmr2 = 0.0;
        double t_fmr = 0.0;
        double t_fnmr = 0.0;

        if (fIn1.hasNext()) {
            t_fmr = fIn1.nextDouble();
        } else {
            t_fmr = 1.0;
        }

        if (fIn2.hasNext()) {
            t_fnmr = fIn2.nextDouble();
        } else {
            t_fnmr = 1.0;
        }

        while (fmr2 > fnmr2) {
            t1 = t2;
            fmr1 = fmr2;
            fnmr1 = fnmr2;

            if (t_fmr < t_fnmr) {
                t2 = t_fmr;

                if (t2 != 1.0) {
                    fmr2 = fIn1.nextDouble();
                } else {
                    fmr2 = 0.0;
                }

                if (fIn1.hasNext()) {
                    t_fmr = fIn1.nextDouble();
                } else {
                    t_fmr = 1.0;
                }

            } else if (t_fmr > t_fnmr) {
                t2 = t_fnmr;

                if (t2 != 1.0) {
                    fnmr2 = fIn2.nextDouble();
                } else {
                    fnmr2 = 1.0;
                }

                if (fIn2.hasNext()) {
                    t_fnmr = fIn2.nextDouble();
                } else {
                    t_fnmr = 1.0;
                }

            } else {
                t2 = t_fmr;

                if (t2 != 1.0) {
                    fmr2 = fIn1.nextDouble();
                } else {
                    fmr2 = 0.0;
                }

                if (fIn1.hasNext()) {
                    t_fmr = fIn1.nextDouble();
                } else {
                    t_fmr = 1.0;
                }

                if (t2 != 1.0) {
                    fnmr2 = fIn2.nextDouble();
                } else {
                    fnmr2 = 1.0;
                }

                if (fIn2.hasNext()) {
                    t_fnmr = fIn2.nextDouble();
                } else {
                    t_fnmr = 1.0;
                }
            }
        }

        fIn1.close();
        fIn2.close();

        double EER_l, EER_h, EER;
        if (fmr1 + fnmr1 < fmr2 + fnmr2) {
            EER_l = fnmr1;
            EER_h = fmr1;
        } else {
            EER_l = fmr2;
            EER_h = fnmr2;
        }
        EER = (EER_l+EER_h)/2;
        return EER + " " + EER_l + " " + EER_h;
    }

    // Read FMR and FNMR and output roc file
    protected void analyzeROC(String fmrFilePath, String FnmrFilePath, String rocFilePath) throws Exception {
        Scanner fIn1 = new Scanner(new FileInputStream(fmrFilePath));
        Scanner fIn2 = new Scanner(new FileInputStream(FnmrFilePath));

        PrintWriter fOut = new PrintWriter(
                new FileWriter(new File(rocFilePath)));

        double t1 = 0.0, r1 = 1.0;
        double t2 = 0.0, r2 = 1.0;
        double t3 = 0.0, r3 = 0.0;
        double t4 = 0.0, r4 = 0.0;
        double fmr, fnmr;

        while (t2 < 1.0 || t4 < 1.0) {

            if (t2 < t4) {
                if (fIn1.hasNext()) {
                    t1 = t2;
                    r1 = r2;
                    t2 = fIn1.nextDouble();
                    r2 = fIn1.nextDouble();

                } else if (t2 < 1.0) {
                    t1 = t2;
                    r1 = r2;
                    t2 = 1.0;
                    r2 = 0.0;
                }

            } else if (t2 > t4) {

                if (fIn2.hasNext()) {
                    t3 = t4;
                    r3 = r4;
                    t4 = fIn2.nextDouble();
                    r4 = fIn2.nextDouble();
                } else if (t4 < 1.0) {
                    t3 = t4;
                    r3 = r4;
                    t4 = 1.0;
                    r4 = 1.0;
                }
            }

            if (t2 == t4) {

                if (fIn1.hasNext()) {
                    t1 = t2;
                    r1 = r2;
                    t2 = fIn1.nextDouble();
                    r2 = fIn1.nextDouble();
                } else if (t2 < 1.0) {
                    t1 = t2;
                    r1 = r2;
                    t2 = 1.0;
                    r2 = 0.0;
                }
                if (t2 == 0.0) {
                    r1 = r2;
                    continue;
                }

                if (fIn2.hasNext()) {
                    t3 = t4;
                    r3 = r4;
                    t4 = fIn2.nextDouble();
                    r4 = fIn2.nextDouble();
                } else if (t4 < 1.0) {
                    t3 = t4;
                    r3 = r4;
                    t4 = 1.0;
                    r4 = 1.0;
                }
                if (t4 == 0.0) {
                    r3 = r4;
                    continue;
                }

                fmr = r1;
                fnmr = r3;
                fOut.println(fmr + " " + fnmr);
            }

            if (t2 < t4) {
                fmr = r2;
                fnmr = (t2 - t3) * (r4 - r3) / (t4 - t3) + r3;
            } else {
                fmr = (t4 - t1) * (r2 - r1) / (t2 - t1) + r1;
                fnmr = r4;
            }

            fOut.println(fmr + " " + fnmr);
        }

        fOut.close();
        fIn2.close();
        fIn1.close();
    }

    // Calculate FMR when FNMR is @fnmr
    private double findFMRonFNMR(double fnmr) throws Exception {
        BufferedReader fnmrReader = new BufferedReader(new FileReader(taskResult.getFnmrFilePath()));
        String line;
        double threshold = 0, errorRate = 0;
        while ((line=fnmrReader.readLine())!=null) {
            String rs[] = StringUtils.strip(line).split(" ");
            errorRate = Double.parseDouble(rs[1]);
            threshold = Double.parseDouble(rs[0]);

            if (errorRate > fnmr) break;
        }
        if (errorRate<fnmr) {
            threshold = 1;
        }

        fnmrReader.close();

        return getFMRonThreshold(threshold);
    }
    // Calculate FNMR when FMR is @fmr
    private double findFNMRonFMR(double fmr) throws Exception {
        BufferedReader fmrReader = new BufferedReader(new FileReader(taskResult.getFmrFilePath()));
        String line;
        double threshold = 0, errorRate = 0, prev_threshold = 0;
        while ((line=fmrReader.readLine())!=null) {
            String rs[] = StringUtils.strip(line).split(" ");
            errorRate = Double.parseDouble(rs[1]);
            threshold = Double.parseDouble(rs[0]);

            if (errorRate <= fmr) break;
            prev_threshold = threshold;
        }
        threshold = prev_threshold;

        if (errorRate > fmr) {
            threshold = 1;
        }

        fmrReader.close();

        return getFNMRonThreshold(threshold);
    }
    // Get FMR at threshold @thresholdIn
    private double getFMRonThreshold(double thresholdIn) throws Exception {
        return getErrorRateOnThreshold(thresholdIn, taskResult.getFmrFilePath());
    }
    // Get FNMR at threshold @thresholdIn
    private double getFNMRonThreshold(double thresholdIn) throws Exception {
        return getErrorRateOnThreshold(thresholdIn, taskResult.getFnmrFilePath());
    }

    private double getErrorRateOnThreshold(double thresholdIn, String filePath) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        double threshold = 0, errorRate = 0;
        while ((line=reader.readLine())!=null) {
            String rs[] = StringUtils.strip(line).split(" ");
            threshold = Double.parseDouble(rs[0]);
            errorRate = Double.parseDouble(rs[1]);

            if (threshold>=thresholdIn) break;
        }
        reader.close();
        return errorRate;
    }

    ///// Draw charts and write to files /////
    public void generateImage() {
        generateFmrFnmrImage();
        generateRocImage();
        generateScoreImage();
    }
    private void generateFmrFnmrImage() {
        JFreeChart chart;
        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();

        XYSeries xySeriesFMR = getXYSeries("FMR", taskResult.getFmrFilePath());
        xySeriesCollection.addSeries(xySeriesFMR);
        XYSeries xySeriesFNMR = getXYSeries("FNMR", taskResult.getFnmrFilePath());
        xySeriesCollection.addSeries(xySeriesFNMR);

        chart = ChartFactory.createXYLineChart(
                "", // title
                "Ratio", // x
                "Threshold", // y
                xySeriesCollection,
                PlotOrientation.HORIZONTAL, true,
                true,
                false
        );


        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setForegroundAlpha(0.6f);
        plot.getDomainAxis().setRange(new Range(0.0, 1.0));
        plot.getRangeAxis().setRange(0.0, 1.0);
        chart.setBackgroundPaint(Color.WHITE);

        Title t = chart.getSubtitle(0);
        t.setBorder(0, 0, 0, 0);
        writePNG(chart, taskResult.getFmrFnmrImagePath());
    }
    private void generateRocImage() {
        JFreeChart chart;
        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();

        addROCCurve(xySeriesCollection, taskResult.getRocFilePath(), "ROC Curve", true);

        String labelX = "FMR", labelY = "FNMR";
        chart = ChartFactory.createXYLineChart(
                "", // title
                labelX, // x
                labelY, // y
                xySeriesCollection,
                PlotOrientation.VERTICAL, true,
                true,
                false
        );

        chart.setBackgroundPaint(Color.WHITE);

        XYPlot plot = (XYPlot) chart.getPlot();

        LogarithmicAxis xAxis = new LogarithmicAxis(labelX);
        xAxis.setRange(new Range(0.00001, 1));
        xAxis.setLog10TickLabelsFlag(true);
        plot.setDomainAxis(xAxis);

        LogarithmicAxis yAxis = new LogarithmicAxis(labelY);
        yAxis.setRange(new Range(0.001, 1));
        yAxis.setLog10TickLabelsFlag(true);
        plot.setRangeAxis(yAxis);

        Title t = chart.getSubtitle(0);
        t.setBorder(0, 0, 0, 0);

        writePNG(chart, taskResult.getRocImagePath());
    }
    private void addROCCurve(XYSeriesCollection xySeriesCollection, String filePath, String xySeriesName, boolean isDET) {
        List<String> lines = null;
        try {
            lines = FileUtils.readLines(new File(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        XYSeries xySeries = new XYSeries(xySeriesName);

        if (lines == null) {
            logger.debug("ROC file is empty");
            return;
        }

        for (String line: lines) {
            line = StringUtils.strip(line);
            String rs[] = line.split(" ");
            double fmr = Double.parseDouble(rs[0]);
            double fnmr = Double.parseDouble(rs[1]);

            if(fmr < E) fmr = E;
            if(fnmr < E) fnmr = E;
            if(fmr == 1.0) fmr = 1.0 - E;
            if(fnmr == 1.0) fnmr = 1.0 - E;

            if (isDET) xySeries.add(fmr, fnmr);
            else xySeries.add(fmr, 1 - fnmr);
        }

        xySeriesCollection.addSeries(xySeries);

    }
    private void generateScoreImage() {
        JFreeChart chart;
        int column = 60;
        double interval = 1.00/column;
        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        xySeriesCollection.setIntervalWidth(interval);

        addDistribution(xySeriesCollection, "Genuine", taskResult.getGenuineFilePath(), interval);
        addDistribution(xySeriesCollection, "Imposter", taskResult.getImposterFilePath(), interval);

        chart = ChartFactory.createHistogram(
                "", // title
                "Score", // x label
                "Percentage", // y label
                xySeriesCollection,
                PlotOrientation.VERTICAL, true,
                false,
                false
        );

        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setForegroundAlpha(0.6f);
        plot.getDomainAxis().setRange(new Range(0.0, 1.0));
        plot.getRangeAxis().setRange(0.0, 1.0);

        Title t = chart.getSubtitle(0);
        t.setBorder(0, 0, 0, 0);

        writePNG(chart, taskResult.getScoreImagePath());
    }

    private void prepare() throws Exception {
        File genuineResultPath = new File(taskResult.getGenuineResultPath());
        File imposterResultPath = new File(taskResult.getImposterResultPath());

        FileUtils.forceMkdir(genuineResultPath);
        FileUtils.forceMkdir(imposterResultPath);

        File resultFile = new File(taskResult.getMatchResultFilePath());
        resultFile.createNewFile();

        // prepare uuid table
        try {
            BufferedReader reader = new BufferedReader(new FileReader(taskResult.getTask().getBenchmark().getUuidTableFilePath()));
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                String[] sp = line.split(" ");
                uuidTable.put(sp[0], sp[1]);
                uuidTable.put(sp[1], sp[2]);
            }
            reader.close();
        } catch (IOException e) {
            logger.fatal("Can't open uuid table file");
        }
    }
}
