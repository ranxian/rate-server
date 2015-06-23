package com.rate.engine.database;

import com.rate.engine.clazz.Clazz;
import com.rate.engine.sample.Sample;
import com.rate.utils.DBUtils;
import com.rate.utils.RateConfig;
import net.lingala.zip4j.core.ZipFile;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;

/**
 * Created by xianran on 6/23/15.
 */
public class ZipImporter {
    private final Logger logger = Logger.getLogger(ZipImporter.class);
    private String zipPath;
    private String importTag;
    private PrintWriter progressWriter;

    public ZipImporter(String zipPath, String importTag, PrintWriter progressWriter) {
        this.zipPath = zipPath;
        this.importTag = importTag;
        this.progressWriter = progressWriter;
    }

    public void zipImport() throws Exception {
        logger.trace("Begin import " + zipPath + " with tag " + importTag);

        ZipFile zipFile = new ZipFile(new File(zipPath));
        String zipFileName = zipFile.getFile().getName();
        String zipFileBaseName = zipFileName.substring(0, zipFileName.length() - 4);
        if (!zipFile.isValidZipFile()) {
            throw new Exception("Zip file is not valid");
        }

        String destDir = RateConfig.getSampleRootDir() + "/" + zipFileBaseName;

        zipFile.extractAll(destDir);
        File sampleDir = new File(destDir);

        File[] clazzdirs = sampleDir.listFiles();
        if (clazzdirs == null) {
            FileUtils.forceDelete(sampleDir);
            throw new Exception("No class directory in .zip");
        }

        progressWriter.println("PROGRESS");
        progressWriter.println("0.00");

        int nSample = 0;
        int nProcessed = 0;

        // Calculate number of samples
        for (File clazzdir : clazzdirs) {
            File[] samples = clazzdir.listFiles();
            if (clazzdir.listFiles() != null) {
                nSample += samples.length;
            }
        }

        Connection conn = DBUtils.getConnection();
        QueryRunner runner = DBUtils.getRunner();
        runner.update(conn, "START TRANSACTION");

        for (File clazzdir : clazzdirs) {
            Clazz clazz = new Clazz();
            clazz.setImportTag(importTag);
            runner.update("INSERT INTO class (uuid,person_uuid,type,subtype,import_tag) VALUES (?,?,?,?,?)",
                    clazz.getUuid(), null, "FINGERVEIN", null, importTag);

            File[] sampleFiles = clazzdir.listFiles();

            if (sampleFiles == null) {
                logger.trace("No samples in some class");
                return;
            }
            for  (File sampleFile : sampleFiles) {
                String samplePath = zipFileBaseName + "/" + sampleFile.getParentFile().getName() + "/" +
                        sampleFile.getName();

                Sample sample = new Sample();
                sample.setFile(samplePath);
                sample.setImportTag(importTag);

                sample.setClassUuid(clazz.getUuid());
                runner.update("INSERT INTO sample (uuid, class_uuid, file, import_tag, classified) VALUES (?,?,?,?,?)",
                        sample.getUuid(), sample.getClassUuid(), sample.getFile(), sample.getImportTag(), "VALID");
                nProcessed += 1;
                if (nProcessed % 50 == 0) {
                    progressWriter.println((double)nProcessed / nSample);
                }
            }
        }
        runner.update("COMMIT");
        progressWriter.println("DONE");
        logger.trace("Import successfully");
    }

}