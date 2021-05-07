package org.dataprocessing.backend.mappers.por;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dataprocessing.backend.tasks.ServerTableConvertTask;
import org.dataprocessing.gui.model.PorModel;
import org.dataprocessing.utils.Alerts;
import org.dataprocessing.utils.FileUtils;
import org.dataprocessing.utils.MapperUtils;
import org.dataprocessing.utils.Utils;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Maps the POR Customer Template
 *
 * @author Nicholas Curl
 */
public class PORCustomer {

    /**
     * The instance of the logger
     */
    private static final Logger                 logger          = LogManager.getLogger();
    /**
     * The instance of the Utils class
     */
    private static final Utils                  utils           = Utils.getInstance();
    /**
     * The instance of the MapperUtils class
     */
    private static final MapperUtils            mapperUtils     = MapperUtils.getInstance();
    /**
     * The instance of the Alerts class
     */
    private static final Alerts                 alerts          = Alerts.getInstance();
    /**
     * The instance of the FileUtils class
     */
    private static final FileUtils              fileUtils       = FileUtils.getInstance();
    /**
     * The template associated with this mapping
     */
    private static final String                 template        = "/templates/Customer Template_MFG FINAL.xlsx";
    /**
     * The corrections file associated with this mapping
     */
    private static final String                 correctionsFile = "./corrections/por.txt";
    /**
     * The instance of the MapCallLog sub-class that maps the CRM Call Log
     */
    private static final MapCallLog             mapCallLog      = new MapCallLog();
    /**
     * The instance of the POR Model
     */
    private static final PorModel               model           = PorModel.getInstance();
    /**
     * The header of the template
     */
    private final        List<String>           header;
    /**
     * The table that stores the mapped data
     */
    private final        List<List<String>>     mapTable;
    /**
     * The overall progress of this task
     */
    private final        DoubleBinding          overallTaskProgress;
    /**
     * The first server table convert task
     */
    private final        ServerTableConvertTask tableConvertTask;
    /**
     * The second server table convert task
     */
    private final        ServerTableConvertTask tableConvertTask1;
    /**
     * The progress of the Customer File table mapping
     */
    private final        DoubleProperty         databaseMapProgress;
    /**
     * The instance of the SetSalesReps sub-class that sets the sales reps in the table of mapped data
     */
    private final        SetSalesReps           setSalesReps;
    /**
     * The task to write the excel spreadsheet
     */
    private final        FileUtils.XlsxTask     writeTask;
    /**
     * The map containing the incorrect string and its correction
     */
    private final        Map<String, String>    corrections;
    /**
     * The list of sub-tasks for this task
     */
    private final        List<Task<?>>          tasks;


    /**
     * The constructor for this class
     *
     * @param porStoreLocation The path to the directory to store the mapped data
     */
    public PORCustomer(Path porStoreLocation) {
        header = mapperUtils.getHeader(template);
        mapTable = mapperUtils.createMapTable(template);
        setSalesReps = new SetSalesReps();
        databaseMapProgress = new SimpleDoubleProperty(0.0);
        tableConvertTask = new ServerTableConvertTask(
                "SELECT CF.NAME,\n" +
                "       CF.Address,\n" +
                "       CF.Address2,\n" +
                "       CF.City,\n" +
                "       CF.Zip,\n" +
                "       CF.ZIP4,\n" +
                "       CF.Phone,\n" +
                "       CF.WORK,\n" +
                "       CF.MOBILE,\n" +
                "       CF.FAX,\n" +
                "       CF.CNUM,\n" +
                "       CF.CreditLimit,\n" +
                "       CS.Description AS Status,\n" +
                "       CF.Email,\n" +
                "       CF.BillContact,\n" +
                "       CF.BillPhone,\n" +
                "       CF.BillAddress1,\n" +
                "       CF.BillAddress2,\n" +
                "       CF.BillCityState,\n" +
                "       CF.BillZip,\n" +
                "       CF.BillZip4,\n" +
                "       CF.TaxExemptNumber,\n" +
                "       CF.TaxExemptExpire,\n" +
                "       CF.InsuranceNumber,\n" +
                "       CF.InsuranceExpire,\n" +
                "       CF.Terms,\n" +
                "       CF.CustomerPrintOut,\n" +
                "       CF.Nontaxable\n" +
                "FROM CustomerFile CF\n" +
                "         LEFT OUTER JOIN CustomerStatus CS on CF.Status = CS.Status"
        );
        tableConvertTask1 = new ServerTableConvertTask(
                "SELECT CallLog.CNUM AS CustomerNo, Salesman.Name AS SalesRep\n" +
                "FROM CallLog\n" +
                "         LEFT JOIN OperatorId ON OperatorId.OPNO = CallLog.Opr\n" +
                "         LEFT JOIN CustomerFile ON CustomerFile.CNUM = CallLog.CNUM\n" +
                "         LEFT JOIN Salesman ON OperatorId.OPNO = Salesman.OperatorNo"
        );
        writeTask = fileUtils.writeXlsxTask(porStoreLocation.resolve("Customer Template.xlsx").toFile());
        corrections = mapperUtils.getCorrections(correctionsFile);
        tasks = new ArrayList<>();
        tasks.add(tableConvertTask);
        tasks.add(tableConvertTask1);
        tasks.add(mapCallLog);
        tasks.add(setSalesReps);
        tasks.add(writeTask);
        overallTaskProgress = Bindings.createDoubleBinding(() -> (
                                                                         Math.max(0, tableConvertTask.getProgress()) +
                                                                         Math.max(0, tableConvertTask1.getProgress()) +
                                                                         Math.max(0, databaseMapProgress.get()) +
                                                                         Math.max(0, mapCallLog.getProgress()) +
                                                                         Math.max(0, setSalesReps.getProgress()) +
                                                                         Math.max(0, writeTask.getProgress())
                                                                 ) / 6,
                                                           tableConvertTask.progressProperty(),
                                                           tableConvertTask1.progressProperty(),
                                                           databaseMapProgress,
                                                           mapCallLog.progressProperty(),
                                                           setSalesReps.progressProperty(),
                                                           writeTask.progressProperty()
        );
    }

