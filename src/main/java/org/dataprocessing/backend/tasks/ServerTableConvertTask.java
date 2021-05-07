package org.dataprocessing.backend.tasks;

import com.google.common.collect.Iterables;
import com.sun.rowset.internal.Row;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dataprocessing.backend.database.SqlServer;
import org.dataprocessing.utils.CustomExecutors;
import org.dataprocessing.utils.Utils;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Converts a SQL query table into a List&lt;List&lt;String&gt;&gt; Table
 *
 * @author Nicholas Curl
 */
public class ServerTableConvertTask extends Task<List<List<?>>> {

    /**
     * The instance of the logger
     */
    private static final Logger    logger = LogManager.getLogger();
    /**
     * The instance of the Utils class
     */
    private static final Utils     utils  = Utils.getInstance();
    /**
     * The instance of the SqlServer class
     */
    private static final SqlServer server = SqlServer.getInstance();
    /**
     * The string containing the SQL Query
     */
    private final        String    sql;

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
    protected List<List<?>> call() throws Exception {
        List<List<?>> table = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<Double> localProgress = new AtomicReference<>(0.0);
        updateProgress(localProgress.get(), 1.0);
        if (utils.isBlankString(sql)) {
            updateProgress(1.0, 1.0);
            return table;
        }
        if (!isCancelled()) {
            if (!server.isClosed()) {
                ResultSet resultSet = server.queryServer(sql);
                RowSetFactory factory = RowSetProvider.newFactory();
                CachedRowSet rowSet = factory.createCachedRowSet();
                rowSet.populate(resultSet);
                List<?> list = new ArrayList<>(rowSet.toCollection());
                double localProgressUpdate = 1.0 / rowSet.size();
                ResultSetMetaData metaData = rowSet.getMetaData();
                List<String> header = new ArrayList<>();
                int colCount = metaData.getColumnCount();
                for (int i = 1; i <= colCount; i++) {
                    if (isCancelled()) {
                        break;
                    }
                    header.add(metaData.getColumnName(i));
                    utils.sleep(1);
                }
                localProgress.updateAndGet(v -> v + localProgressUpdate);
                updateProgress(localProgress.get(), 1.0);
                table.add(header);
                Iterable<? extends List<?>> partition = Iterables.partition(list, 10);
                ExecutorService threadPoolExecutor = CustomExecutors.newFixedThreadPool(20);
                for (List<?> objects : partition) {
                    if (isCancelled()) {
                        break;
                    }
                    threadPoolExecutor.submit(() -> {
                        for (Object object : objects) {
                            if (isCancelled()) {
                                break;
                            }
                            if (object instanceof Row) {
                                Row row = (Row) object;
                                ArrayList<Object> listRow = new ArrayList<>(Arrays.asList(row.getOrigRow()));
                                table.add(listRow);
                            }
                            localProgress.updateAndGet(v -> v + localProgressUpdate);
                            updateProgress(localProgress.get(), 1.0);
                            utils.sleep(1);
                        }
                        return null;
                    });
                }
                utils.shutdownExecutor(threadPoolExecutor, logger);
            }
        }
        return new ArrayList<>(table);
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        updateProgress(1.0, 1.0);
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
