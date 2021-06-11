package org.dataprocessing.backend.mappers.por;

import com.google.common.collect.Iterables;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.concurrent.Task;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dataprocessing.backend.objects.SalesOrder;
import org.dataprocessing.backend.objects.Subassembly;
import org.dataprocessing.backend.objects.Subassembly.AssemblyItem;
import org.dataprocessing.backend.tasks.KitMapper;
import org.dataprocessing.backend.tasks.KitMapper.KitMapping;
import org.dataprocessing.backend.tasks.ServerTableConvertTask;
import org.dataprocessing.utils.CustomExecutors;
import org.dataprocessing.utils.FileUtils;
import org.dataprocessing.utils.FileUtils.XlsxTask;
import org.dataprocessing.utils.FileUtils.XlsxTaskMultiSheet;
import org.dataprocessing.utils.MapperUtils;
import org.dataprocessing.utils.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Maps the POR Open Sales Template
 *
 * @author Nicholas Curl
 */
public class POROpenSales {

    /**
     * The instance of the logger
     */
    private static final Logger                 logger    = LogManager.getLogger(POROpenSales.class);
    /**
     * The instance of the FileUtils class
     */
    private static final FileUtils              fileUtils = FileUtils.getInstance();
    private static final Utils                  utils     = Utils.getInstance();
    /**
     * The template mapping task
     */
    private final        MapTemplate            mapTemplate;
    /**
     * The server table convert task
     */
    private final        ServerTableConvertTask tableConvertTask;
    /**
     * The first excel writing task
     */
    private final        XlsxTask               writeTask1;
    /**
     * The second excel writing task
     */
    private final        XlsxTask               writeTask2;
    /**
     * The third excel writing task
     */
    private final        XlsxTask               writeTask3;
    private final        XlsxTaskMultiSheet     writeTask4;
    private final        XlsxTaskMultiSheet     writeTask5;
    private final        XlsxTaskMultiSheet     writeTask6;
    private final        XlsxTask               writeTask7;
    /**
     * The list of sub-tasks
     */
    private final List<Task<?>>         tasks;
    private final GroupSalesOrders      groupSalesOrders;
    private final FilterSubassemblies   filterSubassemblies;
    private final BreakoutSubassemblies breakoutSubassemblies;
    private final GroupSalesOrders      groupSalesOrders1;
    private final FilterSubassemblies   filterSubassemblies1;
    private final BreakoutSubassemblies breakoutSubassemblies1;
    private final GroupSalesOrders      groupSalesOrders2;
    private final FilterSubassemblies   filterSubassemblies2;
    private final BreakoutSubassemblies breakoutSubassemblies2;
    private final GroupSalesOrders      groupData;
    private final CreateSalesOrders     createSalesOrders;
    /**
     * The total progress of this task
     */
    private final DoubleBinding         totalProgress;
    private final KitMapper             kitMapper;
    private final KitMapping            kitMapping;

