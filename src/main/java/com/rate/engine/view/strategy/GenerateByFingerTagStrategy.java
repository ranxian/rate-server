package com.rate.engine.view.strategy;

/**
 * Created by xuqiantong on 12/15/14.

public class GenerateByFingerTagStrategy extends BasicStrategy {
    @Setter @Getter private String fingerTag;

    public void prepare() throws Exception {
        this.total = DBUtils.count("SELECT COUNT(*) FROM tag WHERE fingerTag=? and classified='VALID'", this.fingerTag);
    }

    @Override
    public String getViewName() {
        if (this.viewName == null)
            return String.format("VIEW_BY_FINGER_TAG_%s", this.fingerTag);
        else {
            return this.viewName;
        }
    }

    @Override
    public String getGenerator() {
        return "GenerateByFingerTagGenerator";
    }

    @Override
    public List<Sample> getNextSamples() {
        List<Sample> samples = DBUtils.executeSQL(Sample.listHandler, "SELECT uuid, class_uuid FROM sample WHERE fingerTag=? and classified='VALID' LIMIT ?,?", this.fingerTag, skip, limit);
        skip += limit;
        return samples;
    }

    public ImportTagStrategy(String fingerTag) {
        this.fingerTag = fingerTag;
    }
}
*/
