package org.dataprocessing.backend.mappers.por;

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
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Maps the POR Open Sales Template
 *
 * @author Nicholas Curl
 */
public class POROpenSales {

    /**
     * The instance of the logger
     */
    private static final Logger logger = LogManager.getLogger();
    /**
     * The instance of the FileUtils class
     */
    private static final FileUtils fileUtils = FileUtils.getInstance();
    /**
     * The template mapping task
     */
    private final MapTemplate mapTemplate;
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
    public POROpenSales(Path storeLocation) {
        tasks = new LinkedList<>();
        mapTemplate = new MapTemplate();
        tableConvertTask = new ServerTableConvertTask(
                "SELECT TransactionItems.CNTR         AS [Contract Number],\n" +
                        "       CustomerFile.NAME             as [Customer Name],\n" +
                        "       Transactions.DATE             AS [Transaction Date],\n" +
                        "       Salesman.Name                 AS [Transaction Sales Rep],\n" +
                        "       TransactionItems.ITEM         as [Item No],\n" +
                        "       ItemFile.Name                 as [Item Name],\n" +
                        "       TransactionItems.QTY,\n" +
                        "       TransactionItems.PRIC         as [Sales Price],\n" +
                        "       ItemDepartment.DepartmentName as [Item Department],\n" +
                        "       ItemFile.CurrentStore         AS [Location],\n" +
                        "       Transactions.DeliveryDate,\n" +
                        "       Transactions.DeliveryAddress,\n" +
                        "       Transactions.DeliveryCity,\n" +
                        "       Transactions.DeliveryZip,\n" +
                        "       Transactions.Contact          as [Delivery Contact],\n" +
                        "       Transactions.ContactPhone     as [Delivery Contact Phone Num],\n" +
                        "       CustomerFile.Terms,\n" +
                        "       CustomerFile.BillContact,\n" +
                        "       CustomerFile.BillPhone,\n" +
                        "       CustomerFile.BillAddress1,\n" +
                        "       CustomerFile.BillAddress2,\n" +
                        "       CustomerFile.BillCityState,\n" +
                        "       CustomerFile.BillZip,\n" +
                        "       CustomerFile.BillZip4,\n" +
                        "       CustomerFile.Email,\n" +
                        "       CustomerFile.FAX,\n" +
                        "       TaxTable.TaxDescription,\n" +
                        "       Transactions.JBPO,\n" +
                        "       Transactions.Notes,\n" +
                        "       Transactions.JBID,\n" +
                        "       TransactionType.TypeName,\n" +
                        "       TransactionItems.Comments,\n" +
                        "       Transactions.EventEndDate,\n" +
                        "       Transactions.ReviewBilling,\n" +
                        "       TaxTable.TaxRent1,\n" +
                        "       Transactions.PickupDate,\n" +
                        "       Transactions.STR\n" +
                        "FROM TransactionItems\n" +
                        "         LEFT JOIN Transactions on Transactions.CNTR = TransactionItems.CNTR\n" +
                        "         LEFT JOIN ItemFile on TransactionItems.ITEM = ItemFile.NUM\n" +
                        "         LEFT JOIN CustomerFile on Transactions.CUSN = CustomerFile.CNUM\n" +
                        "         LEFT JOIN ItemDepartment on ItemFile.Department = ItemDepartment.Department\n" +
                        "         INNER JOIN (SELECT DISTINCT CustomerFile.NAME        as Customer_Name,\n" +
                        "                                     Transactions.TOTL        as Transaction_Total,\n" +
                        "                                     Transactions.STAT        as Transaction_Status,\n" +
                        "                                     Transactions.CNTR        as Transaction_Contract,\n" +
                        "                                     Transactions.PAID,\n" +
                        "                                     Transactions.CLDT        as Transaction_Close_Date,\n" +
                        "                                     Salesman_Cntr.Name       as Contract_Sales_Rep_Name,\n" +
                        "                                     TransactionType.TypeName as Transaction_Type,\n" +
                        "                                     Salesman_customer.Name   as Customer_Sales_Rep_Name,\n" +
                        "                                     Salesman_jobsite.Name    as Jobsite_Sales_Rep_Name,\n" +
                        "                                     Transactions.DEPP        as Deprication,\n" +
                        "                                     CustomerFile.CurrentBalance,\n" +
                        "                                     CustomerFile.Terms\n" +
                        "                     FROM Transactions\n" +
                        "                              LEFT OUTER JOIN CustomerFile ON Transactions.CUSN = CustomerFile.CNUM\n" +
                        "                              LEFT OUTER JOIN Salesman Salesman_Cntr ON Transactions.Salesman = Salesman_Cntr.Number\n" +
                        "                              LEFT OUTER JOIN CustomerJobSite ON Transactions.JobSite = CustomerJobSite.Number\n" +
                        "                              LEFT OUTER JOIN TransactionType\n" +
                        "                                              ON Transactions.TransactionType = TransactionType.TypeNumber\n" +
                        "                              LEFT OUTER JOIN TransactionOperation\n" +
                        "                                              ON Transactions.Operation = TransactionOperation.OperationNumber\n" +
                        "                              LEFT OUTER JOIN ParameterFile ON Transactions.STR = ParameterFile.Store\n" +
                        "                              LEFT OUTER JOIN Salesman Salesman_jobsite\n" +
                        "                                              ON CustomerJobSite.Salesman = Salesman_jobsite.Number\n" +
                        "                              LEFT OUTER JOIN CustomerType ON CustomerFile.Type = CustomerType.Type\n" +
                        "                              LEFT OUTER JOIN Salesman Salesman_customer\n" +
                        "                                              ON CustomerFile.Salesman = Salesman_customer.Number\n" +
                        "                     WHERE (Transactions.TOTL <> Transactions.PAID OR Transactions.DEPP <> 0)\n" +
                        "                       AND Transactions.Archived = 0\n" +
                        "                       AND Transactions.PYMT <> N'T'\n" +
                        "                       AND Transactions.STAT NOT LIKE 'R%'\n" +
                        "                       AND Transactions.STAT NOT LIKE 'Q%'\n" +
                        "                       AND Transactions.STAT NOT LIKE 'C%') Q ON Q.Transaction_Contract = TransactionItems.CNTR\n" +
                        "         LEFT JOIN Salesman on Transactions.Salesman = Salesman.Number\n" +
                        "         LEFT JOIN TransactionType on Transactions.TransactionType = TransactionType.TypeNumber\n" +
                        "         LEFT JOIN TaxTable on Transactions.TaxCode = TaxTable.TaxCode\n"
        );
        writeTask1 = fileUtils.writeXlsxTask(storeLocation.resolve("Open Sales Template-Mahaffey Tent & Awning.xlsx"));
        writeTask2 = fileUtils.writeXlsxTask(storeLocation.resolve("Open Sales Template-Mahaffey USA.xlsx"));
        writeTask3 = fileUtils.writeXlsxTask(storeLocation.resolve("Open Sales Template-Mahaffey USA-Houston.xlsx"));
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
            mapTemplate.setData(tableConvertTask.getValue());
            executorService.submit(mapTemplate);
        });
        mapTemplate.setOnSucceeded(event -> {
            writeTask1.setTable(mapTemplate.getValue().get(0));
            writeTask2.setTable(mapTemplate.getValue().get(1));
            writeTask3.setTable(mapTemplate.getValue().get(2));
            executorService.submit(writeTask1);

        });
        writeTask1.setOnSucceeded(event -> executorService.submit(writeTask2));
        writeTask2.setOnSucceeded(event -> executorService.submit(writeTask3));
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
     * Maps the data to the Open Sales Template
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
         * The instance of SqlServer Class
         */
        private static final SqlServer server = SqlServer.getInstance();
        /**
         * The template associated with this mapping
         */
        private static final String template = "/templates/Open Sales Order Template_MFG FINAL.xlsx";
        /**
         * The corrections file associated with this mapping
         */
        private static final String correctionsFile = "./corrections/por.txt";
        private static final Logger logger = LogManager.getLogger();
        /**
         * The header of the template
         */
        private final List<String> header;
        /**
         * The map containing the incorrect string and its correction
         */
        private final Map<String, String> corrections;
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
         * Local copy of the data to map
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
            corrections = mapperUtils.getCorrections(correctionsFile);
            tables = new LinkedList<>();
        }

        /**
         * Maps the Open Sales template
         *
         * @return The table of the mapped data
         *
         * @throws Exception Any exception that might occur when executing this task
         */
        @Override
        protected List<List<List<String>>> call() throws Exception {
            String[] split;
            String dateString;
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
                for (int j = 0; j < header.size(); j++) {
                    if (isCancelled()) {
                        break loopBreak;
                    }
                    switch (j) {
                        case 0:
                        case 1:
                            mapRow.add(j, row.get(0).trim() + "@");
                            break;
                        case 2:
                            mapRow.add(j, row.get(1).trim());
                            break;
                        case 3:
                        case 5:
                            mapRow.add(j, row.get(2).trim());
                            break;
                        case 4:
                            if (row.get(33).trim().equalsIgnoreCase("1")) {
                                mapRow.add(j, "Review Billing");
                            } else {
                                mapRow.add(j, "Open");
                            }
                            break;
                        case 6:
                            dateString = row.get(32).trim();
                            if (dateString.contains("1899-12-30")) {
                                mapRow.add(j, row.get(35).trim());
                            } else {
                                mapRow.add(j, dateString);
                            }
                            break;
                        case 8:
                            if (i != 1) {
                                if (row.get(0).trim().equalsIgnoreCase(data.get(i - 1).get(0).trim())) {
                                    mapRow.add(j, "");
                                } else {
                                    mapRow.add(j, row.get(28).trim() + "^");
                                }
                            } else {
                                mapRow.add(j, row.get(28).trim() + "^");
                            }
                            break;
                        case 9:
                            mapRow.add(j, row.get(3).trim());
                            break;
                        case 21:
                            mapRow.add(j, row.get(4).trim() + "#");
                            break;
                        case 22:
                            mapRow.add(j, row.get(6).trim() + "#");
                            break;
                        case 25:
                            try {
                                double amount = Double.parseDouble(row.get(7).trim());
                                if (amount == 0) {
                                    mapRow.add(j, row.get(7).trim());
                                } else {
                                    double qty = Double.parseDouble(row.get(6).trim());
                                    double result = amount / qty;
                                    if (Double.isNaN(result) || Double.isInfinite(result)) {
                                        logger.fatal("Divide by zero error");
                                        System.exit(-1);
                                    }
                                    mapRow.add(j, utils.round(result, 2) + "$");
                                }
                            } catch (NumberFormatException ignored) {
                            }
                            break;
                        case 26:
                            mapRow.add(j, row.get(7).trim());
                            break;
                        case 27:
                            mapRow.add(j, row.get(5).trim());
                            break;
                        case 28:
                        case 62:
                            mapRow.add(j, "TRUE");
                            break;
                        case 30:
                            mapRow.add(j, row.get(8).trim());
                            break;
                        case 32:
                            String currentStore = row.get(9).trim();
                            if (currentStore.equalsIgnoreCase("003")) {
                                mapRow.add(j, "Houston Depot");
                            } else {
                                mapRow.add(j, "Memphis");
                            }
                            break;
                        case 36:
                            dateString = row.get(10).trim();
                            if (dateString.contains("1899-12-30")) {
                                mapRow.add(j, row.get(2).trim());
                            } else {
                                mapRow.add(j, dateString);
                            }
                            break;
                        case 41:
                            mapRow.add(j, row.get(14).trim());
                            break;
                        case 43:
                            split = row.get(11).trim().split(", ");
                            mapRow.add(j, split[0]);
                            try {
                                mapRow.add(j + 1, split[1]);
                            } catch (IndexOutOfBoundsException e) {
                                mapRow.add(j + 1, "");
                            }
                            break;
                        case 45:
                            split = row.get(12).trim().split(", ");
                            mapRow.add(j, split[0]);
                            try {
                                mapRow.add(j + 1, split[1]);
                            } catch (IndexOutOfBoundsException e) {
                                mapRow.add(j + 1, "");
                            }
                            break;
                        case 44:
                        case 46:
                        case 56:
                            break;
                        case 47:
                            mapRow.add(j, row.get(13).trim());
                            break;
                        case 49:
                            mapRow.add(j, row.get(15).trim());
                            break;
                        case 50:
                            mapRow.add(j, row.get(16).trim());
                            break;
                        case 51:
                            mapRow.add(j, row.get(17).trim());
                            break;
                        case 53:
                            mapRow.add(j, row.get(19).trim());
                            break;
                        case 54:
                            mapRow.add(j, row.get(20).trim());
                            break;
                        case 55:
                            //Checks to see if the city and state is blank
                            if (utils.isBlankString(row.get(21))) {
                                mapRow.add(j, "");
                                mapRow.add(j + 1, "");
                            }
                            //Checks to see if the city and state has a correction
                            else if (corrections.containsKey(row.get(21).trim())) {
                                split = corrections.get(row.get(21).trim()).split(",");
                                mapRow.add(j, split[0].trim());
                                mapRow.add(j + 1, split[1].trim());
                            } else {
                                split = row.get(21).split(",");
                                mapRow.add(j, split[0].trim());
                                mapRow.add(j + 1, split[1].trim());
                            }
                            break;
                        case 57:
                            if (utils.isBlankString(row.get(23))) {
                                mapRow.add(j, row.get(22));
                            } else if (utils.isBlankString(row.get(22)) && utils.isBlankString(row.get(23))) {
                                mapRow.add(j, "");
                            } else {
                                mapRow.add(j, row.get(22) + "-" + row.get(23));
                            }
                            break;
                        case 48:
                        case 58:
                        case 60:
                            mapRow.add(j, "USA");
                            break;
                        case 59:
                            mapRow.add(j, row.get(18).trim());
                            break;
                        case 61:
                            mapRow.add(j, "1#");
                            break;
                        case 63:
                            String taxState = row.get(26).trim().substring(0, 2);
                            mapRow.add(j, taxState);
                            break;
                        case 64:
                            mapRow.add(j, row.get(34).trim() + "%");
                            break;
                        case 65:
                        case 66:
                        case 68:
                            mapRow.add(j, "FALSE");
                            break;
                        case 67:
                            mapRow.add(j, row.get(24).trim());
                            break;
                        case 69:
                            mapRow.add(j, row.get(25).trim());
                            break;
                        case 72:
                            mapRow.add(j, row.get(27).trim() + "@");
                            break;
                        case 73:
                            mapRow.add(j, row.get(29).trim() + "@");
                            break;
                        case 74:
                            mapRow.add(j, row.get(30).trim());
                            break;
                        case 75:
                            mapRow.add(j, row.get(31).trim() + "^");
                            break;
                        default:
                            mapRow.add(j, "");
                            break;
                    }

                    progress += progressUpdate;
                    updateProgress(progress, 1.0);
                }
                switch (row.get(36).trim()) {
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
    