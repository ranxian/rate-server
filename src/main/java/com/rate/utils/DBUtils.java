package com.rate.utils;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by Ran Xian on 3/12/14.
 */
public class DBUtils {
    static private Logger logger = Logger.getLogger(DBUtils.class);
    public static QueryRunner getRunner() {
        QueryRunner runner = null;
        try {
            runner = new QueryRunner();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return runner;
    }

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://" + RateConfig.getDBUrl(), RateConfig.getDBUser(),
                    RateConfig.getDBPass());
        } catch (Exception e) {
            System.out.println("can't obtain database connection");
        }
        return conn;
    }

    // Count number of rows in a table
    public static Long countTable(String target) throws Exception {
        Connection conn = DBUtils.getConnection();
        Long count = DBUtils.getRunner().query(conn, "select count(*) from "+target, new ScalarHandler<Long>());
        conn.close();
        return count;
    }

    public static Long count(String sql, java.lang.Object... params) {
        Connection conn = null;
        long count = 0;
        try {
            conn = getConnection();
            QueryRunner runner = new QueryRunner();
            count = runner.query(conn, sql, new ScalarHandler<Long>(), params);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    logger.error("Can't close connection");
                }
            }
        }

        return count;
    }

    public static void executeSQL(String sql, java.lang.Object... params) {
        Connection conn = null;
        try {
            conn = getConnection();
            getRunner().update(conn, sql, params);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    logger.error("Can't close connection");
                }
            }

        }
    }

    public static <T> T executeSQL(ResultSetHandler<T> rsh, String sql, Object... params) {
        Connection conn = null;
        T result = null;
        try {
            conn = getConnection();
            QueryRunner runner = getRunner();
            result = runner.query(conn, sql, rsh, params);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    logger.error("Can't close db connection");
                }
            }
        }

        return result;
    }

    public static Timestamp getCurrentTimestamp() {
        Date date = new Date();
        return new Timestamp(date.getTime());
    }
}
