package org.dataprocessing.backend.mappers.custom;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dataprocessing.backend.database.SqlServer;
import org.dataprocessing.backend.tasks.ServerTableConvertTask;
import org.dataprocessing.utils.FileUtils;
import org.dataprocessing.utils.MapperUtils;
import org.dataprocessing.utils.Utils;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Maps the POR Open Purchase Order Template
 *
 * @author Nicholas Curl
 */
public class VendorList {

    /**
     * The instance of the logger
     */
    private static final Logger                 logger    = LogManager.getLogger(VendorList.class);
    /**
     * The instance of the FileUtils class
     */
    private static final FileUtils              fileUtils = FileUtils.getInstance();
    /**
     * The template mapping task
     */
    private final        MapTemplate            mapTemplate;
    /**
     * The server table convert task
     */
    private final        ServerTableConvertTask tableConvertTask;
    /**
     * The excel writing task
     */
    private final        FileUtils.XlsxTask     writeTask;
    /**
     * The list of sub-tasks
     */
    private final        List<Task<?>>          tasks;
    /**
     * The total progress of this task
     */
    private final        DoubleBinding          totalProgress;

    /**
     * The constructor for this class
     *
     * @param storeLocation The path to the directory to store the mapped data
     */
    public VendorList(Path storeLocation) {
        tasks = new LinkedList<>();
        mapTemplate = new MapTemplate();
        tableConvertTask = new ServerTableConvertTask(
                "SELECT PO.PONumber,\n" +
                "       PO.Store,\n" +
                "       PO.Date,\n" +
                "       PO.Notes,\n" +
                "       I.[KEY],\n" +
                "       POD.ItemName,\n" +
                "       POD.Comments,\n" +
                "       POD.PriceEach,\n" +
                "       POD.QuantityOrdered,\n" +
                "       VF.Terms,\n" +
                "       VF.Address1,\n" +
                "       VF.Address2,\n" +
                "       VF.CityState,\n" +
                "       VF.Zip,\n" +
                "       VF.Contact1,\n" +
                "       VF.Phone,\n" +
                "       VF.Contact1Email,\n" +
                "       VF.Fax,\n" +
                "       I.CurrentStore,\n" +
                "       ID.DepartmentName,\n" +
                "       PO.VendorNumber,\n" +
                "       VF.VendorName\n" +
                "FROM PurchaseOrder PO\n" +
                "         INNER JOIN PurchaseOrderDetail POD on PO.PONumber = POD.PONumber\n" +
                "         LEFT JOIN VendorFile VF on PO.VendorNumber = VF.VendorNumber\n" +
                "         LEFT JOIN ItemFile I on POD.ItemNumber = I.NUM\n" +
                "         LEFT JOIN ItemDepartment ID on I.Department = ID.Department\n" +
                "WHERE PO.Status LIKE 'O%'"
        );
        writeTask = fileUtils.writeXlsxTask(storeLocation.resolve("Vendor List.xlsx"));
        tasks.add(tableConvertTask);
        tasks.add(mapTemplate);
        tasks.add(writeTask);
        totalProgress = Bindings.createDoubleBinding(() -> (
                                                                   Math.max(0, tableConvertTask.getProgress()) +
                                                                   Math.max(0, mapTemplate.getProgress()) +
                                                                   Math.max(0, writeTask.getProgress())
                                                           ) / 3,
                                                     tableConvertTask.progressProperty(),
                                                     mapTemplate.progressProperty(),
                                                     writeTask.progressProperty()
        );
    }

    /**
     * Gets the list of sub-tasks
     *
     * @return The list of sub-tasks
     */
    public List<Task<?>> getTasks() {
        return tasks;
    }

    /**
     * Gets the value of the total progress
     *
     * @return The total progress value
     */
    public double getTotalProgress() {
        return totalProgress.get();
    }

    /**
     * Maps the template
     *
     * @param executorService The controller thread executor
     */
    public void map(ExecutorService executorService) {
        tableConvertTask.setOnSucceeded(event -> {
            mapTemplate.setData(fileUtils.convertToTableString(tableConvertTask.getValue()));
            executorService.submit(mapTemplate);
        });
        mapTemplate.setOnSucceeded(event -> {
            writeTask.setTable(mapTemplate.getValue());
            executorService.submit(writeTask);
        });
        executorService.submit(tableConvertTask);
    }

    /**
     * Gets the total progress property
     *
     * @return The total progress property
     */
    public DoubleBinding totalProgressProperty() {
        return totalProgress;
    }

    /**
     * Maps the data to the Open Purchase Order Template
     */
    private static class MapTemplate extends Task<List<List<String>>> {

        /**
         * The instance of the logger
         */
        private static final Logger             logger      = LogManager.getLogger();
        /**
         * The instance of the Utils class
         */
        private static final Utils              utils       = Utils.getInstance();
        /**
         * The instance of the MapperUtils class
         */
        private static final MapperUtils        mapperUtils = MapperUtils.getInstance();
        /**
         * The instance of SqlServer Class
         */
        private static final SqlServer          server      = SqlServer.getInstance();
        /**
         * The template associated with this mapping
         */
        private static final String             template    = "/templates/Open Purchase Order Template_MFG FINAL.xlsx";
        /**
         * The header of the template
         */
        private final        List<String>       header;
        /**
         * The table that stores the mapped data
         */
        private final        List<List<String>> mapTable;
        /**
         * Local copy of the data to map
         */
        private              List<List<String>> data;