    /**
     * Gets the value of the task's overall progress
     *
     * @return The value the task's overall progress
     */
    public double getOverallTaskProgress() {
        return overallTaskProgress.get();
    }

    /**
     * Gets the tasks that are created in the overall task
     *
     * @return The list of tasks
     */
    public List<Task<?>> getTasks() {
        return tasks;
    }

    /**
     * Maps the Customer File and CRM Call Log
     *
     * @param executorService The controller thread executor
     */
    public void map(ExecutorService executorService) {
        tableConvertTask1.setOnSucceeded(event -> {
            List<List<String>> value = utils.convertToTableString(tableConvertTask1.getValue());
            mapCallLog.setCallLog(value);
            mapDatabase(value, executorService);
        });
        mapCallLog.setOnSucceeded(event -> {
            setSalesReps.setSalesReps(mapCallLog.getValue());
            executorService.submit(setSalesReps);
        });
        setSalesReps.setOnSucceeded(event -> {
            mapTable.sort((o1, o2) -> {
                              if (mapTable.indexOf(o1) == 0) {
                                  return -1;
                              }
                              else if (mapTable.indexOf(o2) == 0) {
                                  return 1;
                              }
                              else {
                                  return o1.get(8).compareTo(o2.get(8));
                              }
                          }
            );
            writeTask.setTable(mapTable);
            executorService.submit(writeTask);
        });
        executorService.submit(tableConvertTask);
        executorService.submit(tableConvertTask1);
    }

