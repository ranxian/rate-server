package com.rate.utils;

/**
 * Created by Ran Xian on 3/13/14.
 */
public class BXXUtils {
    private static int base = 93;

    public static String parse(int num) {
        String res = "";
        while (num!=0) {
            int mod = num % base;
            num /= base;
            res += (char)('!'+mod);
        }
        return res;
    }

    public static int get(String code) {
        int res = 0;
        char[] cs = code.toCharArray();

        for (int i = cs.length-1; i >= 0; i--) {
            char c = cs[i];
            res = res * 93 + c - '!';
        }

        return res;
    }
}