        /**
         * The constructor for this inner class
         */
        private MapTemplate() {
            header = mapperUtils.getHeader(template);
            mapTable = mapperUtils.createMapTable(template);
        }

        /**
         * Maps the Open Purchase Order template
         *
         * @return The table of the mapped data
         *
         * @throws Exception Any exception that might occur when executing this task
         */
        @Override
        protected List<List<String>> call() throws Exception {
            double progress = 0.0;
            updateProgress(0, 1.0);
            double progressUpdate = 1.0 / (data.size() - 1) / header.size();
            loopBreak:
            for (int i = 1; i < data.size(); i++) {
                if (isCancelled()) {
                    break;
                }
                List<String> row = data.get(i);
                List<String> mapRow = new LinkedList<>();
                rowBreak:
                for (int j = 0; j < header.size(); j++) {
                    if (isCancelled()) {
                        break loopBreak;
                    }
                    switch (j) {
                        case 0:
                        case 8:
                            mapRow.add(j, row.get(0).trim() + "#");
                            break;
                        case 1:
                            if (i != 1) {
                                try {
                                    int numCur = Integer.parseInt(row.get(20).trim());
                                    int numPrev = Integer.parseInt(data.get(i - 1).get(20).trim());
                                    if (numCur == numPrev) {
                                        mapRow.clear();
                                        progress -= progressUpdate;
                                        progress += progressUpdate * header.size();
                                        updateProgress(progress, 1.0);
                                        break rowBreak;
                                    }
                                }
                                catch (NumberFormatException ignored) {
                                }
                            }
                            mapRow.add(j, row.get(20).trim() + "#");
                            break;
                        case 2:
                            mapRow.add(j, row.get(21).trim());
                            break;
                        case 5:
                        case 26:
                            mapRow.add(j, "TRUE");
                            break;
                        case 14:
                            mapRow.add(j, "1#");
                            break;
                        case 27:
                        case 41:
                        case 42:
                        case 44:
                            mapRow.add(j, "FALSE");
                            break;
                        case 7:
                            mapRow.add(j, row.get(2).trim());
                            break;
                        case 9:
                            if (i != 1) {
                                if (row.get(0).trim().equalsIgnoreCase(data.get(i - 1).get(0).trim())) {
                                    mapRow.add(j, "");
                                }
                                else {
                                    mapRow.add(j, row.get(3).trim() + "^");
                                }
                            }
                            else {
                                mapRow.add(j, row.get(3).trim() + "^");
                            }
                            break;
                        case 12:
                            String location = row.get(1).trim();
                            if (location.equalsIgnoreCase("003")) {
                                mapRow.add(j, "Houston Depot");
                            }
                            else {
                                mapRow.add(j, "Memphis");
                            }
                            break;
                        case 15:
                            mapRow.add(j, row.get(4).trim());
                            break;
                        case 16:
                            mapRow.add(j, row.get(8).trim());
                            break;
                        case 19:
                            mapRow.add(j, row.get(7).trim());
                            break;
                        case 20:
                            double qty = 0;
                            double perUnit = 0;
                            try {
                                qty = Double.parseDouble(row.get(8).trim());
                                perUnit = Double.parseDouble(row.get(7).trim());
                            }
                            catch (NumberFormatException ignored) {
                            }
                            double amount = qty * perUnit;
                            mapRow.add(j, amount + "$");
                            break;
                        case 21:
                            mapRow.add(j, row.get(5).trim());
                            break;
                        case 22:
                            mapRow.add(j, row.get(19).trim());
                            break;
                        case 24:
                            String currentStore = row.get(18).trim();
                            if (currentStore.equalsIgnoreCase("003")) {
                                mapRow.add(j, "Houston Depot");
                            }
                            else {
                                mapRow.add(j, "Memphis");
                            }
                            break;
                        case 30:
                            mapRow.add(j, row.get(9).trim());
                            break;
                        case 32:
                            mapRow.add(j, row.get(14).trim());
                            break;
                        case 34:
                            mapRow.add(j, row.get(10).trim());
                            break;
                        case 35:
                            mapRow.add(j, row.get(11).trim());
                            break;
                        case 36:
                            String[] split = row.get(12).split(",");
                            mapRow.add(j, split[0].trim());
                            mapRow.add(j + 1, split[1].trim());
                            break;
                        case 37:
                            break;
                        case 38:
                            mapRow.add(j, row.get(13).trim());
                            break;
                        case 40:
                            mapRow.add(j, row.get(15).trim() + "@");
                            break;
                        case 43:
                            mapRow.add(j, row.get(16).trim());
                            break;
                        case 45:
                            mapRow.add(j, row.get(17).trim() + "@");
                            break;
                        case 47:
                            mapRow.add(j, row.get(6).trim());
                            break;
                        default:
                            mapRow.add(j, "");
                            break;
                    }
                    progress += progressUpdate;
                    updateProgress(progress, 1.0);
                }
                if (!mapRow.isEmpty()) {
                    mapTable.add(mapRow);
                }
                utils.sleep(1);
            }
            return mapTable;
        }

        /**
         * Logs the exception when the task transitions to the failure state
         */
        @Override
        protected void failed() {
            logger.fatal("Mapping Failed", getException());
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
    }
}
    