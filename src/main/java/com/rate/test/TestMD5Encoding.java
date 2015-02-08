package com.rate.test;

import com.rate.utils.EncodeUtils;

/**
 * Created by Ran Xian on 4/3/14.
 */
public class TestMD5Encoding {
    public static void main(String args[]) {
        String password = "12345";
        System.out.println(EncodeUtils.encodeByMD5(password));
    }
}
