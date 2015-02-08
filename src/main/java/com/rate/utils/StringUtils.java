package com.rate.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Ran Xian on 4/3/14.
 */
public class StringUtils {
    public static HashMap<String, String> parseArgs(String[] args) {
        HashMap<String, String> map = new HashMap<String, String>();
        for (String arg : args) {
            String[] sp = arg.split(":");
            if (sp.length == 2)
                map.put(sp[0], sp[1]);
        }
        return map;
    }

    public static String readline(BufferedInputStream inputStream) {
        byte[] bbuf = new byte[1];
        String line = "";
        try {
            while (true) {
                if (inputStream.read(bbuf) < 0)
                    return null;
                char c = (char)bbuf[0];
                if (c == '\n')
                    break;
                line += c;
            }
        } catch (IOException e) {
            return null;
        }

        return line;
    }
}
