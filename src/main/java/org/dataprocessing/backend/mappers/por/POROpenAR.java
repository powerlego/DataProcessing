package org.dataprocessing.backend.mappers.por;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dataprocessing.backend.tasks.ServerTableConvertTask;
import org.dataprocessing.utils.FileUtils;
import org.dataprocessing.utils.MapperUtils;
import org.dataprocessing.utils.Utils;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps the POR Open AR Template
 *
 * @author Nicholas Curl
 */
public class POROpenAR {

    /**
     * The instance of the logger
     */
    private static final Logger logger = LogManager.getLogger();
    /**
     * The instance of the FileUtils class
     */
    private static final FileUtils fileUtils = FileUtils.getInstance();
    private final        Utils utils = Utils.getInstance();
    /**
     * The template mapping task
     */
    private final        MapTemplate mapTemplate;
    /**
     * The server table convert task
     */
    private final ServerTableConvertTask tableConvertTask;
    /**
     * The first excel writing task
     */
    private final FileUtils.XlsxTask writeTask1;
    /**
     * The second excel writing task
     */
    private final FileUtils.XlsxTask writeTask2;
    /**
     * The third excel writing task
     */
    private final FileUtils.XlsxTask writeTask3;
    /**
     * The list of sub-tasks
     */
    private final List<Task<?>> tasks;
    /**
     * The total progress of this task
     */
    private final DoubleBinding totalProgress;

