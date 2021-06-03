package org.dataprocessing.backend.tasks;

import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes Non-Alphanumeric characters from a specified data table
 *
 * @author Nicholas Curl
 */
public class RemoveNonAlphaNum extends Task<List<List<String>>> {

    /**
     * The instance of the logger
     */
    private static final Logger             logger = LogManager.getLogger(RemoveNonAlphaNum.class);
    /**
     * Local copy of the table to remove the non-alphanumeric characters from
     */
    private              List<List<String>> table;

    /**
     * Removes the non-alphanumeric characters from the table
     *
     * @return The table without the non-alphanumeric characters
     *
     * @throws Exception Any exception that might occur when executing this task
     */
    @Override
    protected List<List<String>> call() throws Exception {
        double progress = 0.0;
        double progressUpdate = 1.0 / table.size();
        updateProgress(progress, 1.0);
        for (List<String> row : table) {
            if (isCancelled()) {
                break;
            }
            String key = row.get(0);
            Pattern alphaNumPattern = Pattern.compile("[a-zA-Z0-9-#_]+");
            Matcher alphaNumMatcher = alphaNumPattern.matcher(key);
            if (alphaNumMatcher.find()) {
                String alphaNum = alphaNumMatcher.group();
                String replacement = alphaNum.replaceAll("[^a-zA-Z0-9-_]+", "_");
                row.set(0, replacement);
            }
            progress += progressUpdate;
            updateProgress(progress, 1.0);
        }
        updateProgress(1.0, 1.0);
        return table;
    }

    /**
     * Logs the exception when the task transitions to the failure state
     */
    @Override
    protected void failed() {
        logger.fatal("RemoveNonAlphaNum Task failed", getException());
        System.exit(-1);
    }

    /**
     * Sets the local copy of the table to remove the non-alphanumeric characters
     *
     * @param table The table to remove the non-alphanumeric characters
     */
    public void setTable(List<List<String>> table) {
        this.table = table;
    }
}
