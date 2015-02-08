package com.rate.engine.view.strategy;

import com.rate.utils.DBUtils;
import com.rate.engine.sample.Sample;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Created by Ran Xian on 3/13/14.
 */
public class ImportTagStrategy extends BasicStrategy {
    @Setter @Getter private String importTag;

    @Override
    public void prepare() throws Exception {
        this.total = DBUtils.count("SELECT COUNT(*) FROM sample WHERE import_tag=? and classified='VALID'", this.importTag);
    }

    @Override
    public String getStrategyName() {
        return "ImportTagStrategy";
    }

    @Override
    public List<Sample> getNextSamples() {
        List<Sample> samples = DBUtils.executeSQL(Sample.listHandler, "SELECT uuid, class_uuid FROM sample WHERE import_tag=? and classified='VALID' LIMIT ?,?", this.importTag, skip, limit);
        skip += limit;
        return samples;
    }

    public ImportTagStrategy(String importTag) {
        super();
        this.importTag = importTag;
    }
}
