package com.rate.server;

import com.rate.engine.exception.InvalidArgumentException;
import com.rate.engine.view.strategy.*;
import com.rate.engine.view.strategy.FileStrategy;
import com.rate.engine.view.strategy.ImportTagStrategy;
import com.rate.engine.view.View;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * Created by Ran Xian on 3/14/14.
 */
public class ViewUtils {
    private static final Logger logger = Logger.getLogger(ViewUtils.class.getName());

    public static View create(HashMap<String, String> args) throws Exception {
        PrintWriter writer = new PrintWriter(System.out);
        View view = create(args, writer);
        writer.close();
        return view;
    }

    public static View create(HashMap<String, String> args, PrintWriter writer) throws Exception {
        String strategyType = args.get("strategy");

        if (strategyType == null)
            throw new InvalidArgumentException("Not enough arguments to create view");

        BasicStrategy strategy = null;
        View view = null;

        if (strategyType.equalsIgnoreCase("import_tag")) {
            String import_tag = args.get("import_tag");
            if (import_tag == null)
                throw new InvalidArgumentException("No import_tag given");
            strategy = new ImportTagStrategy(import_tag);
        }
        /*
        else if (strategyType.equalsIgnoreCase("finger_tag")) {
            String finger_tag = args.get("finger_tag");
            if (finger_tag == null)
                throw new InvalidArgumentException("No finger_tag given");
            strategy = new GenerateByFingerTagStrategy(finger_tag);
        } else if (strategyType.equalsIgnoreCase("gender_tag")) {
            String gender_tag = args.get("gender_tag");
            if (gender_tag == null)
                throw new InvalidArgumentException("No gender_tag given");
            strategy = new GenerateByGenderTagStrategy(gender_tag);
        }*/
        else if (strategyType.equals("file")) {
            String filePath = args.get("filePath");
            if (filePath == null)
                throw new InvalidArgumentException("No file to generate a view");
            strategy = new FileStrategy(new File(filePath));
        } else if (strategyType.equals("all")) {
            strategy = new AllStrategy();
        } else {
            throw new InvalidArgumentException("Strategy not implemented");
        }

        view = View.generate(strategy, writer);

        if (view != null) {
            logger.info("created view [" + view.getUuid() + "] ");
        }
        else
            logger.info("failed to create view");

        return view;
    }

    public static boolean delete(String uuid) throws Exception {
        View view = View.find(uuid);
        if (view != null)
            View.find(uuid).destroy();
        return true;
    }
}
