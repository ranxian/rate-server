package com.rate.test;

import com.rate.utils.BXXUtils;

/**
 * Created by Ran Xian on 5/22/14.
 */
public class TestBXX {
    static public void main(String[] args) {
        String encoded = "Lx+";
        String code = BXXUtils.parse(12345);
        System.out.println(BXXUtils.get(code));
        System.out.println(BXXUtils.get(encoded));
    }
}
