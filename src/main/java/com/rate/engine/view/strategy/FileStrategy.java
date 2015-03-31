package com.rate.engine.view.strategy;

import com.rate.engine.sample.Sample;
import com.rate.utils.DBUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ran Xian on 4/26/14.
 */
public class FileStrategy extends BasicStrategy {
    private File file;
    BufferedReader reader;
    private Boolean opened;

    public FileStrategy(File file) {
        super();
        this.file = file;
        this.opened = false;
    }

    @Override
    public void prepare() throws Exception {
        long c = 0;
        String line;
        reader = new BufferedReader(new FileReader(file));
        while (true) {
            line = reader.readLine();
            if (line == null)
                break;
            if (line.startsWith("#"))
                continue;
            c++;
        }
        this.total = c;
        reader.close();
    }

    @Override
    public List<Sample> getNextSamples() throws Exception {
        List<Sample> samples = new ArrayList<Sample>();

        int added = 0;
        if (!opened) {
            reader = new BufferedReader(new FileReader(this.file));
            opened = true;
        }

        while (added <= limit) {
            String uuid = reader.readLine();
            added += 1;
            if (uuid == null) {
                break;
            }
            if (uuid.startsWith("#") || uuid.equals(""))
                continue;
            Sample sample = DBUtils.executeSQL(Sample.handler, "SELECT uuid, class_uuid FROM sample WHERE uuid=? and classified='VALID'", uuid);
            if (sample == null)
                throw new Exception("No sample with uuid " + uuid);
            samples.add(sample);
        }

        skip += added;
        if (skip >= total) {
            reader.close();
        }

        return samples;
    }

    @Override
    public String getStrategyName() {
        return "FileStrategy";
    }
}
