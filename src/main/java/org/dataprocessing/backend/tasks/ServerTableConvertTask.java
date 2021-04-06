package org.dataprocessing.backend.tasks;

import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dataprocessing.backend.database.SqlServer;
import org.dataprocessing.utils.Utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Converts a SQL query table into a List&lt;List&lt;String&gt;&gt; Table
 *
 * @author Nicholas Curl
 */
public class ServerTableConvertTask extends Task<List<List<String>>> {
    /**
     * The instance of the logger
     */
    private static final Logger logger = LogManager.getLogger();
    /**
     * The instance of the Utils class
     */
    private static final Utils utils = Utils.getInstance();
    /**
     * The instance of the SqlServer class
     */
    private static final SqlServer server = SqlServer.getInstance();
    /**
     * The string containing the SQL Query
     */
    private final String sql;

    /**
     * The constructor for this class
     *
     * @param sql The string containing the SQL Query
     */
    public ServerTableConvertTask(String sql) {
        sql = sql.replaceAll("(?:[\n\r]|\\s{2,})", " ");
        this.sql = sql;
        this.updateProgress(0, 1.0);
    }

    /**
     * Converts the sql query into a data table
     *
     * @return The table containing the SQL query
     *
     * @throws Exception Any exception that might occur when executing this task
     */
    @Override
    protected List<List<String>> call() throws Exception {
        List<List<String>> table = new LinkedList<>();
        double localProgress = 0.0;
        updateProgress(localProgress, 1.0);
        if (utils.isBlankString(sql)) {
            updateProgress(1.0, 1.0);
            return table;
        }
        if (!isCancelled()) {
            if (!server.isClosed()) {
                ResultSet resultSet = server.queryServer(sql);
                double localProgressUpdate = server.getLocalProgressUpdate(sql);
                breakPoint:
                try {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    List<String> header = new LinkedList<>();
                    int colCount = metaData.getColumnCount();

                    for (int i = 1; i <= colCount; i++) {
                        if (isCancelled()) {
                            break breakPoint;
                        }
                        header.add(metaData.getColumnName(i));
                        localProgress += localProgressUpdate;
                        updateProgress(localProgress, 1.0);
                        utils.sleep(1);
                    }
                    if (!isCancelled()) {
                        table.add(header);
                        while (resultSet.next()) {
                            if (isCancelled()) {
                                break breakPoint;
                            }
                            LinkedList<String> row = new LinkedList<>();
                            for (int i = 1; i <= colCount; i++) {
                                if (isCancelled()) {
                                    break breakPoint;
                                }
                                String cell = resultSet.getString(i);
                                if (!utils.isBlankString(cell)) {
                                    row.add(cell.trim());
                                } else {
                                    row.add("");
                                }
                                if (isCancelled()) {
                                    break breakPoint;
                                }
                                localProgress += localProgressUpdate;
                                updateProgress(localProgress, 1.0);
                            }
                            if (isCancelled()) {
                                break breakPoint;
                            }
                            table.add(row);
                            utils.sleep(1);
                        }
                    }
                } catch (SQLException e) {
                    logger.fatal("Unable to convert server table.", e);
                    System.exit(-1);
                }
            }
        }
        return table;
    }

    /**
     * Logs the exception when the task transitions to the failure state
     */
    @Override
    protected void failed() {
        logger.fatal("Conversion Task failed", getException());
        System.exit(-1);
    }
}
