package org.dataprocessing.backend.tasks;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.dataprocessing.backend.database.SqlServer;
import org.dataprocessing.backend.objects.Subassembly;
import org.dataprocessing.backend.objects.Subassembly.AssemblyItem;
import org.dataprocessing.utils.Utils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author Nicholas Curl
 */
public class KitMapper {

    /**
     * The instance of the logger
     */
    private static final Logger                 logger = LogManager.getLogger(KitMapper.class);
    private static final Utils                  utils  = Utils.getInstance();
    private final static SqlServer              server = SqlServer.getInstance();
    /**
     * The server table convert task
     */
    private final        ServerTableConvertTask tableConvertTask;
    /**
     * The list of sub-tasks
     */
    private final        List<Task<?>>          tasks;
    /**
     * The total progress of this task
     */
    private final        DoubleBinding          totalProgress;

    private final KitMapping kitMapping;

    public KitMapper() {
        this.tasks = new ArrayList<>();
        this.tableConvertTask = new ServerTableConvertTask(
                "SELECT Itemfile.[KEY],\n" +
                "       ItemFile.Name,\n" +
                "       ItemKitsAuto.Num,\n" +
                "       Quantity,\n" +
                "       ItemKey,\n" +
                "       MiscName\n" +
                "FROM ItemKitsAuto\n" +
                "         LEFT OUTER JOIN ItemFile ON ItemFile.NUM = ItemKitsAuto.Num\n" +
                "WHERE NOT ItemKey = ''\n" +
                "  AND ItemKey NOT LIKE '.%'\n" +
                "  AND NOT ItemFile.[KEY] = 'kit'"
        );
        this.kitMapping = new KitMapping();
        tasks.add(tableConvertTask);
        tasks.add(kitMapping);
        totalProgress = Bindings.createDoubleBinding(() -> (Math.max(0, tableConvertTask.getProgress()) +
                                                            Math.max(0, kitMapping.getProgress())
                                                           ) / 2,
                                                     tableConvertTask.progressProperty(),
                                                     kitMapping.progressProperty()
        );
    }

    public List<Task<?>> getTasks() {
        return tasks;
    }

    public double getTotalProgress() {
        return totalProgress.get();
    }

    public void map(ExecutorService executorService) {
        tableConvertTask.setOnSucceeded(event -> {
            kitMapping.setData(utils.convertToTableString(tableConvertTask.getValue()));
            executorService.submit(kitMapping);
        });
        executorService.submit(tableConvertTask);
    }

    @Override
    public String toString() {
        return "KitMapper{" +
               "tableConvertTask=" + tableConvertTask +
               ", tasks=" + tasks +
               ", totalProgress=" + totalProgress +
               ", mapper=" + kitMapping +
               '}';
    }

    public DoubleBinding totalProgressProperty() {
        return totalProgress;
    }

    public static class KitMapping extends Task<Map<String, Subassembly>> {

        private List<List<String>> data;

        /**
         * Invoked when the Task is executed, the call method must be overridden and
         * implemented by subclasses. The call method actually performs the
         * background thread logic. Only the updateProgress, updateMessage, updateValue and
         * updateTitle methods of Task may be called from code within this method.
         * Any other interaction with the Task from the background thread will result
         * in runtime exceptions.
         *
         * @return The result of the background work, if any.
         *
         * @throws Exception an unhandled exception which occurred during the
         *                   background operation
         */
        @Override
        protected Map<String, Subassembly> call() throws Exception {
            Map<String, Subassembly> subassemblies = new HashMap<>();
            double progress = 0.0;
            updateProgress(0, 1.0);
            ResultSet resultSet = server.queryServer("Select count(*)\n" +
                                                     "from (select distinct ItemKitsAuto.Num\n" +
                                                     "      from ItemKitsAuto\n" +
                                                     "               LEFT OUTER JOIN ItemFile ON ItemFile.NUM = ItemKitsAuto.Num\n" +
                                                     "      WHERE NOT ItemKey = ''\n" +
                                                     "        AND ItemKey NOT LIKE '.%'\n" +
                                                     "        AND NOT ItemFile.[KEY] = 'kit') as IKAN");
            resultSet.next();
            int kitCount = resultSet.getInt(1);
            double progressUpdate = 1.0 / (data.size() + kitCount);
            for (List<String> row : data) {
                if (data.indexOf(row) == 0) {
                    continue;
                }
                if (isCancelled()) {
                    break;
                }
                String assemblyKey = row.get(0);
                int qty = 0;
                try {
                    qty = (int) Double.parseDouble(row.get(3));
                }
                catch (NumberFormatException ignored) {
                }
                AssemblyItem item = new AssemblyItem(row.get(4),
                                                     qty,
                                                     !Strings.isBlank(row.get(5)) ? row.get(5) : "",
                                                     (row.get(4).toLowerCase().contains(":") ||
                                                      row.get(4).toLowerCase().contains("kit")
                                                     )
                );
                if (subassemblies.containsKey(assemblyKey)) {
                    Subassembly subassembly = subassemblies.get(assemblyKey);
                    subassembly.addItem(item);
                }
                else {
                    Subassembly subassembly = new Subassembly(assemblyKey, row.get(1), item);
                    subassemblies.put(assemblyKey, subassembly);
                }
                progress += progressUpdate;
                updateProgress(progress, 1.0);
                utils.sleep(1);
            }
            for (Subassembly subassembly : subassemblies.values()) {
                subassembly.sortAssemblyItems();
                progress += progressUpdate;
                updateProgress(progress, 1.0);
                utils.sleep(1);
            }
            return subassemblies;
        }

        /**
         * Logs the exception when the task transitions to the failure state
         */
        @Override
        protected void failed() {
            logger.fatal("Kit Mapping failed", getException());
            System.exit(-1);
        }

        /**
         * Sets the data to map
         *
         * @param data The data to map
         */
        public void setData(List<List<String>> data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "KitMapping{" +
                   "data=" + data +
                   '}';
        }
    }
}
