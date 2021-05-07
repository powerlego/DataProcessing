package org.dataprocessing.backend.tasks;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes duplicate entries from a specified data table
 *
 * @author Nicholas Curl
 */
public class RemoveDuplicates extends Task<List<List<String>>> {

    /**
     * The instance of the logger
     */
    private static final Logger logger = LogManager.getLogger();
    /**
     * The new table without the duplicate data
     */
    private final List<List<String>> newTable;
    /**
     * The list containing the duplicated data
     */
    private final List<List<List<String>>> duplicatesList;
    /**
     * The progress of finding the duplicate data
     */
    private final DoubleProperty findProgress;
    /**
     * The progress of removing the duplicated data
     */
    private final DoubleProperty removeProgress;
    /**
     * The local copy of the table to remove the duplicated data from
     */
    private List<List<String>> table;

    /**
     * The constructor for this sub-task
     */
    public RemoveDuplicates() {
        newTable = new ArrayList<>();
        duplicatesList = new ArrayList<>();
        findProgress = new SimpleDoubleProperty(0.0);
        removeProgress = new SimpleDoubleProperty(0.0);
    }

    /**
     * Finds and removes the duplicate data in the given table
     *
     * @return The new table without duplicate data
     *
     * @throws Exception Any exception that might occur when executing this task
     */
    @Override
    protected List<List<String>> call() throws Exception {
        findDuplicates();
        removeDuplicates();
        return newTable;
    }

    /**
     * Finds the duplicate data
     */
    private void findDuplicates() {
        double progress = 0.0;
        List<List<String>> tempTable = new ArrayList<>(table);
        newTable.add(tempTable.get(0));
        tempTable.remove(0);
        double progressUpdate = 1.0 / tempTable.size();
        loopBreak:
        while (!tempTable.isEmpty()) {
            if (isCancelled()) {
                break;
            }
            List<String> row = tempTable.get(0);
            List<List<String>> duplicates = new ArrayList<>();
            String key = row.get(1);
            for (List<String> searchRow : tempTable) {
                if (isCancelled()) {
                    break loopBreak;
                }
                String searchName = searchRow.get(1);
                if (searchName.equalsIgnoreCase(key)) {
                    duplicates.add(searchRow);
                    progress += progressUpdate;
                    findProgress.set(progress);
                    updateTotalProgress();
                }
            }
            tempTable.removeAll(duplicates);
            duplicatesList.add(duplicates);
        }
    }

    /**
     * Removes the duplicate data
     */
    private void removeDuplicates() {
        double progress = 0.0;
        double progressUpdate = 1.0 / duplicatesList.size();
        loopBreak:
        for (List<List<String>> duplicates : duplicatesList) {
            if (isCancelled()) {
                break;
            }
            if (duplicates.size() == 1) {
                newTable.addAll(duplicates);
                progress += progressUpdate;
                removeProgress.set(progress);
                updateTotalProgress();
                continue;
            }
            if (duplicates.get(1).get(1).toLowerCase().contains("berry")) {
                newTable.addAll(duplicates);
                progress += progressUpdate;
                removeProgress.set(progress);
                updateTotalProgress();
                continue;
            }
            duplicateBreak:
            while (!duplicates.isEmpty()) {
                if (isCancelled()) {
                    break loopBreak;
                }
                List<List<String>> duplicatesNew = new ArrayList<>();
                String name = duplicates.get(0).get(1);
                for (List<String> entry : duplicates) {
                    if (isCancelled()) {
                        break loopBreak;
                    }
                    if (entry.get(1).equalsIgnoreCase(name)) {
                        duplicatesNew.add(entry);
                    }
                }
                duplicates.removeAll(duplicatesNew);
                if (duplicatesNew.size() == 1) {
                    newTable.addAll(duplicatesNew);
                    continue;
                }
                if (duplicatesNew.get(0).get(1).equalsIgnoreCase("Rush Charge")) {
                    for (List<String> entry : duplicatesNew) {
                        if (isCancelled()) {
                            break loopBreak;
                        }
                        if (entry.get(0).equalsIgnoreCase("|ACC-900R")) {
                            newTable.add(entry);
                            break duplicateBreak;
                        }
                    }
                }
                if (duplicatesNew.size() == 2) {
                    if (duplicatesNew.get(0).get(0).contains("HD") || duplicatesNew.get(1).get(0).contains("HD")) {
                        newTable.addAll(duplicatesNew);
                    } else {
                        newTable.add(duplicatesNew.get(0));/*
                        if (duplicatesNew.get(1).get(0).contains(duplicatesNew.get(0).get(0))) {

                        } else {
                            newTable.addAll(duplicatesNew);
                        }*/
                    }
                    continue;
                }
                newTable.add(duplicatesNew.get(0));
            }
            progress += progressUpdate;
            removeProgress.set(progress);
            updateTotalProgress();
        }
    }

    /**
     * Updates the total progress of this sub-task
     */
    private void updateTotalProgress() {
        updateProgress((Math.max(0, findProgress.get())
                + Math.max(0, removeProgress.get())
        ) / 2, 1.0);
    }

    /**
     * Logs the exception when the task transitions to the failure state
     */
    @Override
    protected void failed() {
        logger.fatal("RemoveDuplicates Task failed", getException());
        System.exit(-1);
    }

    /**
     * Sets the local copy of the table to remove the duplicated data from
     *
     * @param table The table to remove the duplicated data from
     */
    public void setTable(List<List<String>> table) {
        this.table = table;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        updateProgress(1.0, 1.0);
    }
}
