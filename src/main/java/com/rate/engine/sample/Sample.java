package com.rate.engine.sample;

import com.rate.engine.RateBeanProcessor;
import com.rate.utils.DBUtils;
import lombok.Data;
import lombok.ToString;
import net.sf.json.JSONObject;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.sql.*;
import java.util.List;

/**
 * Created by Ran Xian on 3/11/14.
 */

@Data
@ToString
public class Sample {
    private String uuid;
    private String classUuid;
    private String personUuid;
    private String deviceUuid;
    private Timestamp created;
    private String file;
    private String importTag;
    private String classified;
    private String md5;

    public static final BeanHandler<Sample> handler = new BeanHandler<Sample>(Sample.class, new BasicRowProcessor(new RateBeanProcessor()));
    public static final BeanListHandler<Sample> listHandler = new BeanListHandler<Sample>(Sample.class, new BasicRowProcessor(new RateBeanProcessor()));

    public static List<Sample> findByImportTag(String importTag) {
        return DBUtils.executeSQL(listHandler, "SELECT * FROM sample WHERE import_tag=? and classified=?", importTag, "VALID");
    }

    public static List<Sample> findByFingerTag(String fingerTag)
    {
        return DBUtils.executeSQL(listHandler, "SELECT * FROM tag WHERE fingerTag=? and classified='VALID'", fingerTag);
    }

    public static List<Sample> findByGenderTag(String genderTag)
    {
        return DBUtils.executeSQL(listHandler, "SELECT * FROM tag WHERE genderTag=? and classified='VALID'", genderTag);
    }

    public static Sample find(String uuid) {
        return DBUtils.executeSQL(handler, "SELECT * FROM sample WHERE uuid=? and classified=?", uuid, "VALID");
    }

    public JSONObject toJSON() {
        JSONObject object = JSONObject.fromObject(this);

        object.put("created", this.created.toString());
        object.put("person_uuid", this.personUuid);
        object.put("import_tag", this.importTag);

        object.remove("personUuid");
        object.remove("importTag");
        return object;
    }
}
