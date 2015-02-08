package com.rate.test;

import com.rate.utils.ZipUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Created by Ran Xian on 4/3/14.
 */
public class TestZip {
    public static void main(String args[]) throws Exception {
        String path = "/Users/Ran Xian/RATE_ROOT/my_alg";
        String zipfile = ZipUtils.zip(path);
        System.out.println(zipfile);
        ZipUtils.unzip(zipfile, "/Users/Ran Xian/RATE_ROOT/test", null);

        FileUtils.forceDelete(new File(zipfile));
    }
}
