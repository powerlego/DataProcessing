package org.dataprocessing.backend.mappers.por;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dataprocessing.backend.database.SqlServer;
import org.dataprocessing.backend.tasks.RemoveDuplicates;
import org.dataprocessing.backend.tasks.RemoveNonAlphaNum;
import org.dataprocessing.backend.tasks.ServerTableConvertTask;
import org.dataprocessing.utils.FileUtils;
import org.dataprocessing.utils.MapperUtils;
import org.dataprocessing.utils.Utils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Maps the POR Item Master Template @author Nicholas Curl
 */
public class PORItemMaster {

    /**
     * The instance of the logger
     */
    private static final Logger                 logger    = LogManager.getLogger();
    /**
     * The instance of the FileUtils class
     */
    private static final FileUtils              fileUtils = FileUtils.getInstance();
    private final        Utils                  utils     = Utils.getInstance();
    /**
     * The server table convert task
     */
    private final        ServerTableConvertTask tableConvertTask;
    /**
     * The remove duplicates task
     */
    private final        RemoveDuplicates       removeDuplicates;
    /**
     * The remove non-alphanumeric task
     */
    private final        RemoveNonAlphaNum      removeNonAlphaNum;
    /**
     * The template mapping task
     */
    private final        MapTemplate            mapTemplate;
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
     * The constructor for this class @param porStoreLocation The path to the directory to store the mapped data
     */
    public PORItemMaster(Path porStoreLocation) {
        tasks = new ArrayList<>();
        mapTemplate = new MapTemplate();
        tableConvertTask = new ServerTableConvertTask(
                "SELECT A.[KEY],\n" +
                "                A.Name,\n" +
                "                E.NewDepartmentName As Department,\n" +
                "                A.QTY               AS Quantity,\n" +
                "                A.MANF              AS Manufacturer,\n" +
                "                A.RMIN              AS [Reorder Min],\n" +
                "                A.VendorNumber1,\n" +
                "                A.VendorNumber2,\n" +
                "                A.VendorNumber3,\n" +
                "                A.Weight,\n" +
                "                A.PURP              AS [Purchase Price],\n" +
                "                A.SELL              AS [Sell Price],\n" +
                "                A.CurrentStore,\n" +
                "                F.STORE_NAME,\n" +
                "                B.Name              AS Category,\n" +
                "                A.QYOT              AS Quantity_Rented,\n" +
                "                N.AccountNum          AS Income_Account_Num,\n" +
                "                N.AccountName       AS Income_Account_Name,\n" +
                "                G.AccountNum        AS COGS_Account_Num,\n" +
                "                G.AccountName       AS COGS_Account_Name,\n" +
                "                A.ReplacementCost,\n" +
                "                12100               as InvAsset,\n" +
                "                'Inventory Asset'   as InvAssetName,\n" +
                "                213                 as Inventory_Asset_Internal_Id,\n" +
                "                N.InternalId        as Income_Internal_ID,\n" +
                "                G.InternalId        as COGS_Internal_ID\n" +
                "\n" +
                "FROM (SELECT *\n" +
                "      FROM dbo.ItemFile\n" +
                "      WHERE [KEY] NOT LIKE '.%'\n" +
                "        AND [KEY] NOT LIKE ':%'\n" +
                "        AND Inactive = 'false'\n" +
                "        AND Name NOT LIKE 'Custom Accessory%'\n" +
                "        AND [KEY] NOT LIKE '%PKG'\n" +
                "        AND NOT ((NOT (Department = 13 OR Department = 7 OR Department = 10)) AND CurrentStore = '003')\n" +
                "        AND [KEY] NOT LIKE 'Y%'\n" +
                "     ) AS A\n" +
                "         INNER JOIN (SELECT [IF*].Name as JoinName, MAX([IF*].QTY) as JoinQTY\n" +
                "                     from (SELECT *\n" +
                "                           FROM dbo.ItemFile\n" +
                "                           WHERE [KEY] NOT LIKE '.%'\n" +
                "                             AND [KEY] NOT LIKE ':%'\n" +
                "                             AND Inactive = 'false'\n" +
                "                             AND Name NOT LIKE 'Custom Accessory%'\n" +
                "                             AND [KEY] NOT LIKE '%PKG'\n" +
                "                             AND NOT ((NOT (Department = 13 OR Department = 7 OR Department = 10)) AND\n" +
                "                                      CurrentStore = '003')) as [IF*]\n" +
                "                     group by IIF([IF*].Name NOT like '%Berry%', [IF*].Name, CAST([IF*].QTY as NVARCHAR)), [IF*].Name\n" +
                ") as H on A.Name = JoinName and A.QTY = H.JoinQTY\n" +
                "         LEFT OUTER JOIN dbo.ItemCategory AS B ON A.Category = B.Category\n" +
                "         LEFT OUTER JOIN dbo.ItemType AS C ON A.TYPE = C.Type\n" +
                "         LEFT OUTER JOIN dbo.DepartmentMapping AS E ON A.Department = E.OldDepartment\n" +
                "         LEFT OUTER JOIN dbo.ParameterFile AS F ON A.CurrentStore = F.Store\n" +
                "         LEFT OUTER JOIN (select NAM.OldAccountNum, NAM.InternalId, NA.AccountNum, NA.AccountName\n" +
                "                          from NewAccountMap as NAM\n" +
                "                                   INNER JOIN NewAccounts NA on NAM.InternalId = NA.InternalId) AS N\n" +
                "                         on N.OldAccountNum = B.GLNumber\n" +
                "         LEFT OUTER JOIN (SELECT DepartmentNum, NA.InternalId, NA.AccountNum, NA.AccountName\n" +
                "                          from COGS_Association as CA\n" +
                "                                   INNER JOIN NewAccounts NA on CA.IternalID = NA.InternalId) AS G\n" +
                "                         ON E.NewDepartment = G.DepartmentNum\n" +
                "         LEFT OUTER JOIN dbo.NewAccounts AS D ON B.GLNumber = D.AccountNum"
        );
        removeDuplicates = new RemoveDuplicates();
        removeNonAlphaNum = new RemoveNonAlphaNum();
        writeTask = fileUtils.writeXlsxTask(porStoreLocation.resolve("Inventory Item Template.xlsx").toFile());
        tasks.add(tableConvertTask);
        tasks.add(removeDuplicates);
        tasks.add(removeNonAlphaNum);
        tasks.add(mapTemplate);
        tasks.add(writeTask);
        totalProgress = Bindings.createDoubleBinding(() -> (Math.max(0, tableConvertTask.getProgress()) +
                                                            Math.max(0, removeDuplicates.getProgress()) +
                                                            Math.max(0, removeNonAlphaNum.getProgress()) +
                                                            Math.max(0, mapTemplate.getProgress()) +
                                                            Math.max(0, writeTask.getProgress())
                                                           ) / 5,
                                                     tableConvertTask.progressProperty(),
                                                     removeDuplicates.progressProperty(),
                                                     removeNonAlphaNum.progressProperty(),
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
     * Maps the template @param executorService The controller thread executor
     */
    public void map(ExecutorService executorService) {
        tableConvertTask.setOnSucceeded(event -> {
            removeDuplicates.setTable(utils.convertToTableString(tableConvertTask.getValue()));
            executorService.submit(removeDuplicates);
        });
        removeDuplicates.setOnSucceeded(event -> {
            removeNonAlphaNum.setTable(removeDuplicates.getValue());
            executorService.submit(removeNonAlphaNum);
        });
        removeNonAlphaNum.setOnSucceeded(event -> {
            mapTemplate.setData(removeNonAlphaNum.getValue());
            executorService.submit(mapTemplate);
        });
        mapTemplate.setOnSucceeded(event -> {
            writeTask.setTable(mapTemplate.getValue());
            executorService.submit(writeTask);
        });
        executorService.submit(tableConvertTask);
    }

    /**
     * Gets the total progress property @return The total progress property
     */
    public DoubleBinding totalProgressProperty() {
        return totalProgress;
    }

    /**
     * Maps the data to the Item Master Template
     */
    private static class MapTemplate extends Task<List<List<String>>> {

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
        private static final String             template    = "/templates/Inventory Item Template_MFG FINAL.xlsx";
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
         * Maps the Item Master Template
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
                List<String> mapRow = new ArrayList<>();
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
                            mapRow.add(j, row.get(1).trim() + "@");
                            break;
                        case 4:
                        case 5:
                        case 6:
                        case 7:
                            mapRow.add(j, "Each");
                            break;
                        case 9:
                            mapRow.add(j, "Mahaffey USA");
                            break;
                        case 10:
                        case 14:
                        case 112:
                            mapRow.add(j, "TRUE");
                            break;
                        case 11:
                            mapRow.add(j, row.get(2).trim());
                            break;
                        case 13:
                            String currentStore = row.get(12).trim();
                            if (currentStore.equalsIgnoreCase("003")) {
                                mapRow.add(j, "Houston Depot");
                            }
                            else if (row.get(2).trim().equalsIgnoreCase("Fort Polk")) {
                                mapRow.add(j, "Fort Polk");
                            }
                            else {
                                mapRow.add(j, "Memphis");
                            }
                            break;
                        case 15:
                            mapRow.add(j, "Standard");
                            break;
                        case 17:
                            mapRow.add(j, row.get(10).trim());
                            break;
                        case 23:
                            mapRow.add(j, "Reorder Point");
                            break;
                        case 28:
                            mapRow.add(j, "Auto-Calculating");
                            break;
                        case 36:
                            mapRow.add(j, row.get(4).trim());
                            break;
                        case 48:
                            if (row.get(6).trim().equalsIgnoreCase("0") || row.get(6).trim().equalsIgnoreCase("-1")) {
                                mapRow.add(j, "");
                            }
                            else {
                                try {
                                    int vendNum = Integer.parseInt(row.get(6).trim());
                                    String vendName = server.getVendorName(vendNum);
                                    if (vendName == null) {
                                        logger.fatal("Vendor name must not be null.", new NullPointerException());
                                        System.exit(-1);
                                    }
                                    else {
                                        mapRow.add(j, vendName.trim());
                                    }
                                }
                                catch (NumberFormatException e) {
                                    logger.fatal("Unable to parse vendor number.", e);
                                    System.exit(-1);
                                }
                            }
                            break;
                        case 53:
                            if (row.get(6).trim().equalsIgnoreCase("0")) {
                                mapRow.add(j, "");
                            }
                            else {
                                mapRow.add(j, row.get(6).trim() + "#");
                            }
                            break;
                        case 54:
                            if (row.get(7).trim().equalsIgnoreCase("0") || row.get(7).trim().equalsIgnoreCase("-1")) {
                                mapRow.add(j, "");
                            }
                            else {
                                try {
                                    int vendNum = Integer.parseInt(row.get(7).trim());
                                    String vendName = server.getVendorName(vendNum);
                                    if (vendName == null) {
                                        logger.fatal("Vendor name must not be null.", new NullPointerException());
                                        System.exit(-1);
                                    }
                                    else {
                                        mapRow.add(j, vendName.trim());
                                    }
                                }
                                catch (NumberFormatException e) {
                                    logger.fatal("Unable to parse vendor number.", e);
                                    System.exit(-1);
                                }
                            }
                            break;
                        case 59:
                            if (row.get(7).trim().equalsIgnoreCase("0")) {
                                mapRow.add(j, "");
                            }
                            else {
                                mapRow.add(j, row.get(7).trim() + "#");
                            }
                            break;
                        case 60:
                            if (row.get(8).trim().equalsIgnoreCase("0") || row.get(8).trim().equalsIgnoreCase("-1")) {
                                mapRow.add(j, "");
                            }
                            else {
                                try {
                                    int vendNum = Integer.parseInt(row.get(8).trim());
                                    String vendName = server.getVendorName(vendNum);
                                    if (vendName == null) {
                                        logger.fatal("Vendor name must not be null.", new NullPointerException());
                                        System.exit(-1);
                                    }
                                    else {
                                        mapRow.add(j, vendName.trim());
                                    }
                                }
                                catch (NumberFormatException e) {
                                    logger.fatal("Unable to parse vendor number.", e);
                                    System.exit(-1);
                                }
                            }
                            break;
                        case 65:
                            if (row.get(8).trim().equalsIgnoreCase("0")) {
                                mapRow.add(j, "");
                            }
                            else {
                                mapRow.add(j, row.get(8).trim() + "#");
                            }
                            break;
                        case 69:
                            mapRow.add(j, row.get(5).trim() + "#");
                            break;
                        case 77:
                            mapRow.add(j, row.get(9).trim());
                            break;
                        case 78:
                            mapRow.add(j, "lb");
                            break;
                        case 83:
                            mapRow.add(j, row.get(11).trim());
                            break;
                        case 96:
                            mapRow.add(j, row.get(25).trim());
                            break;
                        case 97:
                            mapRow.add(j, row.get(18).trim() + " " + row.get(19).trim());
                            break;
                        case 98:
                            mapRow.add(j, row.get(24).trim());
                            break;
                        case 99:
                            mapRow.add(j, row.get(16).trim() + " " + row.get(17).trim());
                            break;
                        case 100:
                            mapRow.add(j, row.get(23).trim());
                            break;
                        case 101:
                            mapRow.add(j, row.get(21).trim() + " " + row.get(22).trim());
                            break;
                        case 114:
                            mapRow.add(j, "FALSE");
                            break;
                        case 115:
                            mapRow.add(j, row.get(3).trim() + "#");
                            break;
                        case 116:
                            mapRow.add(j, row.get(15).trim() + "#");
                            break;
                        case 117:
                            mapRow.add(j, row.get(14).trim());
                            break;
                        case 118:
                            mapRow.add(j, row.get(20).trim() + "$");
                            break;
                        default:
                            mapRow.add(j, "");
                            break;
                    }
                    progress += progressUpdate;
                    updateProgress(progress, 1.0);
                }
                mapTable.add(mapRow);
                utils.sleep(1);
            }
            mapTable.sort((o1, o2) -> {
                              if (mapTable.indexOf(o1) == 0) {
                                  return -1;
                              }
                              else if (mapTable.indexOf(o2) == 0) {
                                  return 1;
                              }
                              else {
                                  return o1.get(0).compareTo(o2.get(0));
                              }
                          }
            );
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
         * @param data The data to map
         */
        public void setData(List<List<String>> data) {
            this.data = data;
        }
    }
}
