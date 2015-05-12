package com.rate.engine.clazz;

import com.rate.engine.RateBeanProcessor;
import com.rate.utils.DBUtils;
import lombok.Data;
import net.sf.json.JSONObject;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Created by Ran Xian on 3/13/14.
 */
@Data
public class Clazz {
    private String uuid;
    private String personUuid;
    private String type;
    private int subtype;
    private Timestamp created;
    private String importTag;
    private Long sampleCount; // This is just used for optimize benchmark generation
    public static final BeanHandler<Clazz> handler = new BeanHandler<Clazz>(Clazz.class, new BasicRowProcessor(new RateBeanProcessor()));
    public static final BeanListHandler<Clazz> listHandler = new BeanListHandler<Clazz>(Clazz.class, new BasicRowProcessor(new RateBeanProcessor()));

    public Clazz() {
        this.uuid = UUID.randomUUID().toString();
    }

    public void save() {
        DBUtils.executeSQL("REPLACE INTO class (uuid,person_uuid,type,subtype,import_tag) VALUES (?,?,?,?,?)",
                this.uuid, this.personUuid, this.type, this.subtype, this.importTag);
    }


    public static Clazz find(String uuid) throws Exception {
        return DBUtils.executeSQL(handler, "SELECT * FROM class WHERE uuid=?", uuid);
    }

    public static List<Clazz> findByImportTag(String importTag) {
        return DBUtils.executeSQL(listHandler, "SELECT * FROM view WHERE import_tag=?", importTag);
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