    /**
     * The constructor for this class
     *
     * @param storeLocation The path to the directory to store the mapped data
     */
    public POROpenSales(Path storeLocation) {
        tasks = new ArrayList<>();
        mapTemplate = new MapTemplate();
        tableConvertTask = new ServerTableConvertTask(
                "SELECT TransactionItems.CNTR             AS [Contract Number],\n" +
                "       CustomerFile.NAME                 as [Customer Name],\n" +
                "       Transactions.DATE                 AS [Transaction Date],\n" +
                "       Salesman.Name                     AS [Transaction Sales Rep],\n" +
                "       ItemFile.[KEY]                    as [Item No],\n" +
                "       iif((ItemFile.[KEY] = 'ACC-TENTNATC' OR ItemFile.[KEY] like 'ACC-ACCNAT%' or\n" +
                "            ItemFile.[KEY] = 'ACC-FLDSUPP' or ItemFile.[KEY] = 'ACC-FLRNAT' or ItemFile.[KEY] = 'ACC-HVAC' OR\n" +
                "            ItemFile.[KEY] = 'ACC-TENTNATC_S' OR ItemFile.[KEY] = 'NOTE'),\n" +
                "           iif(TransactionItems.[Desc] = '', ItemFile.Name, TransactionItems.[Desc]),\n" +
                "           ItemFile.Name)\n" +
                "                                         as [Item Name],\n" +
                "       TransactionItems.QTY,\n" +
                "       TransactionItems.PRIC             as [Sales Price],\n" +
                "       ItemDepartment.DepartmentName     as [Item Department],\n" +
                "       ItemFile.CurrentStore             AS [Location],\n" +
                "       Transactions.DeliveryDate,\n" +
                "       Transactions.DeliveryAddress,\n" +
                "       Transactions.DeliveryCity,\n" +
                "       Transactions.DeliveryZip,\n" +
                "       Transactions.Contact              as [Delivery Contact],\n" +
                "       Transactions.ContactPhone         as [Delivery Contact Phone Num],\n" +
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
                "       Transactions.STR,\n" +
                "       ItemCategory.Name,\n" +
                "       isnull(ServiceMap.ServiceName, 0) as ServiceName,\n" +
                "       isnull(ServiceMap.ServiceID, 0)   as ServiceID,\n" +
                "       Transactions.JOBN\n" +
                "FROM TransactionItems\n" +
                "         LEFT JOIN Transactions on Transactions.CNTR = TransactionItems.CNTR\n" +
                "         LEFT JOIN ItemFile on TransactionItems.ITEM = ItemFile.NUM\n" +
                "         LEFT JOIN CustomerFile on Transactions.CUSN = CustomerFile.CNUM\n" +
                "         LEFT JOIN ItemDepartment on ItemFile.Department = ItemDepartment.Department\n" +
                "         INNER JOIN (\n" +
                "    SELECT DISTINCT CustomerFile.NAME        as Customer_Name,\n" +
                "                    Transactions.TOTL        as Transaction_Total,\n" +
                "                    Transactions.STAT        as Transaction_Status,\n" +
                "                    Transactions.CNTR        as Transaction_Contract,\n" +
                "                    Transactions.PAID,\n" +
                "                    Transactions.CLDT        as Transaction_Close_Date,\n" +
                "                    Salesman_Cntr.Name       as Contract_Sales_Rep_Name,\n" +
                "                    TransactionType.TypeName as Transaction_Type,\n" +
                "                    Salesman_customer.Name   as Customer_Sales_Rep_Name,\n" +
                "                    Salesman_jobsite.Name    as Jobsite_Sales_Rep_Name,\n" +
                "                    Transactions.DEPP        as Deprication,\n" +
                "                    CustomerFile.CurrentBalance,\n" +
                "                    CustomerFile.Terms\n" +
                "    FROM Transactions\n" +
                "             LEFT OUTER JOIN CustomerFile ON Transactions.CUSN = CustomerFile.CNUM\n" +
                "             LEFT OUTER JOIN Salesman Salesman_Cntr ON Transactions.Salesman = Salesman_Cntr.Number\n" +
                "             LEFT OUTER JOIN CustomerJobSite ON Transactions.JobSite = CustomerJobSite.Number\n" +
                "             LEFT OUTER JOIN TransactionType\n" +
                "                             ON Transactions.TransactionType = TransactionType.TypeNumber\n" +
                "             LEFT OUTER JOIN TransactionOperation\n" +
                "                             ON Transactions.Operation = TransactionOperation.OperationNumber\n" +
                "             LEFT OUTER JOIN ParameterFile ON Transactions.STR = ParameterFile.Store\n" +
                "             LEFT OUTER JOIN Salesman Salesman_jobsite\n" +
                "                             ON CustomerJobSite.Salesman = Salesman_jobsite.Number\n" +
                "             LEFT OUTER JOIN CustomerType ON CustomerFile.Type = CustomerType.Type\n" +
                "             LEFT OUTER JOIN Salesman Salesman_customer\n" +
                "                             ON CustomerFile.Salesman = Salesman_customer.Number\n" +
                "    WHERE (Transactions.TOTL <> Transactions.PAID OR Transactions.DEPP <> 0)\n" +
                "      AND Transactions.Archived = 0\n" +
                "      AND Transactions.PYMT <> N'T'\n" +
                "      AND Transactions.STAT NOT LIKE 'R%'\n" +
                "      AND Transactions.STAT NOT LIKE 'Q%'\n" +
                "      AND Transactions.STAT NOT LIKE 'C%'\n" +
                ") Q ON Q.Transaction_Contract = TransactionItems.CNTR\n" +
                "         LEFT JOIN Salesman on Transactions.Salesman = Salesman.Number\n" +
                "         LEFT JOIN TransactionType on Transactions.TransactionType = TransactionType.TypeNumber\n" +
                "         LEFT JOIN TaxTable on Transactions.TaxCode = TaxTable.TaxCode\n" +
                "         LEFT JOIN ItemCategory on ItemFile.Category = ItemCategory.Category\n" +
                "         left join ServiceMap\n" +
                "                   on ItemFile.Category = ServiceMap.ItemCatID and ItemFile.Department = ServiceMap.ItemDeptID"
        );
        groupData = new GroupSalesOrders();
        createSalesOrders = new CreateSalesOrders();
        kitMapper = new KitMapper();
        kitMapping = (KitMapping) kitMapper.getTasks().get(1);
        writeTask1 = fileUtils.writeXlsxTask(storeLocation.resolve("Open Sales Template-Mahaffey Tent & Awning.xlsx"));
        writeTask2 = fileUtils.writeXlsxTask(storeLocation.resolve("Open Sales Template-Mahaffey USA.xlsx"));
        writeTask3 = fileUtils.writeXlsxTask(storeLocation.resolve("Open Sales Template-Mahaffey USA-Houston.xlsx"));
        writeTask4 = fileUtils.writeXlsxTaskMultiSheet(storeLocation.resolve(
                "Open Sales Template-Filtered Mahaffey Tent & Awning.xlsx"));
        writeTask5 = fileUtils.writeXlsxTaskMultiSheet(storeLocation.resolve(
                "Open Sales Template-Filtered Mahaffey USA.xlsx"));
        writeTask6 = fileUtils.writeXlsxTaskMultiSheet(storeLocation.resolve(
                "Open Sales Template-Filtered Mahaffey USA-Houston.xlsx"));
        writeTask7 = fileUtils.writeXlsxTask(storeLocation.resolve("Open Sales Template-All.xlsx"));
        groupSalesOrders = new GroupSalesOrders();
        filterSubassemblies = new FilterSubassemblies();
        breakoutSubassemblies = new BreakoutSubassemblies();
        groupSalesOrders1 = new GroupSalesOrders();
        filterSubassemblies1 = new FilterSubassemblies();
        breakoutSubassemblies1 = new BreakoutSubassemblies();
        groupSalesOrders2 = new GroupSalesOrders();
        filterSubassemblies2 = new FilterSubassemblies();
        breakoutSubassemblies2 = new BreakoutSubassemblies();
        tasks.add(tableConvertTask);
        tasks.add(groupData);
        tasks.add(mapTemplate);
        tasks.add(createSalesOrders);
        tasks.addAll(kitMapper.getTasks());
        tasks.add(groupSalesOrders);
        tasks.add(filterSubassemblies);
        tasks.add(breakoutSubassemblies);
        tasks.add(groupSalesOrders1);
        tasks.add(filterSubassemblies1);
        tasks.add(breakoutSubassemblies1);
        tasks.add(groupSalesOrders2);
        tasks.add(filterSubassemblies2);
        tasks.add(breakoutSubassemblies2);
        tasks.add(writeTask1);
        tasks.add(writeTask2);
        tasks.add(writeTask3);
        tasks.add(writeTask4);
        tasks.add(writeTask5);
        tasks.add(writeTask6);
        tasks.add(writeTask7);
        totalProgress = Bindings.createDoubleBinding(() -> (Math.max(0, tableConvertTask.getProgress()) +
                                                            Math.max(0, groupData.getProgress()) +
                                                            Math.max(0, createSalesOrders.getProgress()) +
                                                            Math.max(0, mapTemplate.getProgress()) +
                                                            Math.max(0, kitMapper.getTotalProgress()) +
                                                            Math.max(0, groupSalesOrders.getProgress()) +
                                                            Math.max(0, filterSubassemblies.getProgress()) +
                                                            Math.max(0, breakoutSubassemblies.getProgress()) +
                                                            Math.max(0, groupSalesOrders1.getProgress()) +
                                                            Math.max(0, filterSubassemblies1.getProgress()) +
                                                            Math.max(0, breakoutSubassemblies1.getProgress()) +
                                                            Math.max(0, groupSalesOrders2.getProgress()) +
                                                            Math.max(0, filterSubassemblies2.getProgress()) +
                                                            Math.max(0, breakoutSubassemblies2.getProgress()) +
                                                            Math.max(0, writeTask1.getProgress()) +
                                                            Math.max(0, writeTask2.getProgress()) +
                                                            Math.max(0, writeTask3.getProgress()) +
                                                            Math.max(0, writeTask4.getProgress()) +
                                                            Math.max(0, writeTask5.getProgress()) +
                                                            Math.max(0, writeTask6.getProgress()) +
                                                            Math.max(0, writeTask7.getProgress())
                                                           ) / 21,
                                                     tableConvertTask.progressProperty(),
                                                     groupData.progressProperty(),
                                                     createSalesOrders.progressProperty(),
                                                     mapTemplate.progressProperty(),
                                                     kitMapper.totalProgressProperty(),
                                                     groupSalesOrders.progressProperty(),
                                                     filterSubassemblies.progressProperty(),
                                                     breakoutSubassemblies.progressProperty(),
                                                     groupSalesOrders1.progressProperty(),
                                                     filterSubassemblies1.progressProperty(),
                                                     breakoutSubassemblies1.progressProperty(),
                                                     groupSalesOrders2.progressProperty(),
                                                     filterSubassemblies2.progressProperty(),
                                                     breakoutSubassemblies2.progressProperty(),
                                                     writeTask1.progressProperty(),
                                                     writeTask2.progressProperty(),
                                                     writeTask3.progressProperty(),
                                                     writeTask4.progressProperty(),
                                                     writeTask5.progressProperty(),
                                                     writeTask6.progressProperty(),
                                                     writeTask7.progressProperty()
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
     */
    public void map(ExecutorService executorService) {
        kitMapping.setOnSucceeded(event -> {
            tableConvertTask.getValue().remove(0);
            List<List<?>> dataTemp = tableConvertTask.getValue()
                                                     .parallelStream()
                                                     .sorted(Comparator.comparing((List<?> o) -> (String) o.get(0))
                                                                       .thenComparing((List<?> o) -> {
                                                                           if ((Double) o.get(6) == 0) {
                                                                               return (BigDecimal) o.get(7);
                                                                           }
                                                                           else {
                                                                               return ((BigDecimal) o.get(7)).divide(
                                                                                       BigDecimal.valueOf((Double) o.get(
                                                                                               6)),
                                                                                       RoundingMode.HALF_UP
                                                                               );
                                                                           }
                                                                       }, Comparator.reverseOrder())
                                                                       .thenComparing((List<?> o) -> (String) o.get(4)))
                                                     .collect(Collectors.toList());
            groupData.setHasHeader(false);
            List<List<String>> dataTempString = utils.convertToTableString(dataTemp);
            groupData.setData(dataTempString);
            executorService.submit(groupData);
        });
        groupData.setOnSucceeded(event -> {
            createSalesOrders.setData(groupData.getValue());
            executorService.submit(createSalesOrders);
        });
        createSalesOrders.setOnSucceeded(event -> {
            mapTemplate.setData(createSalesOrders.getValue());
            executorService.submit(mapTemplate);
        });
        mapTemplate.setOnSucceeded(event -> {
            writeTask1.setTable(mapTemplate.getValue().get(0));
            writeTask2.setTable(mapTemplate.getValue().get(1));
            writeTask3.setTable(mapTemplate.getValue().get(2));
            writeTask7.setTable(mapTemplate.getValue().get(3));
            groupSalesOrders.setData(mapTemplate.getValue().get(0));
            groupSalesOrders1.setData(mapTemplate.getValue().get(1));
            groupSalesOrders2.setData(mapTemplate.getValue().get(2));
            executorService.submit(writeTask1);
        });
        writeTask3.setOnSucceeded(event -> executorService.submit(groupSalesOrders));
        groupSalesOrders.setOnSucceeded(event -> {
            filterSubassemblies.setData(groupSalesOrders.getValue());
            filterSubassemblies.setKits(kitMapping.getValue());
            executorService.submit(filterSubassemblies);
        });
        filterSubassemblies.setOnSucceeded(event -> {
            breakoutSubassemblies.setData(filterSubassemblies.getValue());
            breakoutSubassemblies.setKits(kitMapping.getValue());
            executorService.submit(breakoutSubassemblies);
        });
        breakoutSubassemblies.setOnSucceeded(event -> {
            List<List<String>> table = utils.parallelSortListAscending(new ArrayList<>(filterSubassemblies.getValue()
                                                                                                          .values()),
                                                                       0
            );
            table.add(0, mapTemplate.getHeader());
            List<List<String>> breakoutTable = new ArrayList<>(breakoutSubassemblies.getValue().values());
            writeTask4.setSheets(addBreakoutHeader(table, breakoutTable));
            executorService.submit(writeTask4);
        });
        writeTask4.setOnSucceeded(event -> executorService.submit(groupSalesOrders1));
        groupSalesOrders1.setOnSucceeded(event -> {
            filterSubassemblies1.setData(groupSalesOrders1.getValue());
            filterSubassemblies1.setKits(kitMapping.getValue());
            executorService.submit(filterSubassemblies1);
        });
        filterSubassemblies1.setOnSucceeded(event -> {
            breakoutSubassemblies1.setData(filterSubassemblies1.getValue());
            breakoutSubassemblies1.setKits(kitMapping.getValue());
            executorService.submit(breakoutSubassemblies1);
        });
        breakoutSubassemblies1.setOnSucceeded(event -> {
            List<List<String>> table = utils.parallelSortListAscending(new ArrayList<>(filterSubassemblies1.getValue()
                                                                                                           .values()),
                                                                       0
            );
            table.add(0, mapTemplate.getHeader());
            List<List<String>> breakoutTable = new ArrayList<>(breakoutSubassemblies1.getValue().values());
            writeTask5.setSheets(addBreakoutHeader(table, breakoutTable));
            executorService.submit(writeTask5);
        });
        writeTask5.setOnSucceeded(event -> executorService.submit(groupSalesOrders2));
        groupSalesOrders2.setOnSucceeded(event -> {
            filterSubassemblies2.setData(groupSalesOrders2.getValue());
            filterSubassemblies2.setKits(kitMapping.getValue());
            executorService.submit(filterSubassemblies2);
        });
        filterSubassemblies2.setOnSucceeded(event -> {
            breakoutSubassemblies2.setData(filterSubassemblies2.getValue());
            breakoutSubassemblies2.setKits(kitMapping.getValue());
            executorService.submit(breakoutSubassemblies2);
        });
        breakoutSubassemblies2.setOnSucceeded(event -> {
            List<List<String>> table = utils.parallelSortListAscending(new ArrayList<>(filterSubassemblies2.getValue()
                                                                                                           .values()),
                                                                       0
            );
            table.add(0, mapTemplate.getHeader());
            List<List<String>> breakoutTable = new ArrayList<>(breakoutSubassemblies2.getValue().values());
            writeTask6.setSheets(addBreakoutHeader(table, breakoutTable));
            executorService.submit(writeTask6);
        });
        writeTask6.setOnSucceeded(event -> executorService.submit(writeTask7));
        writeTask7.setOnSucceeded(event -> totalProgress.add(0.0001));
        writeTask1.setOnSucceeded(event -> executorService.submit(writeTask2));
        writeTask2.setOnSucceeded(event -> executorService.submit(writeTask3));
        executorService.submit(tableConvertTask);
        kitMapper.map(executorService);
    }

    private Map<String, List<List<String>>> addBreakoutHeader(List<List<String>> table,
                                                              List<List<String>> breakoutTable
    ) {
        List<String> header = new ArrayList<>();
        header.add("contractId");
        header.add("mainAssemblyKey");
        header.add("parentKey");
        header.add("itemKey");
        header.add("itemName");
        header.add("itemQty");
        header.add("subAssemblyTotalQty");
        breakoutTable.add(0, header);
        Map<String, List<List<String>>> sheets = new HashMap<>();
        sheets.put("Mapped", table);
        sheets.put("Breakout", breakoutTable);
        return sheets;
    }

    /**
     * Gets the total progress property
     *
     * @return The total progress property
     */
    public DoubleBinding totalProgressProperty() {
        return totalProgress;
    }


    private static class CreateSalesOrders extends Task<List<SalesOrder>> {

        private static final Utils                                utils = Utils.getInstance();
        private final        List<SalesOrder>                     salesOrders;
        /**
         * Local copy of the data to map
         */
        private              MultiValuedMap<String, List<String>> data;

        private CreateSalesOrders() {
            this.salesOrders = new ArrayList<>();
        }

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
        protected List<SalesOrder> call() throws Exception {
            double progress = 0.0;
            double progressUpdate = 1.0 / data.keySet().size();
            updateProgress(progress, 1.0);
            for (String salesKey : data.keySet()) {
                SalesOrder salesOrder = new SalesOrder(salesKey);
                List<List<String>> dataSet = new ArrayList<>(data.get(salesKey));
                if (dataSet.get(0).get(40).trim().toLowerCase(Locale.ROOT).contains("mts")) {
                    List<List<String>> structures = findStructure(dataSet, "mts");
                    findMainStructures(salesKey, salesOrder, dataSet, structures);
                }
                else if (dataSet.get(0).get(40).trim().toLowerCase(Locale.ROOT).contains("h334")) {
                    List<List<String>> structures = findStructure(dataSet, "h334");
                    findMainStructures(salesKey, salesOrder, dataSet, structures);
                }
                else if (dataSet.get(0).get(40).trim().toLowerCase(Locale.ROOT).contains("l300") ||
                         dataSet.get(0).get(40).trim().toLowerCase(Locale.ROOT).contains("lm300")
                ) {
                    List<List<String>> structures = findStructure(dataSet, "l300");
                    findMainStructures(salesKey, salesOrder, dataSet, structures);
                }
                else if (dataSet.get(0).get(40).trim().toLowerCase(Locale.ROOT).contains("h202")) {
                    List<List<String>> structures = findStructure(dataSet, "h202");
                    findMainStructures(salesKey, salesOrder, dataSet, structures);
                }
                else if (dataSet.get(0).get(40).trim().toLowerCase(Locale.ROOT).contains("max756")) {
                    List<List<String>> structures = findStructure(dataSet, "max756");
                    findMainStructures(salesKey, salesOrder, dataSet, structures);
                }
                else if (dataSet.get(0).get(40).trim().toLowerCase(Locale.ROOT).contains("boomerang") ||
                         dataSet.get(0).get(40).trim().toLowerCase(Locale.ROOT).contains("bmrg")) {
                    List<List<String>> structures = findStructure(dataSet, "boomerang");
                    findMainStructures(salesKey, salesOrder, dataSet, structures);
                }
                else if (dataSet.get(0).get(40).trim().toLowerCase(Locale.ROOT).contains("arcum")) {
                    List<List<String>> structures = findStructure(dataSet, "arcum");
                    findMainStructures(salesKey, salesOrder, dataSet, structures);
                }
                else if (dataSet.get(0).get(40).trim().toLowerCase(Locale.ROOT).contains("l200")) {
                    List<List<String>> structures = findStructure(dataSet, "l200");
                    findMainStructures(salesKey, salesOrder, dataSet, structures);
                }
                else {
                    List<List<String>> structures = findStructures(dataSet);
                    findMainStructures(salesKey, salesOrder, dataSet, structures);
                }
                salesOrder.setItemLines(dataSet);
                salesOrders.add(salesOrder);
                progress += progressUpdate;
                updateProgress(progress, 1.0);
            }
            return salesOrders;
        }

        @Override
        protected void succeeded() {
            updateProgress(1.0, 1.0);
            super.succeeded();
        }

        /**
         * Logs the exception when the task transitions to the failure state
         */
        @Override
        protected void failed() {
            logger.fatal("Mapping Failed", getException());
            System.exit(-1);
        }

        private List<List<String>> findStructure(List<List<String>> dataSet, String type) {
            List<List<String>> structure = new ArrayList<>();
            for (List<String> list : dataSet) {
                if (type.equalsIgnoreCase("mts")) {
                    if ((list.get(4).trim().toLowerCase(Locale.ROOT).contains(".") ||
                         list.get(4).trim().equalsIgnoreCase("ACC-TENTNATC")
                        ) && (list.get(5).trim().toLowerCase(
                            Locale.ROOT).contains("mts") ||
                              list.get(5)
                                  .trim()
                                  .toLowerCase(Locale.ROOT)
                                  .contains("tension")
                        )) {
                        structure.add(list);
                    }
                }
                else if (type.equalsIgnoreCase("h334")) {
                    if ((list.get(4).trim().toLowerCase(Locale.ROOT).contains(".") ||
                         list.get(4).trim().equalsIgnoreCase("ACC-TENTNATC")
                        ) && (list.get(5).trim().toLowerCase(
                            Locale.ROOT).contains("h334") ||
                              list.get(5)
                                  .trim()
                                  .toLowerCase(Locale.ROOT)
                                  .contains("334")
                        )) {
                        structure.add(list);
                    }
                }
                else if (type.equalsIgnoreCase("l300")) {
                    if ((list.get(4).trim().toLowerCase(Locale.ROOT).contains(".") ||
                         list.get(4).trim().equalsIgnoreCase("ACC-TENTNATC")
                        ) &&
                        (list.get(5).trim().toLowerCase(Locale.ROOT).contains("lm300") ||
                         list.get(5).trim().toLowerCase(Locale.ROOT).contains("lm 300") ||
                         list.get(5).trim().toLowerCase(Locale.ROOT).contains("l 300") ||
                         list.get(5).trim().toLowerCase(Locale.ROOT).contains("l300") ||
                         list.get(5).trim().toLowerCase(Locale.ROOT).contains("clearspan")
                        )) {
                        structure.add(list);
                    }
                }
                else if (type.equalsIgnoreCase("h202")) {
                    if ((list.get(4).trim().toLowerCase(Locale.ROOT).contains(".") ||
                         list.get(4).trim().equalsIgnoreCase("ACC-TENTNATC")
                        ) && (list.get(5).trim().toLowerCase(Locale.ROOT).contains("h202") ||
                              list.get(5).trim().toLowerCase(Locale.ROOT).contains("h 202") ||
                              list.get(5).trim().toLowerCase(Locale.ROOT).contains("202") ||
                              list.get(5).trim().toLowerCase(Locale.ROOT).contains("clearspan")
                        )) {
                        structure.add(list);
                    }
                }
                else if (type.equalsIgnoreCase("boomerang")) {
                    if ((list.get(4).trim().toLowerCase(Locale.ROOT).contains(".") ||
                         list.get(4).trim().equalsIgnoreCase("ACC-TENTNATC")
                        ) && (list.get(5).trim().toLowerCase(Locale.ROOT).contains("boomerang")
                        )) {
                        structure.add(list);
                    }
                }
                else if (type.equalsIgnoreCase("max756")) {
                    if ((list.get(4).trim().toLowerCase(Locale.ROOT).contains(".") ||
                         list.get(4).trim().equalsIgnoreCase("ACC-TENTNATC")
                        ) && (list.get(5).trim().toLowerCase(Locale.ROOT).contains("max 756") ||
                              list.get(5).trim().toLowerCase(Locale.ROOT).contains("max756")
                        )) {
                        structure.add(list);
                    }
                }
                else if ((list.get(4).trim().toLowerCase(Locale.ROOT).contains(".") &&
                          (list.get(5).trim().toLowerCase(Locale.ROOT).contains("structure") ||
                           list.get(5).trim().toLowerCase(Locale.ROOT).contains("boomerang")
                          )
                )) {
                    structure.add(list);
                }
            }
            return structure;
        }

        private void findMainStructures(String salesKey,
                                        SalesOrder salesOrder,
                                        List<List<String>> dataSet,
                                        List<List<String>> structures
        ) {
            if (structures.size() > 0) {
                dataSet.remove(structures.get(0));
                salesOrder.setMainStructure(structures.get(0));
            }
            else {
                List<List<String>> bays = findBays(dataSet);
                if (bays.size() > 0) {
                    dataSet.remove(bays.get(0));
                    salesOrder.setMainStructure(bays.get(0));
                }
                else {
                    logger.debug("Zero Length Contract Num {}", salesKey);
                }
            }
        }

        private List<List<String>> findStructures(List<List<String>> dataSet) {
            List<List<String>> structures = new ArrayList<>();
            for (List<String> list : dataSet) {
                if ((list.get(4).trim().toLowerCase(Locale.ROOT).contains(".") &&
                     (list.get(5).trim().toLowerCase(Locale.ROOT).contains("structure") ||
                      list.get(5).trim().toLowerCase(Locale.ROOT).contains("boomerang")
                     )
                )) {
                    structures.add(list);
                }
            }
            return structures;
        }

        private List<List<String>> findBays(List<List<String>> dataSet) {
            List<List<String>> bays = new ArrayList<>();
            for (List<String> list : dataSet) {
                if ((list.get(4).trim().toLowerCase(Locale.ROOT).contains(":") &&
                     (list.get(5).trim().toLowerCase(Locale.ROOT).contains("bay")
                     )
                )) {
                    bays.add(list);
                }
            }
            return bays;
        }

        public void setData(MultiValuedMap<String, List<String>> data) {
            this.data = data;
        }
    }

    /**
     * Maps the data to the Open Sales Template
     */
    private static class MapTemplate extends Task<List<List<List<String>>>> {

        /**
         * The instance of the Utils class
         */
        private static final Utils                    utils           = Utils.getInstance();
        /**
         * The instance of the MapperUtils class
         */
        private static final MapperUtils              mapperUtils     = MapperUtils.getInstance();
        /**
         * The template associated with this mapping
         */
        private static final String                   template
                                                                      = "/templates/Open Sales Order Template_MFG FINAL.xlsx";
        /**
         * The corrections file associated with this mapping
         */
        private static final String                   correctionsFile = "./corrections/por.txt";
        private static final Logger                   logger          = LogManager.getLogger();
        /**
         * The header of the template
         */
        private final        List<String>             header;
        /**
         * The map containing the incorrect string and its correction
         */
        private final        Map<String, String>      corrections;
        /**
         * The table that stores the mapped data for store 1
         */
        private final        List<List<String>>       mapTable1;
        /**
         * The table that stores the mapped data for store 2
         */
        private final        List<List<String>>       mapTable2;
        /**
         * The table that stores the mapped data for store 3
         */
        private final        List<List<String>>       mapTable3;
        private final        List<List<String>>       mapTableAll;
        /**
         * The list of tables for every store listed in POR
         */
        private final        List<List<List<String>>> tables;
        /**
         * Local copy of the data to map
         */
        private              List<SalesOrder>         data;

        /**
         * The constructor for this inner class
         */
        private MapTemplate() {
            header = mapperUtils.getHeader(template);
            mapTable1 = mapperUtils.createMapTable(template);
            mapTable2 = mapperUtils.createMapTable(template);
            mapTable3 = mapperUtils.createMapTable(template);
            mapTableAll = mapperUtils.createMapTable(template);
            corrections = mapperUtils.getCorrections(correctionsFile);
            tables = new ArrayList<>();
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
            List<List<String>> mapTable1Temp = Collections.synchronizedList(new ArrayList<>());
            List<List<String>> mapTable2Temp = Collections.synchronizedList(new ArrayList<>());
            List<List<String>> mapTable3Temp = Collections.synchronizedList(new ArrayList<>());
            List<List<String>> mapTableAllTemp = Collections.synchronizedList(new ArrayList<>());
            AtomicReference<Double> progress = new AtomicReference<>(0.0);
            updateProgress(0, 1.0);
            double progressUpdate = 1.0 / data.size() / header.size();
            ExecutorService service = CustomExecutors.newFixedThreadPool(20);
            for (SalesOrder salesOrder : data) {
                if (salesOrder.getSalesOrderNum().equalsIgnoreCase("7865")) {
                    continue;
                }
                List<List<String>> list = new ArrayList<>(salesOrder.getItemLines());
                if (salesOrder.getMainStructure().size() > 0) {
                    list.add(0, salesOrder.getMainStructure());
                }
                Iterable<List<List<String>>> partition = Iterables.partition(list, 10);
                ExecutorService executorService = CustomExecutors.newFixedThreadPool(20);
                service.submit(() -> {
                    for (List<List<String>> dataTempString : partition) {
                        executorService.submit(() -> {
                            String[] split;
                            String dateString;
                            loopBreak:
                            for (List<String> strings : dataTempString) {
                                if (isCancelled()) {
                                    break;
                                }
                                String[] mapRow = new String[header.size()];
                                for (int j = 0; j < header.size(); j++) {
                                    if (isCancelled()) {
                                        break loopBreak;
                                    }
                                    switch (j) {
                                        case 0:
                                        case 1:
                                            mapRow[j] = strings.get(0).trim() + "@";
                                            break;
                                        case 2:
                                            mapRow[j] = strings.get(1).trim();
                                            break;
                                        case 3:
                                        case 5:
                                            mapRow[j] = strings.get(2).trim();
                                            break;
                                        case 4:
                                            if (strings.get(33).trim().equalsIgnoreCase("1")) {
                                                mapRow[j] = "Review Billing";
                                            }
                                            else {
                                                mapRow[j] = "Open";
                                            }
                                            break;
                                        case 6:
                                            dateString = strings.get(32).trim();
                                            if (dateString.contains("1899-12-30")) {
                                                mapRow[j] = strings.get(35).trim();
                                            }
                                            else {
                                                mapRow[j] = dateString;
                                            }
                                            break;
                                        case 8:
                                            if (list.indexOf(strings) == 0) {
                                                mapRow[j] = strings.get(28).trim() + "^";
                                            }
                                            else {
                                                mapRow[j] = "";
                                            }
                                            break;
                                        case 9:
                                            mapRow[j] = strings.get(3).trim();
                                            break;
                                        case 21:
                                            String itemKey = strings.get(4).trim();
                                            mapRow[j] = itemKey;
                                            break;
                                        case 22:
                                            mapRow[j] = strings.get(6).trim() + "#";
                                            break;
                                        case 25:
                                            try {
                                                double amount = Double.parseDouble(strings.get(7).trim());
                                                if (amount == 0) {
                                                    mapRow[j] = strings.get(7).trim();
                                                }
                                                else {
                                                    double qty = Double.parseDouble(strings.get(6).trim());
                                                    double result = amount / qty;
                                                    if (Double.isNaN(result) || Double.isInfinite(result)) {
                                                        logger.fatal("Divide by zero error");
                                                        System.exit(-1);
                                                    }
                                                    mapRow[j] = utils.round(result, 2) + "$";
                                                }
                                            }
                                            catch (NumberFormatException ignored) {
                                            }
                                            break;
                                        case 26:
                                            mapRow[j] = strings.get(7).trim();
                                            break;
                                        case 27:
                                            mapRow[j] = strings.get(5).trim();
                                            break;
                                        case 28:
                                        case 62:
                                            mapRow[j] = "TRUE";
                                            break;
                                        case 30:
                                            mapRow[j] = strings.get(8).trim();
                                            break;
                                        case 32:
                                            String currentStore = strings.get(9).trim();
                                            if (currentStore.equalsIgnoreCase("003")) {
                                                mapRow[j] = "Houston Depot";
                                            }
                                            else {
                                                mapRow[j] = "Memphis";
                                            }
                                            break;
                                        case 36:
                                            dateString = strings.get(10).trim();
                                            if (dateString.contains("1899-12-30")) {
                                                mapRow[j] = strings.get(2).trim();
                                            }
                                            else {
                                                mapRow[j] = dateString;
                                            }
                                            break;
                                        case 41:
                                            mapRow[j] = strings.get(14).trim();
                                            break;
                                        case 43:
                                            split = strings.get(11).trim().split(", ");
                                            mapRow[j] = split[0];
                                            try {
                                                mapRow[j + 1] = split[1];
                                            }
                                            catch (IndexOutOfBoundsException e) {
                                                mapRow[j + 1] = "";
                                            }
                                            break;
                                        case 45:
                                            split = strings.get(12).trim().split(", ");
                                            mapRow[j] = split[0];
                                            try {
                                                mapRow[j + 1] = split[1];
                                            }
                                            catch (IndexOutOfBoundsException e) {
                                                mapRow[j + 1] = "";
                                            }
                                            break;
                                        case 44:
                                        case 46:
                                        case 56:
                                        case 77:
                                            break;
                                        case 47:
                                            mapRow[j] = strings.get(13).trim();
                                            break;
                                        case 49:
                                            mapRow[j] = strings.get(15).trim();
                                            break;
                                        case 50:
                                            mapRow[j] = strings.get(16).trim();
                                            break;
                                        case 51:
                                            mapRow[j] = strings.get(17).trim();
                                            break;
                                        case 53:
                                            mapRow[j] = strings.get(19).trim();
                                            break;
                                        case 54:
                                            mapRow[j] = strings.get(20).trim();
                                            break;
                                        case 55:                                            /*Checks to see if the city and state is blank*/
                                            if (utils.isBlankString(strings.get(21))) {
                                                mapRow[j] = "";
                                                mapRow[j + 1] = "";
                                            }                                            /*Checks to see if the city and state has a correction*/
                                            else if (corrections.containsKey(strings.get(21).trim())) {
                                                split = corrections.get(strings.get(21).trim()).split(",");
                                                mapRow[j] = split[0].trim();
                                                mapRow[j + 1] = split[1].trim();
                                            }
                                            else {
                                                split = strings.get(21).split(",");
                                                mapRow[j] = split[0].trim();
                                                try {
                                                    mapRow[j + 1] = split[1].trim();
                                                }
                                                catch (ArrayIndexOutOfBoundsException e) {
                                                    logger.fatal("Array out of bound for string {}",
                                                                 strings.get(21),
                                                                 e
                                                    );
                                                    System.exit(1);
                                                }
                                            }
                                            break;
                                        case 57:
                                            if (utils.isBlankString(strings.get(23))) {
                                                mapRow[j] = strings.get(22);
                                            }
                                            else if (utils.isBlankString(strings.get(22)) &&
                                                     utils.isBlankString(strings.get(23))) {
                                                mapRow[j] = "";
                                            }
                                            else {
                                                mapRow[j] = strings.get(22) + "-" + strings.get(23);
                                            }
                                            break;
                                        case 48:
                                        case 58:
                                        case 60:
                                            mapRow[j] = "USA";
                                            break;
                                        case 59:
                                            mapRow[j] = strings.get(18).trim();
                                            break;
                                        case 61:
                                            mapRow[j] = "1#";
                                            break;
                                        case 63:
                                            String taxState = strings.get(26).trim().substring(0, 2);
                                            mapRow[j] = taxState;
                                            break;
                                        case 64:
                                            mapRow[j] = strings.get(34).trim() + "%";
                                            break;
                                        case 65:
                                        case 66:
                                        case 68:
                                            mapRow[j] = "FALSE";
                                            break;
                                        case 67:
                                            mapRow[j] = strings.get(24).trim();
                                            break;
                                        case 69:
                                            mapRow[j] = strings.get(25).trim();
                                            break;
                                        case 72:
                                            mapRow[j] = strings.get(27).trim() + "@";
                                            break;
                                        case 73:
                                            mapRow[j] = strings.get(29).trim() + "@";
                                            break;
                                        case 74:
                                            mapRow[j] = strings.get(30).trim();
                                            break;
                                        case 75:
                                            mapRow[j] = strings.get(31).trim() + "^";
                                            break;
                                        case 76:
                                            switch (strings.get(4).trim()) {
                                                case "DMGWVR":
                                                    mapRow[j] = "Damage Waiver - Monthly Billing@";
                                                    mapRow[j + 1] = "6076#";
                                                    break;
                                                case "ACC-ACCNAT-Lt":
                                                    mapRow[j] = "Lights - Monthly Billing@";
                                                    mapRow[j + 1] = "6067#";
                                                    break;
                                                case "ACC-ACCNAT-GD":
                                                    mapRow[j] = "Doors - Monthly Billing@";
                                                    mapRow[j + 1] = "6066#";
                                                    break;
                                                case "ACC-ACCNAT-ShNP":
                                                    mapRow[j] = "Tent Accessories - Monthly Billing@";
                                                    mapRow[j + 1] = "6070#";
                                                    break;
                                                case "ACCSUB":
                                                    mapRow[j] = "ACCSUB@";
                                                    mapRow[j + 1] = "9434#";
                                                    break;
                                                case "SALSUB":
                                                    mapRow[j] = "SALSUB@";
                                                    mapRow[j + 1] = "9436#";
                                                    break;
                                                case "DORSUB":
                                                    mapRow[j] = "DORSUB@";
                                                    mapRow[j + 1] = "9446#";
                                                    break;
                                                case "LAB-010":
                                                    mapRow[j] = "Hotel/Per Diem - Monthly Billing@";
                                                    mapRow[j + 1] = "6073#";
                                                    break;
                                                default:
                                                    mapRow[j] = strings.get(38).trim() + "@";
                                                    mapRow[j + 1] = strings.get(39).trim() + "#";
                                                    break;
                                            }
                                            if (strings.get(38).equalsIgnoreCase("0")) {
                                                try {
                                                    mapRow[j] = salesOrder.getMainStructure().get(38).trim() + "@";
                                                    mapRow[j + 1] = salesOrder.getMainStructure().get(39).trim() + "#";
                                                }
                                                catch (IndexOutOfBoundsException e) {
                                                    logger.debug("Sales Order to Check {}",
                                                                 salesOrder.getSalesOrderNum()
                                                    );
                                                }
                                            }
                                            break;
                                        case 78:
                                            mapRow[j] = strings.get(37).trim() + "@";
                                            break;
                                        default:
                                            mapRow[j] = "";
                                            break;
                                    }
                                    progress.updateAndGet(v -> v + progressUpdate);
                                    updateProgress(progress.get(), 1.0);
                                }
                                switch (strings.get(36).trim()) {
                                    case "001":
                                        mapTable1Temp.add(new ArrayList<>(Arrays.asList(mapRow)));
                                        break;
                                    case "002":
                                        mapTable2Temp.add(new ArrayList<>(Arrays.asList(mapRow)));
                                        break;
                                    case "003":
                                        mapTable3Temp.add(new ArrayList<>(Arrays.asList(mapRow)));
                                        break;
                                }
                                mapTableAllTemp.add(new ArrayList<>(Arrays.asList(mapRow)));
                                utils.sleep(1);
                            }
                            return null;
                        });
                    }
                    utils.shutdownExecutor(executorService, logger);
                    return null;
                });
            }
            utils.shutdownExecutor(service, logger);
            mapTable1.addAll(utils.parallelSortListAscending(mapTable1Temp, 0));
            mapTable2.addAll(utils.parallelSortListAscending(mapTable2Temp, 0));
            mapTable3.addAll(utils.parallelSortListAscending(mapTable3Temp, 0));
            mapTableAll.addAll(utils.parallelSortListAscending(mapTableAllTemp, 0));
            tables.add(mapTable1);
            tables.add(mapTable2);
            tables.add(mapTable3);
            tables.add(mapTableAll);
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

        public List<String> getHeader() {
            return header;
        }

        /**
         * Sets the data to map
         *
         * @param data The data to map
         */
        public void setData(List<SalesOrder> data) {
            this.data = new ArrayList<>(data);
        }
    }


    private static class GroupSalesOrders extends Task<MultiValuedMap<String, List<String>>> {

        private List<List<String>> data;
        private boolean            hasHeader = true;

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
        protected MultiValuedMap<String, List<String>> call() throws Exception {
            final MultiValuedMap<String, List<String>> groupedMap = new ArrayListValuedHashMap<>();
            double progress = 0.0;
            updateProgress(progress, 1.0);
            if (hasHeader) {
                data.remove(0);
            }
            double progressUpdate = 1.0 / data.size();
            for (List<String> list : data) {
                if (isCancelled()) {
                    break;
                }
                groupedMap.put(list.get(0), list);
                progress += progressUpdate;
                updateProgress(progress, 1.0);
                utils.sleep(1);
            }
            return groupedMap;
        }

        public void setData(List<List<String>> data) {
            this.data = new ArrayList<>(data);
        }

        public void setHasHeader(boolean hasHeader) {
            this.hasHeader = hasHeader;
        }
    }

    private static class FilterSubassemblies extends Task<MultiValuedMap<String, List<String>>> {

        private MultiValuedMap<String, List<String>> data;
        private Map<String, Subassembly>             kits;

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
        protected MultiValuedMap<String, List<String>> call() throws Exception {
            double progress = 0.0;
            updateProgress(progress, 1.0);
            double progressUpdate = 1.0 / data.keySet().size();
            MultiValuedMap<String, List<String>> filteredTable = new ArrayListValuedHashMap<>();
            for (String salesOrder : data.keySet()) {
                if (isCancelled()) {
                    break;
                }
                List<List<String>> tempTable = new ArrayList<>(data.get(salesOrder));
                List<List<String>> filteredKits = filterKits(tempTable);
                for (List<String> filteredKit : filteredKits) {
                    String assemblyKey = filteredKit.get(21);
                    removeComponents(tempTable, kits.get(assemblyKey));
                }
                filteredTable.putAll(salesOrder, tempTable);
                progress += progressUpdate;
                updateProgress(progress, 1.0);
                utils.sleep(1);
            }
            return filteredTable;
        }

        @Override
        protected void failed() {
            logger.fatal("removing failed", getException());
            System.exit(1);
        }

        private List<List<String>> filterKits(List<List<String>> table) {
            List<List<String>> filtered = new ArrayList<>();
            for (List<String> row : table) {
                String itemKey = row.get(21);
                if (kits.containsKey(itemKey)) {
                    filtered.add(row);
                }
            }
            return filtered;
        }

        private void removeComponents(List<List<String>> tempTable, Subassembly subassembly) {
            for (AssemblyItem assemblyItem : subassembly.getAssemblyItems()) {
                if (isCancelled()) {
                    break;
                }
                if (assemblyItem.isSubassembly()) {
                    removeItem(tempTable, assemblyItem);
                    removeComponents(tempTable, kits.get(assemblyItem.getItemKey()));
                }
                else {
                    removeItem(tempTable, assemblyItem);
                }
            }
        }

        private void removeItem(List<List<String>> tempTable, AssemblyItem assemblyItem) {
            for (Iterator<List<String>> iterator = tempTable.iterator(); iterator.hasNext(); ) {
                List<String> strings = iterator.next();
                if (isCancelled()) {
                    break;
                }
                if (strings.get(21).toLowerCase().contains(assemblyItem.getItemKey().toLowerCase())) {
                    iterator.remove();
                }
            }
        }

        public void setData(MultiValuedMap<String, List<String>> data) {
            this.data = new ArrayListValuedHashMap<>(data);
        }

        public void setKits(Map<String, Subassembly> kits) {
            this.kits = new HashMap<>(kits);
        }
    }

    private static class BreakoutSubassemblies extends Task<MultiValuedMap<String, List<String>>> {

        private MultiValuedMap<String, List<String>> data;
        private Map<String, Subassembly>             kits;

        // Row format | contractNum | parentKey | itemKey | itemName | qty| subAssemblyTotalQty
        private void breakoutRecurse(MultiValuedMap<String, List<String>> breakoutList,
                                     Subassembly subassembly,
                                     String contractNum,
                                     double qty,
                                     Map<String, Double> grandTotal,
                                     String parentKey
        ) {
            for (AssemblyItem assemblyItem : subassembly.getAssemblyItems()) {
                if (isCancelled()) {
                    break;
                }
                List<String> row = new ArrayList<>();
                row.add(contractNum);
                row.add(parentKey);
                row.add(subassembly.getAssemblyKey());
                row.add(assemblyItem.getItemKey());
                row.add(assemblyItem.getName());
                row.add(assemblyItem.getQty() + "#");
                row.add((assemblyItem.getQty() * qty) + "#");
                breakoutList.put(contractNum, row);
                if (assemblyItem.isSubassembly()) {
                    Subassembly subassembly1 = kits.get(assemblyItem.getItemKey());
                    breakoutRecurse(breakoutList,
                                    subassembly1,
                                    contractNum,
                                    qty * assemblyItem.getQty(),
                                    grandTotal,
                                    parentKey
                    );
                }
                else {
                    if (grandTotal.containsKey(assemblyItem.getItemKey())) {
                        double total =
                                grandTotal.get(assemblyItem.getItemKey()) + (qty * assemblyItem.getQty());
                        grandTotal.put(assemblyItem.getItemKey(), total);
                    }
                    else {
                        grandTotal.put(assemblyItem.getItemKey(), qty * assemblyItem.getQty());
                    }
                }
            }
        }

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
        protected MultiValuedMap<String, List<String>> call() throws Exception {
            MultiValuedMap<String, List<String>> breakout = new ArrayListValuedHashMap<>();
            double progress = 0.0;
            updateProgress(progress, 1.0);
            double progressUpdate = 1.0 / data.keySet().size();
            int headerSize = 7;
            loopBreak:
            for (String key : data.keySet()) {
                if (isCancelled()) {
                    break;
                }
                List<List<String>> contractTable = new ArrayList<>(data.get(key));
                Map<String, Double> grandTotal = new TreeMap<>();
                for (List<String> row : contractTable) {
                    if (isCancelled()) {
                        break loopBreak;
                    }
                    String itemKey = row.get(21).trim();
                    if (kits.containsKey(itemKey)) {
                        Subassembly subassembly = kits.get(itemKey);
                        List<String> row1 = new ArrayList<>();
                        row1.add(key);
                        row1.add(subassembly.getAssemblyKey());
                        row1.add("");
                        row1.add(subassembly.getAssemblyKey());
                        row1.add(subassembly.getName());
                        row1.add(row.get(22));
                        row1.add(row.get(22));
                        breakout.put(key, row1);
                        breakoutRecurse(breakout,
                                        subassembly,
                                        key,
                                        Double.parseDouble(row.get(22).substring(0, row.get(22).length() - 1)),
                                        grandTotal,
                                        subassembly.getAssemblyKey()
                        );
                    }
                }
                if (!grandTotal.isEmpty()) {
                    List<String> row = new ArrayList<>();
                    row.add(key);
                    row.add("Totals");
                    row.addAll(utils.createEmptyRow(headerSize - 2));
                    breakout.put(key, row);
                    for (String s : grandTotal.keySet()) {
                        List<String> totalRow = new ArrayList<>();
                        totalRow.add(key);
                        totalRow.add(s);
                        totalRow.add(grandTotal.get(s) + "#");
                        totalRow.addAll(utils.createEmptyRow(headerSize - 3));
                        breakout.put(key, totalRow);
                    }
                }
                progress += progressUpdate;
                updateProgress(progress, 1.0);
                utils.sleep(1);
            }
            return breakout;
        }

        public void setData(MultiValuedMap<String, List<String>> data) {
            this.data = new ArrayListValuedHashMap<>(data);
        }

        public void setKits(Map<String, Subassembly> kits) {
            this.kits = new HashMap<>(kits);
        }
    }
}
    