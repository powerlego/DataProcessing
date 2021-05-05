package org.dataprocessing.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A utility class for converting table types
 *
 * @author Nicholas Curl
 */
public class Converters {

    /**
     * The instance of the logger
     */
    private static final Logger logger = LogManager.getLogger();

    /**
     * Converts List&lt;String[]&gt; Table into List&lt;List&lt;String&gt;&gt; Table
     *
     * @param table The table to convert
     *
     * @return The converted table
     */
    public static List<List<String>> convertTableArrayToTableString(List<String[]> table) {
        List<List<String>> tableNew = new ArrayList<>();
        for (String[] rowArray : table) {
            List<String> row = new ArrayList<>(Arrays.asList(rowArray.clone()));
            tableNew.add(row);
        }
        return tableNew;
    }

    public static List<Object[]> convertTableToTableArray(List<List<?>> table) {
        List<Object[]> tableNew = new ArrayList<>();

        for (List<?> row : table) {
            Object[] rowArray = row.toArray(new Object[0]);
            tableNew.add(rowArray);
        }
        return tableNew;
    }

    /**
     * Converts List&lt;List&lt;String&gt;&gt; Table into List&lt;String[]&gt; Table
     *
     * @param table The table to convert
     *
     * @return The converted table
     */
    public static List<String[]> convertTableToTableArrayString(List<List<String>> table) {
        List<String[]> tableNew = new ArrayList<>();

        for (List<String> row : table) {
            String[] rowArray = row.toArray(new String[0]);
            tableNew.add(rowArray);
        }
        return tableNew;
    }
}
