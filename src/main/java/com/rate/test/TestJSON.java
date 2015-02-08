package com.rate.test;

import com.rate.engine.benchmark.Benchmark;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ran Xian on 4/6/14.
 */
public class TestJSON {
    public static void main(String[] args) throws Exception {
        JSONObject jsonObject;
        Benchmark benchmark = Benchmark.find("291ec684-ab20-4936-b150-650abbeb4a49");

        jsonObject = JSONObject.fromObject(benchmark);

        System.out.println(jsonObject.toString());

        List<String> list = new ArrayList<String>();
        list.add("I");
        list.add("love");

        JSONArray jsonArray = JSONArray.fromObject(list);

        System.out.println(jsonArray.toString());
    }
}