    /**
     * The constructor for this class
     *
     * @param storeLocation The path to the directory to store the mapped data
     */
    public POROpenAR(Path storeLocation) {
        tasks = new ArrayList<>();
        tableConvertTask = new ServerTableConvertTask(
                "SELECT DISTINCT CustomerFile.NAME        as Customer_Name,\n" +
                "                Transactions.STAT        as Transaction_Status,\n" +
                "                Transactions.CNTR        as Transaction_Contract,\n" +
                "                Transactions.CLDT        as Transaction_Close_Date,\n" +
                "                Salesman_Cntr.Name       as Contract_Sales_Rep_Name,\n" +
                "                TransactionType.TypeName as Transaction_Type,\n" +
                "                Salesman_customer.Name   as Customer_Sales_Rep_Name,\n" +
                "                Salesman_jobsite.Name    as Jobsite_Sales_Rep_Name,\n" +
                "                CustomerFile.Terms,\n" +
                        "                Transactions.STR\n" +
                        "FROM Transactions\n" +
                        "         LEFT OUTER JOIN CustomerFile ON Transactions.CUSN = CustomerFile.CNUM\n" +
                        "         LEFT OUTER JOIN Salesman Salesman_Cntr ON Transactions.Salesman = Salesman_Cntr.Number\n" +
                        "         LEFT OUTER JOIN CustomerJobSite ON Transactions.JobSite = CustomerJobSite.Number\n" +
                        "         LEFT OUTER JOIN TransactionType ON Transactions.TransactionType = TransactionType.TypeNumber\n" +
                        "         LEFT OUTER JOIN TransactionOperation ON Transactions.Operation = TransactionOperation.OperationNumber\n" +
                        "         LEFT OUTER JOIN ParameterFile ON Transactions.STR = ParameterFile.Store\n" +
                        "         LEFT OUTER JOIN Salesman Salesman_jobsite ON CustomerJobSite.Salesman = Salesman_jobsite.Number\n" +
                        "         LEFT OUTER JOIN CustomerType ON CustomerFile.Type = CustomerType.Type\n" +
                        "         LEFT OUTER JOIN Salesman Salesman_customer ON CustomerFile.Salesman = Salesman_customer.Number\n" +
                        "WHERE (Transactions.TOTL <> Transactions.PAID OR Transactions.DEPP <> 0)\n" +
                        "  AND Transactions.Archived = 0\n" +
                        "  AND Transactions.PYMT <> N'T'\n" +
                        "  AND Transactions.STAT NOT LIKE 'R%'\n" +
                        "  AND Transactions.STAT NOT LIKE 'Q%'\n" +
                        "  AND Transactions.STAT NOT LIKE 'O%'"
        );
        mapTemplate = new POROpenAR.MapTemplate();
        writeTask1 = fileUtils.writeXlsxTask(storeLocation.resolve("Open AR Template-Mahaffey Tent & Awning.xlsx"));
        writeTask2 = fileUtils.writeXlsxTask(storeLocation.resolve("Open AR Template-Mahaffey USA.xlsx"));
        writeTask3 = fileUtils.writeXlsxTask(storeLocation.resolve("Open AR Template-Mahaffey USA-Houston.xlsx"));
        tasks.add(tableConvertTask);
        tasks.add(mapTemplate);
        tasks.add(writeTask1);
        tasks.add(writeTask2);
        tasks.add(writeTask3);
        totalProgress = Bindings.createDoubleBinding(() -> (
                        Math.max(0, tableConvertTask.getProgress()) +
                                Math.max(0, mapTemplate.getProgress()) +
                                Math.max(0, writeTask1.getProgress()) +
                                Math.max(0, writeTask2.getProgress()) +
                                Math.max(0, writeTask3.getProgress())
                ) / 5,
                tableConvertTask.progressProperty(),
                mapTemplate.progressProperty(),
                writeTask1.progressProperty(),
                writeTask2.progressProperty(),
                writeTask3.progressProperty()
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
            mapTemplate.setData(utils.convertToTableString(tableConvertTask.getValue()));
            executorService.submit(mapTemplate);
        });
        mapTemplate.setOnSucceeded(event -> {
            writeTask1.setTable(mapTemplate.getValue().get(0));
            writeTask2.setTable(mapTemplate.getValue().get(1));
            writeTask3.setTable(mapTemplate.getValue().get(2));
            executorService.submit(writeTask1);

        });
        writeTask1.setOnSucceeded(event -> {
            executorService.submit(writeTask2);
        });
        writeTask2.setOnSucceeded(event -> {
            executorService.submit(writeTask3);
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
     * Maps the data to the Open AR Template
     */
    private static class MapTemplate extends Task<List<List<List<String>>>> {
        /**
         * The instance of the Utils class
         */
        private static final Utils utils = Utils.getInstance();
        /**
         * The instance of the MapperUtils class
         */
        private static final MapperUtils mapperUtils = MapperUtils.getInstance();
        /**
         * The template associated to this mapping
         */
        private static final String template = "/templates/Open AR (Invoices) Template_MFG FINAL.xlsx";
        /**
         * The header of the template
         */
        private final List<String> header;
        /**
         * The table that stores the mapped data for store 1
         */
        private final List<List<String>> mapTable1;
        /**
         * The table that stores the mapped data for store 2
         */
        private final List<List<String>> mapTable2;
        /**
         * The table that stores the mapped data for store 3
         */
        private final List<List<String>> mapTable3;
        /**
         * The list of tables for every store listed in POR
         */
        private final List<List<List<String>>> tables;
        /**
         * The data to be mapped
         */
        private List<List<String>> data;

        /**
         * The constructor for this inner class
         */
        private MapTemplate() {
            header = mapperUtils.getHeader(template);
            mapTable1 = mapperUtils.createMapTable(template);
            mapTable2 = mapperUtils.createMapTable(template);
            mapTable3 = mapperUtils.createMapTable(template);
            tables = new ArrayList<>();
        }

        /**
         * Maps the Open AR template
         *
         * @return The table of the mapped data
         *
         * @throws Exception Any exception that might occur when executing this task
         */
        @Override
        protected List<List<List<String>>> call() throws Exception {
            double progress = 0.0;
            double progressUpdate = 1.0 / (data.size() - 1) / header.size();
            updateProgress(progress, 1.0);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date;
            String dateString;
            loopBreak:
            for (int i = 1; i < data.size(); i++) {
                if (isCancelled()) {
                    break;
                }
                List<String> row = data.get(i);
                List<String> mapRow = new ArrayList<>();
                for (int j = 0; j < header.size(); j++) {
                    if (isCancelled()) {
                        break loopBreak;
                    }
                    switch (j) {
                        case 0:
                        case 1:
                            mapRow.add(j, row.get(2).trim() + "@");
                            break;
                        case 2:
                            mapRow.add(j, row.get(0).trim());
                            break;
                        case 5:
                            if (!utils.isBlankString(row.get(3).trim())) {
                                mapRow.add(j, row.get(3).trim());
                            }
                            break;
                        case 8:
                            mapRow.add(j, row.get(8).trim());
                            break;
                        case 9:
                            date = dateFormat.parse(row.get(3).trim());
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(date);
                            Pattern p = Pattern.compile("\\d+");
                            Matcher m = p.matcher(row.get(8).trim());
                            String terms = "";
                            if (m.find()) {
                                terms = m.group();
                            }
                            int days = 0;
                            try {
                                days = Integer.parseInt(terms);
                            } catch (NumberFormatException ignored) {
                            }
                            calendar.add(Calendar.DATE, days);
                            dateString = dateFormat.format(calendar.getTime());
                            mapRow.add(j, dateString);
                            break;
                        case 10:

                            date = dateFormat.parse(row.get(3).trim());
                            SimpleDateFormat postDate = new SimpleDateFormat("MM/yy");
                            dateString = postDate.format(date);
                            mapRow.add(j, dateString);
                            break;
                        case 11:
                            String contractSalesRep = row.get(4).trim();
                            mapRow.add(j, contractSalesRep);
                            break;
                        case 12:
                            mapRow.add(j, row.get(5).trim());
                            break;
                        case 15:
                            mapRow.add(j, "USA");
                            break;
                        case 16:
                            mapRow.add(j, "1#");
                            break;
                        default:
                            mapRow.add(j, "");
                            break;
                    }
                    progress += progressUpdate;
                    updateProgress(progress, 1.0);
                }
                switch (row.get(9).trim()) {
                    case "001":
                        mapTable1.add(mapRow);
                        break;
                    case "002":
                        mapTable2.add(mapRow);
                        break;
                    case "003":
                        mapTable3.add(mapRow);
                        break;
                }

                utils.sleep(1);
            }
            mapTable1.sort((o1, o2) -> {
                               if (mapTable1.indexOf(o1) == 0) {
                                   return -1;
                               }
                               else if (mapTable1.indexOf(o2) == 0) {
                                   return 1;
                               }
                               else {
                                   return o1.get(0).compareTo(o2.get(0));
                               }
                           }
            );
            mapTable2.sort((o1, o2) -> {
                               if (mapTable2.indexOf(o1) == 0) {
                                   return -1;
                               }
                               else if (mapTable2.indexOf(o2) == 0) {
                                   return 1;
                               }
                               else {
                                   return o1.get(0).compareTo(o2.get(0));
                               }
                           }
            );
            mapTable3.sort((o1, o2) -> {
                               if (mapTable3.indexOf(o1) == 0) {
                                   return -1;
                               }
                               else if (mapTable3.indexOf(o2) == 0) {
                                   return 1;
                               }
                               else {
                                   return o1.get(0).compareTo(o2.get(0));
                               }
                           }
            );
            tables.add(mapTable1);
            tables.add(mapTable2);
            tables.add(mapTable3);
            return tables;
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
    