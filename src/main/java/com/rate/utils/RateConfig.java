package com.rate.utils;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

/**
 * Created by Ran Xian on 3/13/14.
 */
public class RateConfig {
    private static final Logger logger = Logger.getLogger(RateConfig.class);

    private static final Configuration config = buildConfig();

    private static PropertiesConfiguration buildConfig() {
        try {
            return new PropertiesConfiguration("rate.properties");
        }
        catch (ConfigurationException ex) {
            logger.error(ex);
            return null;
        }
    }
    public static String getBinDir() {
        return FilenameUtils.concat(getRootDir(), "bin");
    }

    public static String getRootDir() {
        return config.getString("RATE_ROOT");
    }

    public static String getSampleRootDir() {
        return FilenameUtils.concat(getRootDir(), "samples");
    }

    public static String getBenchmarkRootDir() {
        return FilenameUtils.concat(getRootDir(), "benchmarks");
    }

    public static String getAlgorithmRootDir() {
        return FilenameUtils.concat(getRootDir(), "algorithms");
    }

    public static String getTempRootDir() {
        return FilenameUtils.concat(getRootDir(), "temp");
    }

    public static String getTaskRootDir() {
        return FilenameUtils.concat(getRootDir(), "tasks");
    }

    public static String getZipRootDir() {
        return FilenameUtils.concat(getRootDir(), "zips");
    }

    public static Boolean isDistRun() {
        return config.getString("DIST_RUN").equals("1");
    }

    public static String getDistEngineDir() {
        return FilenameUtils.concat(getBinDir(), "engine");
    }

    public static String getDistEnginePath() {
        return FilenameUtils.concat(getDistEngineDir(), "engine_run.py");
    }

    public static String getDistEngineServer() {
        return config.getString("DIST_SERVER");
    }

    public static String getPythonPath() {
        return config.getString("PYTHON_PATH");
    }

    public static Integer getServerPort() {
        return Integer.parseInt(config.getString("SERVER_PORT"));
    }

    public static String getDBUrl() {
        return config.getString("DBUrl");
    }

    public static String getDBUser() {
        return config.getString("DBUser");
    }

    public static String getDBPass() {
        return config.getString("DBPass");
    }

    public static Integer getFetchLimit() {
        return Integer.parseInt(config.getString("FETCH_LIMIT"));
    }
}