    /**
     * Maps the Customer File Table
     *
     * @param database        The table containing the Customer File server table
     * @param executorService The controller thread executor
     */
    private void mapDatabase(List<List<String>> database, ExecutorService executorService) {
        Map<Integer, Integer> customerNums = new HashMap<>();
        String[] split;
        double localProgress = 0.0;
        double localProgressUpdate = 1.0 / (database.size() - 1) / header.size();
        loopBreak:
        for (int i = 1; i < database.size(); i++) {
            if (model.isCanceled()) {
                break;
            }
            List<String> row = database.get(i);
            List<String> mapRow = new ArrayList<>();
            for (int j = 0; j < header.size(); j++) {
                if (model.isCanceled()) {
                    break loopBreak;
                }
                switch (j) {
                    case 1:
                        mapRow.add(j, row.get(10) + "#");
                        break;
                    case 2:
                    case 36:
                    case 47:
                    case 48:
                        mapRow.add(j, "FALSE");
                        break;
                    case 8:
                        mapRow.add(j, row.get(0).toUpperCase(Locale.ROOT));
                        break;
                    case 10:
                        mapRow.add(j, row.get(12));
                        break;
                    case 11:
                        mapRow.add(j, "Mahaffey USA");
                        break;
                    case 12:
                        try {
                            customerNums.put(Integer.parseInt(row.get(10)), i);
                            mapRow.add(j, "");
                        }
                        catch (NumberFormatException e) {
                            logger.fatal("Unable to grab customer number.", e);
                            System.exit(-1);
                        }
                        break;
                    case 18:
                        mapRow.add(j, row.get(13));
                        break;
                    case 20:
                        mapRow.add(j, row.get(6));
                        break;
                    case 21:
                        mapRow.add(j, row.get(7));
                        break;
                    case 22:
                        mapRow.add(j, row.get(8));
                        break;
                    case 24:
                        mapRow.add(j, row.get(9));
                        break;
                    case 29:
                        mapRow.add(j, row.get(1));
                        break;
                    case 30:
                        mapRow.add(j, row.get(2));
                        break;
                    case 31:
                        //Checks to see if the city and state is blank
                        if (utils.isBlankString(row.get(3))) {
                            mapRow.add(j, "");
                            mapRow.add(j + 1, "");
                        }
                        //Checks to see if the city and state has a correction
                        else if (corrections.containsKey(row.get(3).trim())) {
                            split = corrections.get(row.get(3).trim()).split(",");
                            mapRow.add(j, split[0].trim());
                            mapRow.add(j + 1, split[1].trim());
                        }
                        else {
                            split = row.get(3).split(",");
                            //Checks to see if the proper format exists, if not prompts the user to fix the format
                            if (split.length < 2) {
                                String correction = alerts.stringFormatPrompt(
                                        "Please fix the format shown in the prompt",
                                        "(format should be City, State)",
                                        row.get(3).trim(),
                                        row.get(0).trim()
                                );
                                corrections.put(row.get(3).trim(), correction);
                                String[] correctionSplit = correction.split(",");
                                mapRow.add(j, correctionSplit[0].trim());
                                mapRow.add(j + 1, correctionSplit[1].trim());
                            }
                            else {
                                mapRow.add(j, split[0].trim());
                                mapRow.add(j + 1, split[1].trim());
                            }
                        }
                        break;
                    case 32:
                    case 44:
                        break;
                    case 33:
                        //formats the zipcode
                        if (utils.isBlankString(row.get(5))) {
                            mapRow.add(j, row.get(4));
                        }
                        else if (utils.isBlankString(row.get(4)) && utils.isBlankString(row.get(5))) {
                            mapRow.add(j, "");
                        }
                        else {
                            mapRow.add(j, row.get(4) + "-" + row.get(5));
                        }
                        break;
                    case 34:
                    case 46:
                        mapRow.add(j, "United States");
                        break;
                    case 35:
                        mapRow.add(j, "TRUE");
                        break;
                    case 38:
                        mapRow.add(j, row.get(14));
                        break;
                    case 40:
                        mapRow.add(j, row.get(15));
                        break;
                    case 41:
                        mapRow.add(j, row.get(16));
                        break;
                    case 42:
                        mapRow.add(j, row.get(17));
                        break;
                    case 43:
                        //Checks to see if the city and state is blank
                        if (utils.isBlankString(row.get(18))) {
                            mapRow.add(j, "");
                            mapRow.add(j + 1, "");
                        }
                        //Checks to see if the city and state has a correction
                        else if (corrections.containsKey(row.get(18).trim())) {
                            split = corrections.get(row.get(18).trim()).split(",");
                            mapRow.add(j, split[0].trim());
                            mapRow.add(j + 1, split[1].trim());
                        }
                        else {
                            split = row.get(18).split(",");
                            //Checks to see if the proper format exists, if not prompts the user to fix the format
                            if (split.length < 2) {
                                String correction = alerts.stringFormatPrompt(
                                        "Please fix the format shown in the prompt",
                                        "(format should be City, State)",
                                        row.get(18).trim(),
                                        row.get(0).trim()
                                );
                                corrections.put(row.get(18).trim(), correction);
                                String[] correctionSplit = correction.split(",");
                                mapRow.add(j, correctionSplit[0].trim());
                                mapRow.add(j + 1, correctionSplit[1].trim());
                            }
                            else {
                                mapRow.add(j, split[0].trim());
                                mapRow.add(j + 1, split[1].trim());
                            }
                        }
                        break;
                    case 45:
                        //formats the zipcode
                        if (utils.isBlankString(row.get(20))) {
                            mapRow.add(j, row.get(19));
                        }
                        else if (utils.isBlankString(row.get(19)) && utils.isBlankString(row.get(20))) {
                            mapRow.add(j, "");
                        }
                        else {
                            mapRow.add(j, row.get(19) + "-" + row.get(20));
                        }
                        break;
                    case 64:
                        mapRow.add(j, "USA");
                        break;
                    case 65:
                        mapRow.add(j, row.get(25));
                        break;
                    case 66:
                        mapRow.add(j, row.get(11));
                        break;
                    case 70:
                        String tempStr = row.get(27);
                        if (tempStr.equalsIgnoreCase("FALSE")) {
                            mapRow.add(j, "TRUE");
                        }
                        else if (tempStr.equalsIgnoreCase("TRUE")) {
                            mapRow.add(j, "FALSE");
                        }
                        else {
                            mapRow.add(j, "");
                        }
                        break;
                    case 72:
                        mapRow.add(j, row.get(21) + "@");
                        break;
                    case 76:
                        mapRow.add(j, "PDF");
                        break;
                    case 77:
                        mapRow.add(j, row.get(0));
                        break;
                    case 98:
                        mapRow.add(j, row.get(23) + "@");
                        break;
                    case 99:
                        mapRow.add(j, row.get(24));
                        break;
                    case 100:
                        mapRow.add(j, row.get(22));
                        break;
                    default:
                        mapRow.add(j, "");
                        break;
                }
                localProgress += localProgressUpdate;
                databaseMapProgress.set(localProgress);
            }
            mapTable.add(mapRow);
            utils.sleep(1);
        }
        mapperUtils.writeCorrections(corrections, correctionsFile);
        if (!model.isCanceled()) {
            setSalesReps.setCustomerNums(customerNums);
            executorService.submit(mapCallLog);
        }
    }

