package com.rate.engine.view.strategy;

/**
 * Created by xuqiantong on 12/13/14.

public class GenerateByGenderTagStrategy extends BasicStrategy {
    @Setter @Getter private String genderTag;

    public void prepare() throws Exception {
        //建议不要做这个，这样的跨表查询大约需要2小时
        this.total = DBUtils.count("SELECT count(*) FROM 
                                    (
                                        select ID from tag where type = 'person' 
                                        and tag = 'gender' 
                                        and Content_string = 'FEMALE' 
                                    )t, sample
                                    WHERE sample.person_uuid=t.ID;", 
                                    this.genderTag);

    }

    @Override
    public String getViewName() {
        if (this.viewName == null)
            return String.format("VIEW_BY_GENDER_TAG_%s", this.genderTag);
        else {
            return this.viewName;
        }
    }

    @Override
    public String getGenerator() {
        return "GenerateByGenderTagGenerator";
    }

    @Override
    public List<Sample> getNextSamples() {
        List<Sample> samples = DBUtils.executeSQL(Sample.listHandler, 
                                                "SELECT sample.uuid, sample.file FROM 
                                                (
                                                    select ID from tag where type = 'person' 
                                                    and tag = 'gender' 
                                                    and Content_string = 'FEMALE' 
                                                )t, sample
                                                WHERE sample.person_uuid=t.ID;", 
                                                this.genderTag, skip, limit);
        skip += limit;
        return samples;
    }

//这段sql语句效率会很高，但对count无效
/*
create table tmp(id blob);
insert into tmp
select ID from tag 
where type = 'person' and tag = 'gender' and Content_string = 'FEMALE'
;

SELECT sample.uuid, sample.file FROM 
sample, tmp
WHERE sample.person_uuid=tmp.ID 
;
drop table tmp;



    public ImportTagStrategy(String genderTag) {
        this.genderTag = genderTag;
    }
}
*/