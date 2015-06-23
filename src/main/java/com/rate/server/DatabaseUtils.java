package com.rate.server;

import com.rate.engine.database.ZipImporter;
import com.rate.engine.exception.InvalidArgumentException;

import java.io.PrintWriter;
import java.util.HashMap;

/**
 * Created by xianran on 6/23/15.
 */
public class DatabaseUtils {
    public static void zipImport(HashMap<String, String> args, PrintWriter writer) throws Exception {
        String importTag = args.get("import_tag");
        String zipPath = args.get("zipPath");
        if (zipPath == null || importTag == null) {
            throw new InvalidArgumentException("Not enough argument");
        }
        ZipImporter zipImporter = new ZipImporter(zipPath, importTag, writer);
        zipImporter.zipImport();
    }
}
