package com.rate.engine.task.result;

import com.rate.engine.task.Task;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.sf.json.JSONObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Ran Xian on 3/18/14.
 */
// TaskResult for GeneralBenchmark
@Data
@EqualsAndHashCode(callSuper = false)
public class GeneralResult extends BasicResult {
    private final Logger logger = Logger.getLogger(GeneralResult.class);
    private String errorRateFilePath;
    private String rocFilePath;
    private String genuineFilePath;
    private String imposterFilePath;
    private String fmrFilePath;
    private String fnmrFilePath;
    private String badResultDir;
    private String genuineResultPath;
    private String imposterResultPath;
    private String revImposterPath;
    private String fmrFnmrImagePath;
    private String rocImagePath;
    private String scoreImagePath;

    private double EER;
    private double EER_l;
    private double EER_h;
    private double FMR100;
    private double FMR1000;
    private double zeroFMR;
    private double zeroFNMR;

    // Prepare file paths
    public GeneralResult(Task task) {
        super(task);

        errorRateFilePath = FilenameUtils.concat(task.getDirPath(), "rate.txt");
        rocFilePath = FilenameUtils.concat(task.getDirPath(), "roc.txt");
        genuineFilePath = FilenameUtils.concat(task.getDirPath(), "genuine.txt");
        imposterFilePath = FilenameUtils.concat(task.getDirPath(), "imposter.txt");
        fnmrFilePath = FilenameUtils.concat(task.getDirPath(), "fnmr.txt");
        fmrFilePath = FilenameUtils.concat(task.getDirPath(), "fmr.txt");
        badResultDir = FilenameUtils.concat(task.getDirPath(), "bad-result");
        genuineResultPath = FilenameUtils.concat(badResultDir, "genuine");
        imposterResultPath = FilenameUtils.concat(badResultDir, "imposter");
        fmrFnmrImagePath = FilenameUtils.concat(task.getDirPath(), "fmrFnmr.png");
        rocImagePath = FilenameUtils.concat(task.getDirPath(), "roc.png");
        scoreImagePath = FilenameUtils.concat(task.getDirPath(), "score.png");

        revImposterPath = FilenameUtils.concat(task.getDirPath(), "revImp.txt");
        if (task.getFinished() != null) {
            getErrorRates();
        }
    }
    // Read error rates from rate.txt
    private void getErrorRates() {
        try {
            BufferedReader errorRateReader = new BufferedReader(new FileReader(errorRateFilePath));
            String line = StringUtils.strip(errorRateReader.readLine());
            String rs[] = line.split(" ");
            EER = Double.parseDouble(rs[0]);
            EER_l = Double.parseDouble(rs[1]);
            EER_h = Double.parseDouble(rs[2]);
            line = StringUtils.strip(errorRateReader.readLine());
            FMR100 = Double.parseDouble(line);
            line = StringUtils.strip(errorRateReader.readLine());
            FMR1000 = Double.parseDouble(line);
            line = StringUtils.strip(errorRateReader.readLine());
            zeroFMR = Double.parseDouble(line);
            line = StringUtils.strip(errorRateReader.readLine());
            zeroFNMR = Double.parseDouble(line);
            errorRateReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();

        object.put("EER", this.EER);
        object.put("EER_L", this.EER_l);
        object.put("EER_H", this.EER_h);
        object.put("FMR100", this.FMR100);
        object.put("FMR1000", this.FMR1000);
        object.put("zeroFMR", this.zeroFMR);
        object.put("zeroFNMR", this.zeroFNMR);

        return object;
    }
}