    /**
     * Gets the task's overall progress property
     *
     * @return The task's overall progress property
     */
    public DoubleBinding overallTaskProgressProperty() {
        return overallTaskProgress;
    }

    /**
     * The sub-class that maps the CRM Call Log
     */
    private static class MapCallLog extends Task<Map<Integer, String>> {

        /**
         * Local copy of the CRM Call Log
         */
        private List<List<String>> callLog;

        /**
         * Maps the CRM Call Log
         *
         * @return The map of the names of the sales reps and the customer they are associated with
         *
         * @throws Exception Any exception that might occur when executing this task
         */
        @Override
        protected Map<Integer, String> call() throws Exception {
            Map<Integer, String> salesReps = new HashMap<>();
            double localProgress = 0.0;
            double localProgressUpdate = 1.0 / callLog.size();
            updateProgress(0.0, 1.0);
            MultiValuedMap<Integer, String> customerReps = new ArrayListValuedHashMap<>();
            for (int i = 1; i < callLog.size(); i++) {
                if (isCancelled()) {
                    break;
                }
                List<String> row = callLog.get(i);
                customerReps.put(Integer.parseInt(row.get(0)), row.get(1));
                localProgress += localProgressUpdate;
                updateProgress(localProgress, 1.0);
                utils.sleep(1);
            }
            for (int customerNum : customerReps.keySet()) {
                if (isCancelled()) {
                    break;
                }
                List<String> repsTemp = (List<String>) customerReps.get(customerNum);
                Set<String> reps = repsTemp.stream().filter(e -> !utils.isBlankString(e)).collect(Collectors.toSet());
                String allReps = String.join(", ", reps);
                if (!utils.isBlankString(allReps)) {
                    salesReps.put(customerNum, allReps);
                }
                utils.sleep(1);
            }
            return salesReps;
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
         * Sets the local copy of the CRM Call Log
         *
         * @param callLog The CRM Call Log
         */
        private void setCallLog(List<List<String>> callLog) {
            this.callLog = callLog;
        }
    }


    /**
     * The sub-class that sets the sales reps column in the table of mapped data
     */
    private class SetSalesReps extends Task<Void> {

        /**
         * Local copy of the salesReps map
         */
        private Map<Integer, String>  salesReps;
        /**
         * Local copy of the customerNums map
         */
        private Map<Integer, Integer> customerNums;

        /**
         * Sets the sales reps column in the table of mapped data
         *
         * @return null
         *
         * @throws Exception Any exception that might occur when executing this task
         */
        @Override
        protected Void call() throws Exception {
            double localProgress = 0.0;
            double localProgressUpdate = 1.0 / customerNums.size();
            for (int customerNum : customerNums.keySet()) {
                if (isCancelled()) {
                    break;
                }
                int rowNum = customerNums.get(customerNum);
                List<String> row = mapTable.get(rowNum);
                row.set(12, salesReps.getOrDefault(customerNum, "Unassigned"));
                localProgress += localProgressUpdate;
                updateProgress(localProgress, 1.0);
                utils.sleep(1);
            }
            return null;
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
         * Sets the local copy of customerNums
         *
         * @param customerNums The customerNums map
         */
        private void setCustomerNums(Map<Integer, Integer> customerNums) {
            this.customerNums = customerNums;
        }

        /**
         * Sets the local copy of the salesReps
         *
         * @param salesReps The salesReps map
         */
        private void setSalesReps(Map<Integer, String> salesReps) {
            this.salesReps = salesReps;
        }
    }

}
