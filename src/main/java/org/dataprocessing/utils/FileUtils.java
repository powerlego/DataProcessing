package org.dataprocessing.utils;

import com.opencsv.CSVWriter;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTAutoFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Nicholas Curl
 */
public class FileUtils extends Utils {

    private static final Logger    logger   = LogManager.getLogger();
    private static final FileUtils instance = new FileUtils();

    public static FileUtils getInstance() {
        return instance;
    }

    /**
     * Deletes the specified directory
     *
     * @param directoryToBeDeleted The directory to be deleted
     */
    public void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }

    /**
     * Writes the CSV of the data tables
     *
     * @param filename The filename of the CSV
     * @param table    The table to write
     */
    public void writeCSV(Path filename, List<List<String>> table) {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filename.toFile()));
            List<String[]> convertedTable = Converters.convertTableToTableArrayString(table);
            writer.writeAll(convertedTable);
            writer.close();
        }
        catch (IOException e) {
            logger.fatal("Unable to write CSV", e);
            System.exit(-1);
        }
    }

    public void writeXlsx(String filename, List<List<String>> table) {
        writeXlsx(Paths.get(filename).toFile(), table);
    }

    // TODO: 3/16/2021 Convert to Task
    public void writeXlsx(File filename, List<List<String>> table) {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Mapped");
        CreationHelper creationHelper = wb.getCreationHelper();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setFontName("Arial");
        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFont(headerFont);
        Row headerRow = sheet.createRow(0);
        List<String> tableHeaderRow = table.get(0);
        for (int i = 0; i < tableHeaderRow.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(tableHeaderRow.get(i));
            cell.setCellStyle(headerStyle);
        }
        Font dataFont = wb.createFont();
        dataFont.setFontName("Arial");
        dataFont.setFontHeightInPoints((short) 10);
        CellStyle dataCell = wb.createCellStyle();
        dataCell.setFont(dataFont);
        CellStyle dateCell = wb.createCellStyle();
        dateCell.setDataFormat(creationHelper.createDataFormat().getFormat("[$-en-US]mmmm d, yyyy;@"));
        dateCell.setFont(dataFont);
        CellStyle phoneNumCell = wb.createCellStyle();
        phoneNumCell.setFont(dataFont);
        phoneNumCell.setDataFormat(creationHelper.createDataFormat().getFormat("[<=9999999]###-####;(###) ###-####"));
        CellStyle zipCell = wb.createCellStyle();
        zipCell.setFont(dataFont);
        zipCell.setDataFormat(creationHelper.createDataFormat().getFormat("00000"));
        CellStyle zipPlusFourCell = wb.createCellStyle();
        zipPlusFourCell.setFont(dataFont);
        zipPlusFourCell.setDataFormat(creationHelper.createDataFormat().getFormat("00000-0000"));
        CellStyle currencyCell = wb.createCellStyle();
        currencyCell.setFont(dataFont);
        currencyCell.setDataFormat(creationHelper.createDataFormat().getFormat("$#,##0.00"));
        CellStyle weightCell = wb.createCellStyle();
        weightCell.setFont(dataFont);
        weightCell.setDataFormat(creationHelper.createDataFormat().getFormat("0.000"));
        Pattern datePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}");
        Pattern phoneNumPattern = Pattern.compile("^\\d{10}$");
        Pattern zipPattern = Pattern.compile("^\\d{5}$");
        Pattern zipPlusFourPattern = Pattern.compile("^\\d{5}-\\d{4}$");
        Pattern currencyPattern = Pattern.compile("^\\d*\\.\\d{4}$");
        Pattern weightPattern = Pattern.compile("^\\d*\\.\\d{1,3}$");
        for (int i = 1; i < table.size(); i++) {
            Row row = sheet.createRow(i);
            List<String> tableRow = table.get(i);
            for (int j = 0; j < tableRow.size(); j++) {
                Cell cell = row.createCell(j);
                Matcher dateMatcher = datePattern.matcher(tableRow.get(j));
                Matcher phoneNumMatcher = phoneNumPattern.matcher(tableRow.get(j));
                Matcher zipMatcher = zipPattern.matcher(tableRow.get(j));
                Matcher zipPlusFourMatcher = zipPlusFourPattern.matcher(tableRow.get(j));
                Matcher currencyMatcher = currencyPattern.matcher(tableRow.get(j));
                Matcher weightMatcher = weightPattern.matcher(tableRow.get(j));
                if (dateMatcher.find()) {
                    String dateString = dateMatcher.group();
                    cell.setCellValue(getDateFormat(dateString));
                    cell.setCellStyle(dateCell);
                }
                else if (phoneNumMatcher.find()) {
                    String phoneNumString = phoneNumMatcher.group();
                    double phoneNum = 0;
                    try {
                        phoneNum = Double.parseDouble(phoneNumString);
                    }
                    catch (NumberFormatException e) {
                        logger.fatal("Unable to parse phone number.", e);
                        System.exit(-1);
                    }
                    cell.setCellValue(phoneNum);
                    cell.setCellStyle(phoneNumCell);
                }
                else if (zipMatcher.find()) {
                    String zipString = zipMatcher.group();
                    double zip = 0;
                    try {
                        zip = Double.parseDouble(zipString);
                    }
                    catch (NumberFormatException e) {
                        logger.fatal("Unable to parse zip code.", e);
                        System.exit(-1);
                    }
                    cell.setCellValue(zip);
                    cell.setCellStyle(zipCell);
                }
                else if (zipPlusFourMatcher.find()) {
                    String zipPlusFourString = zipPlusFourMatcher.group();
                    zipPlusFourString = zipPlusFourString.replace("-", "");
                    double zipPlusFour = 0;
                    try {
                        zipPlusFour = Double.parseDouble(zipPlusFourString);
                    }
                    catch (NumberFormatException e) {
                        logger.fatal("Unable to parse zip code.", e);
                        System.exit(-1);
                    }
                    cell.setCellValue(zipPlusFour);
                    cell.setCellStyle(zipPlusFourCell);
                }
                else if (currencyMatcher.find()) {
                    String currencyString = currencyMatcher.group();
                    double currency = 0;
                    try {
                        currency = Double.parseDouble(currencyString);
                    }
                    catch (NumberFormatException e) {
                        logger.fatal("Unable to parse currency.", e);
                        System.exit(-1);
                    }
                    cell.setCellValue(currency);
                    cell.setCellStyle(currencyCell);
                }
                else if (weightMatcher.find()) {
                    String weightString = weightMatcher.group();
                    double weight = 0;
                    try {
                        weight = Double.parseDouble(weightString);
                    }
                    catch (NumberFormatException e) {
                        logger.fatal("Unable to parse weight.", e);
                        System.exit(-1);
                    }
                    cell.setCellValue(weight);
                    cell.setCellStyle(weightCell);
                }
                else if (tableRow.get(j).equalsIgnoreCase("FALSE")) {
                    cell.setCellValue(false);
                    cell.setCellStyle(dataCell);
                }
                else if (tableRow.get(j).equalsIgnoreCase("TRUE")) {
                    cell.setCellValue(true);
                    cell.setCellStyle(dataCell);
                }
                else {
                    try {
                        double num = Double.parseDouble(tableRow.get(j).trim());
                        cell.setCellValue(num);
                    }
                    catch (NumberFormatException e) {
                        cell.setCellValue(tableRow.get(j).trim());
                    }
                    cell.setCellStyle(dataCell);
                }
            }
        }
        for (int i = 0; i < tableHeaderRow.size(); i++) {
            sheet.autoSizeColumn(i);
        }
        try {
            FileOutputStream fileOut = new FileOutputStream(filename);
            wb.write(fileOut);
            fileOut.close();
            wb.close();
        }
        catch (IOException e) {
            logger.fatal("Unable to write Excel Workbook", e);
            System.exit(-1);
        }
    }

    public void writeXlsx(Path filename, List<List<String>> table) {
        writeXlsx(filename.toFile(), table);
    }

    public XlsxTask writeXlsxTask(String filename) {
        return writeXlsxTask(Paths.get(filename));
    }

    public XlsxTask writeXlsxTask(Path filename) {
        return writeXlsxTask(filename.toFile());
    }

    public XlsxTask writeXlsxTask(File filename) {
        return new XlsxTask(filename);
    }

    public XlsxTaskMultiSheet writeXlsxTaskMultiSheet(String filename) {
        return writeXlsxTaskMultiSheet(Paths.get(filename));
    }

    public XlsxTaskMultiSheet writeXlsxTaskMultiSheet(Path filename) {
        return writeXlsxTaskMultiSheet(filename.toFile());
    }

    public XlsxTaskMultiSheet writeXlsxTaskMultiSheet(File filename) {
        return new XlsxTaskMultiSheet(filename);
    }

    public static class XlsxTaskMultiSheet extends Task<Void> {

        private static final Logger                          logger = LogManager.getLogger();
        private static final Utils                           utils  = getInstance();
        private final        File                            filename;
        private              Map<String, List<List<String>>> sheets;

        public XlsxTaskMultiSheet(File filename) {
            this.filename = filename;
        }

        @Override
        protected Void call() {
            XSSFWorkbook wb = new XSSFWorkbook();
            if (!isCancelled()) {
                int totalSize = 0;
                int totalHeaderSize = 0;
                for (List<List<String>> value : sheets.values()) {
                    totalSize += (value.size() + 1);
                    totalHeaderSize += (value.get(0).size());
                }
                double progressUpdate = 1.0 / (totalSize * 2) / totalHeaderSize;
                double progress = 0.0;
                updateProgress(progress, 1.0);
                for (String sheetName : sheets.keySet()) {
                    if (isCancelled()) {
                        break;
                    }
                    XSSFSheet sheet = wb.createSheet(sheetName);
                    List<List<String>> table = sheets.get(sheetName);
                    CreationHelper creationHelper = wb.getCreationHelper();
                    Font headerFont = wb.createFont();
                    headerFont.setBold(true);
                    headerFont.setFontHeightInPoints((short) 10);
                    headerFont.setFontName("Arial");
                    CellStyle headerStyle = wb.createCellStyle();
                    headerStyle.setFont(headerFont);
                    List<String> tableHeaderRow = table.get(0);
                    if (!isCancelled()) {
                        XSSFRow headerRow = sheet.createRow(0);
                        for (int i = 0; i < tableHeaderRow.size(); i++) {
                            if (isCancelled()) {
                                break;
                            }
                            XSSFCell cell = headerRow.createCell(i);
                            cell.setCellValue(tableHeaderRow.get(i).trim());
                            cell.setCellStyle(headerStyle);
                            progress += progressUpdate;
                            updateProgress(progress, 1.0);
                        }
                    }
                    if (!isCancelled()) {
                        XSSFFont dataFont = wb.createFont();
                        dataFont.setFontName("Arial");
                        dataFont.setColor(IndexedColors.AUTOMATIC.getIndex());
                        dataFont.setFontHeightInPoints((short) 10);
                        CellStyle numCell = wb.createCellStyle();
                        numCell.setFont(dataFont);
                        numCell.setDataFormat((short) 1);
                        CellStyle dataCell = wb.createCellStyle();
                        dataCell.setFont(dataFont);
                        dataCell.setDataFormat((short) 0);
                        CellStyle textCell = wb.createCellStyle();
                        textCell.setFont(dataFont);
                        textCell.setDataFormat((short) 0x31);
                        CellStyle booleanCell = wb.createCellStyle();
                        booleanCell.setFont(dataFont);
                        booleanCell.setDataFormat((short) 0);
                        CellStyle dateCell = wb.createCellStyle();
                        dateCell.setDataFormat(creationHelper.createDataFormat().getFormat("[$-en-US]mmmm d, yyyy;@"));
                        dateCell.setFont(dataFont);
                        CellStyle phoneNumCell = wb.createCellStyle();
                        phoneNumCell.setFont(dataFont);
                        phoneNumCell.setDataFormat(creationHelper.createDataFormat()
                                                                 .getFormat("[<=9999999]###-####;(###) ###-####"));
                        CellStyle zipCell = wb.createCellStyle();
                        zipCell.setFont(dataFont);
                        zipCell.setDataFormat(creationHelper.createDataFormat().getFormat("00000"));
                        CellStyle zipPlusFourCell = wb.createCellStyle();
                        zipPlusFourCell.setFont(dataFont);
                        zipPlusFourCell.setDataFormat(creationHelper.createDataFormat().getFormat("00000-0000"));
                        CellStyle currencyCell = wb.createCellStyle();
                        currencyCell.setFont(dataFont);
                        currencyCell.setDataFormat((short) 7);
                        CellStyle weightCell = wb.createCellStyle();
                        weightCell.setFont(dataFont);
                        weightCell.setDataFormat(creationHelper.createDataFormat().getFormat("0.000"));
                        CellStyle memoStyle = wb.createCellStyle();
                        memoStyle.setWrapText(true);
                        memoStyle.setFont(dataFont);
                        memoStyle.setDataFormat((short) 0x31);
                        XSSFCellStyle percentStyle = wb.createCellStyle();
                        percentStyle.setFont(dataFont);
                        percentStyle.setDataFormat((short) 0xa);
                        Pattern datePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}");
                        Pattern phoneNumPattern = Pattern.compile("^\\d{10}$");
                        Pattern zipPattern = Pattern.compile("^\\d{5}$");
                        Pattern zipPlusFourPattern = Pattern.compile("^\\d{5}-\\d{4}$");
                        Pattern currencyPattern = Pattern.compile("^\\d*\\.\\d{4}$");
                        Pattern weightPattern = Pattern.compile("^\\d*\\.\\d{1,3}$");
                        Pattern specificTypePattern = Pattern.compile("[@$^%#]");
                        XSSFRow row;
                        XSSFCell cell;
                        int comments = 0;
                        int memo = 0;
                        int i = 1;
                        CellReference tableStart = null;
                        CellReference tableEnd = null;
                        loopBreak:
                        for (List<String> tableRow : table) {
                            if (tableRow.equals(tableHeaderRow)) {
                                continue;
                            }
                            if (isCancelled()) {
                                break;
                            }
                            if (tableRow.isEmpty()) {
                                progress += progressUpdate;
                                updateProgress(progress, 1.0);
                                continue;
                            }
                            row = sheet.createRow(i);
                            /*if (tableRow.get(0).toLowerCase().contains("contractid") || table.get(i-1).get(0).toLowerCase().contains("contractid")) {
                                if ((i - 1) == 0) {
                                    tableStart = new CellReference(0, 0);
                                }
                                else {
                                    tableEnd = new CellReference(row.getRowNum() - 1, tableHeaderRow.size() - 1);
                                    if (tableStart == null) {
                                        tableStart = new CellReference(0, 0);
                                    }
                                    AreaReference reference = new AreaReference(tableStart,
                                                                                tableEnd,
                                                                                SpreadsheetVersion.EXCEL2007
                                    );
                                    XSSFTable xssfTable = sheet.createTable(reference);
                                    xssfTable.getCTTable().addNewTableStyleInfo();
                                    XSSFTableStyleInfo style = (XSSFTableStyleInfo) xssfTable.getStyle();
                                    style.setName("TableStyleMedium16");
                                    style.setShowColumnStripes(false);
                                    style.setShowRowStripes(true);
                                    style.setFirstColumn(false);
                                    style.setLastColumn(false);
                                    CTAutoFilter autoFilter = xssfTable.getCTTable().addNewAutoFilter();
                                    autoFilter.setRef(reference.formatAsString());
                                    tableStart = new CellReference(row.getRowNum(),0);
                                }
                            }
                            if(i+1>=table.size() && table.get(0).get(0).toLowerCase().contains("contractid")){
                                tableEnd = new CellReference(row.getRowNum(), tableHeaderRow.size());
                                if (tableStart == null) {
                                    tableStart = new CellReference(0, 0);
                                }
                                AreaReference reference = new AreaReference(tableStart,
                                                                            tableEnd,
                                                                            SpreadsheetVersion.EXCEL2007
                                );
                                XSSFTable xssfTable = sheet.createTable(reference);
                                xssfTable.getCTTable().addNewTableStyleInfo();
                                XSSFTableStyleInfo style = (XSSFTableStyleInfo) xssfTable.getStyle();
                                style.setName("TableStyleMedium16");
                                style.setShowColumnStripes(false);
                                style.setShowRowStripes(true);
                                style.setFirstColumn(false);
                                style.setLastColumn(false);
                                CTAutoFilter autoFilter = xssfTable.getCTTable().addNewAutoFilter();
                                autoFilter.setRef(reference.formatAsString());
                            }*/
                            for (int j = 0; j < tableRow.size(); j++) {
                                if (table.get(0).get(j).toLowerCase().contains("memo")) {
                                    memo = j;
                                }
                                if (table.get(0).get(j).toLowerCase().contains("comments")) {
                                    comments = j;
                                }
                                if (isCancelled()) {
                                    break loopBreak;
                                }
                                cell = row.createCell(j);
                                Matcher dateMatcher = datePattern.matcher(tableRow.get(j));
                                Matcher phoneNumMatcher = phoneNumPattern.matcher(tableRow.get(j));
                                Matcher zipMatcher = zipPattern.matcher(tableRow.get(j));
                                Matcher zipPlusFourMatcher = zipPlusFourPattern.matcher(tableRow.get(j));
                                Matcher currencyMatcher = currencyPattern.matcher(tableRow.get(j));
                                Matcher weightMatcher = weightPattern.matcher(tableRow.get(j));
                                boolean specificType = specificTypePattern.matcher(tableRow.get(j)).find();
                                if (dateMatcher.find() && !specificType) {
                                    String dateString = dateMatcher.group();
                                    cell.setCellValue(utils.getDateFormat(dateString));
                                    cell.setCellStyle(dateCell);
                                }
                                else if (phoneNumMatcher.find() && !specificType) {
                                    String phoneNumString = phoneNumMatcher.group();
                                    double phoneNum = 0;
                                    try {
                                        phoneNum = Double.parseDouble(phoneNumString);
                                    }
                                    catch (NumberFormatException e) {
                                        logger.fatal("Unable to parse phone number.", e);
                                        System.exit(-1);
                                    }
                                    cell.setCellValue(phoneNum);
                                    cell.setCellStyle(phoneNumCell);
                                }
                                else if (zipMatcher.find() && !specificType) {
                                    String zipString = zipMatcher.group();
                                    double zip = 0;
                                    try {
                                        zip = Double.parseDouble(zipString);
                                    }
                                    catch (NumberFormatException e) {
                                        logger.fatal("Unable to parse zip code.", e);
                                        System.exit(-1);
                                    }
                                    cell.setCellValue(zip);
                                    cell.setCellStyle(zipCell);
                                }
                                else if (zipPlusFourMatcher.find() && !specificType) {
                                    String zipPlusFourString = zipPlusFourMatcher.group();
                                    zipPlusFourString = zipPlusFourString.replace("-", "");
                                    double zipPlusFour = 0;
                                    try {
                                        zipPlusFour = Double.parseDouble(zipPlusFourString);
                                    }
                                    catch (NumberFormatException e) {
                                        logger.fatal("Unable to parse zip code.", e);
                                        System.exit(-1);
                                    }
                                    cell.setCellValue(zipPlusFour);
                                    cell.setCellStyle(zipPlusFourCell);
                                }
                                else if (currencyMatcher.find() && !specificType) {
                                    String currencyString = currencyMatcher.group();
                                    double currency = 0;
                                    try {
                                        currency = Double.parseDouble(currencyString);
                                    }
                                    catch (NumberFormatException e) {
                                        logger.fatal("Unable to parse currency.", e);
                                        System.exit(-1);
                                    }
                                    cell.setCellValue(currency);
                                    cell.setCellStyle(currencyCell);
                                }
                                else if (weightMatcher.find() && !specificType) {
                                    String weightString = weightMatcher.group();
                                    double weight = 0;
                                    try {
                                        weight = Double.parseDouble(weightString);
                                    }
                                    catch (NumberFormatException e) {
                                        logger.fatal("Unable to parse weight.", e);
                                        System.exit(-1);
                                    }
                                    cell.setCellValue(weight);
                                    cell.setCellStyle(weightCell);
                                }
                                else if (tableRow.get(j).equalsIgnoreCase("FALSE")) {
                                    cell.setCellValue(false);
                                    cell.setCellStyle(booleanCell);
                                }
                                else if (tableRow.get(j).equalsIgnoreCase("TRUE")) {
                                    cell.setCellValue(true);
                                    cell.setCellStyle(booleanCell);
                                }
                                else {
                                    String entry = tableRow.get(j).trim();
                                    if (entry.endsWith("#")) {
                                        entry = entry.substring(0, entry.length() - 1);
                                        try {
                                            double transID = Double.parseDouble(entry);
                                            cell.setCellValue(transID);
                                            cell.setCellStyle(numCell);
                                        }
                                        catch (NumberFormatException e) {
                                            logger.warn("Unable to parse number.", e);
                                        }
                                    }
                                    else if (entry.endsWith("@")) {
                                        entry = entry.substring(0, entry.length() - 1);
                                        cell.setCellValue(entry);
                                        cell.setCellStyle(textCell);
                                    }
                                    else if (entry.endsWith("$")) {
                                        entry = entry.substring(0, entry.length() - 1);
                                        double currency = 0;
                                        try {
                                            currency = Double.parseDouble(entry);
                                        }
                                        catch (NumberFormatException e) {
                                            logger.fatal("Unable to parse currency.", e);
                                            System.exit(-1);
                                        }
                                        cell.setCellValue(currency);
                                        cell.setCellStyle(currencyCell);
                                    }
                                    else if (entry.endsWith("^")) {
                                        entry = entry.substring(0, entry.length() - 1);
                                        if (entry.endsWith("\n.")) {
                                            entry = entry.substring(0, entry.length() - 2);
                                        }
                                        entry = entry.trim();
                                        cell.setCellValue(entry);
                                        cell.setCellStyle(memoStyle);

                                    }
                                    else if (entry.endsWith("%")) {
                                        entry = entry.substring(0, entry.length() - 1);
                                        double percentage = 0;
                                        try {
                                            percentage = Double.parseDouble(entry);
                                        }
                                        catch (NumberFormatException e) {
                                            logger.fatal("Unable to parse percentage.", e);
                                            System.exit(-1);
                                        }
                                        cell.setCellValue(percentage);
                                        cell.setCellStyle(percentStyle);
                                    }
                                    else {
                                        if (utils.isBlankString(entry)) {
                                            cell.setCellType(CellType.BLANK);
                                        }
                                        else {
                                            cell.setCellValue(entry);
                                        }
                                        cell.setCellStyle(dataCell);
                                    }
                                }
                                progress += progressUpdate;
                                updateProgress(progress, 1.0);
                            }
                            i++;
                        }
                        //if (!table.get(0).get(0).toLowerCase().contains("contractid")) {
                        AreaReference reference = new AreaReference(new CellReference(0, 0),
                                                                    new CellReference(table.size() - 1,
                                                                                      tableHeaderRow.size() - 1
                                                                    ),
                                                                    SpreadsheetVersion.EXCEL2007
                        );
                        XSSFTable xssfTable = sheet.createTable(reference);
                        xssfTable.getCTTable().addNewTableStyleInfo();
                        XSSFTableStyleInfo style = (XSSFTableStyleInfo) xssfTable.getStyle();
                        style.setName("TableStyleMedium16");
                        style.setShowColumnStripes(false);
                        style.setShowRowStripes(true);
                        style.setFirstColumn(false);
                        style.setLastColumn(false);
                        CTAutoFilter autoFilter = xssfTable.getCTTable().addNewAutoFilter();
                        autoFilter.setRef(reference.formatAsString());
                        //}
                        for (int j = 0; j < tableHeaderRow.size(); j++) {
                            if (isCancelled()) {
                                break;
                            }
                            if (comments != 0 && comments == j) {
                                sheet.setColumnWidth(comments, 25600);
                                comments = 0;
                                continue;
                            }
                            if (memo != 0 && memo == j) {
                                sheet.setColumnWidth(memo, 25600);
                                memo = 0;
                                continue;
                            }
                            sheet.autoSizeColumn(j);
                            progress += progressUpdate;
                            updateProgress(progress, 1.0);
                        }

                    }
                }
                if (!isCancelled()) {
                    try {
                        FileOutputStream fileOut = new FileOutputStream(filename);
                        wb.write(fileOut);
                        fileOut.close();
                        wb.close();
                        updateProgress(1.0, 1.0);
                        return null;
                    }
                    catch (IOException e) {
                        logger.fatal("Unable to write Excel Workbook", e);
                        System.exit(-1);
                    }
                }
                else {
                    try {
                        wb.close();
                        return null;
                    }
                    catch (IOException e) {
                        logger.fatal("Unable to close workbook.", e);
                        System.exit(-1);
                    }
                }
            }
            else {
                try {
                    wb.close();
                    return null;
                }
                catch (IOException e) {
                    logger.fatal("Unable to close workbook.", e);
                    System.exit(-1);
                }

            }
            return null;
        }

        /**
         * Logs the exception when the task transitions to the failure state
         */
        @Override
        protected void failed() {
            logger.fatal("Write Task failed", getException());
            System.exit(-1);
        }

        public void setSheets(Map<String, List<List<String>>> sheets) {
            this.sheets = sheets;
        }
    }

    public static class XlsxTask extends Task<Void> {

        private static final Logger             logger = LogManager.getLogger();
        private static final Utils              utils  = getInstance();
        private final        File               filename;
        private              List<List<String>> table;

        public XlsxTask(File filename) {
            this.filename = filename;
        }

        @Override
        protected Void call() {
            XSSFWorkbook wb = new XSSFWorkbook();
            if (!isCancelled()) {
                XSSFSheet sheet = wb.createSheet("Mapped");
                CreationHelper creationHelper = wb.getCreationHelper();
                Font headerFont = wb.createFont();
                headerFont.setBold(true);
                headerFont.setFontHeightInPoints((short) 10);
                headerFont.setFontName("Arial");
                CellStyle headerStyle = wb.createCellStyle();
                headerStyle.setFont(headerFont);
                List<String> tableHeaderRow = table.get(0);
                double progressUpdate = 1.0 / ((table.size() + 1) * 2) / tableHeaderRow.size();
                double progress = 0.0;
                updateProgress(progress, 1.0);
                if (!isCancelled()) {
                    XSSFRow headerRow = sheet.createRow(0);
                    for (int i = 0; i < tableHeaderRow.size(); i++) {
                        if (isCancelled()) {
                            break;
                        }
                        XSSFCell cell = headerRow.createCell(i);
                        cell.setCellValue(tableHeaderRow.get(i).trim());
                        cell.setCellStyle(headerStyle);
                        progress += progressUpdate;
                        updateProgress(progress, 1.0);
                    }
                }
                if (!isCancelled()) {
                    XSSFFont dataFont = wb.createFont();
                    dataFont.setFontName("Arial");
                    dataFont.setColor(IndexedColors.AUTOMATIC.getIndex());
                    dataFont.setFontHeightInPoints((short) 10);
                    CellStyle numCell = wb.createCellStyle();
                    numCell.setFont(dataFont);
                    numCell.setDataFormat((short) 1);
                    CellStyle dataCell = wb.createCellStyle();
                    dataCell.setFont(dataFont);
                    dataCell.setDataFormat((short) 0);
                    CellStyle textCell = wb.createCellStyle();
                    textCell.setFont(dataFont);
                    textCell.setDataFormat((short) 0x31);
                    CellStyle booleanCell = wb.createCellStyle();
                    booleanCell.setFont(dataFont);
                    booleanCell.setDataFormat((short) 0);
                    CellStyle dateCell = wb.createCellStyle();
                    dateCell.setDataFormat(creationHelper.createDataFormat().getFormat("[$-en-US]mmmm d, yyyy;@"));
                    dateCell.setFont(dataFont);
                    CellStyle phoneNumCell = wb.createCellStyle();
                    phoneNumCell.setFont(dataFont);
                    phoneNumCell.setDataFormat(creationHelper.createDataFormat()
                                                             .getFormat("[<=9999999]###-####;(###) ###-####"));
                    CellStyle zipCell = wb.createCellStyle();
                    zipCell.setFont(dataFont);
                    zipCell.setDataFormat(creationHelper.createDataFormat().getFormat("00000"));
                    CellStyle zipPlusFourCell = wb.createCellStyle();
                    zipPlusFourCell.setFont(dataFont);
                    zipPlusFourCell.setDataFormat(creationHelper.createDataFormat().getFormat("00000-0000"));
                    CellStyle currencyCell = wb.createCellStyle();
                    currencyCell.setFont(dataFont);
                    currencyCell.setDataFormat((short) 7);
                    CellStyle weightCell = wb.createCellStyle();
                    weightCell.setFont(dataFont);
                    weightCell.setDataFormat(creationHelper.createDataFormat().getFormat("0.000"));
                    CellStyle memoStyle = wb.createCellStyle();
                    memoStyle.setWrapText(true);
                    memoStyle.setFont(dataFont);
                    memoStyle.setDataFormat((short) 0x31);
                    XSSFCellStyle percentStyle = wb.createCellStyle();
                    percentStyle.setFont(dataFont);
                    percentStyle.setDataFormat((short) 0xa);
                    Pattern datePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}");
                    Pattern phoneNumPattern = Pattern.compile("^\\d{10}$");
                    Pattern zipPattern = Pattern.compile("^\\d{5}$");
                    Pattern zipPlusFourPattern = Pattern.compile("^\\d{5}-\\d{4}$");
                    Pattern currencyPattern = Pattern.compile("^\\d*\\.\\d{4}$");
                    Pattern weightPattern = Pattern.compile("^\\d*\\.\\d{1,3}$");
                    Pattern specificTypePattern = Pattern.compile("[@$^%#]");
                    XSSFRow row;
                    XSSFCell cell;
                    int comments = 0;
                    int memo = 0;
                    int i = 1;
                    loopBreak:
                    for (List<String> tableRow : table) {
                        if (tableRow.equals(tableHeaderRow)) {
                            continue;
                        }
                        if (isCancelled()) {
                            break;
                        }
                        if (tableRow.isEmpty()) {
                            progress += progressUpdate;
                            updateProgress(progress, 1.0);
                            continue;
                        }
                        row = sheet.createRow(i);
                        for (int j = 0; j < tableRow.size(); j++) {
                            if (table.get(0).get(j).toLowerCase().contains("memo")) {
                                memo = j;
                            }
                            if (table.get(0).get(j).toLowerCase().contains("comments")) {
                                comments = j;
                            }
                            if (isCancelled()) {
                                break loopBreak;
                            }
                            cell = row.createCell(j);
                            Matcher dateMatcher = datePattern.matcher(tableRow.get(j));
                            Matcher phoneNumMatcher = phoneNumPattern.matcher(tableRow.get(j));
                            Matcher zipMatcher = zipPattern.matcher(tableRow.get(j));
                            Matcher zipPlusFourMatcher = zipPlusFourPattern.matcher(tableRow.get(j));
                            Matcher currencyMatcher = currencyPattern.matcher(tableRow.get(j));
                            Matcher weightMatcher = weightPattern.matcher(tableRow.get(j));
                            boolean specificType = specificTypePattern.matcher(tableRow.get(j)).find();
                            if (dateMatcher.find() && !specificType) {
                                String dateString = dateMatcher.group();
                                cell.setCellValue(utils.getDateFormat(dateString));
                                cell.setCellStyle(dateCell);
                            }
                            else if (phoneNumMatcher.find() && !specificType) {
                                String phoneNumString = phoneNumMatcher.group();
                                double phoneNum = 0;
                                try {
                                    phoneNum = Double.parseDouble(phoneNumString);
                                }
                                catch (NumberFormatException e) {
                                    logger.fatal("Unable to parse phone number.", e);
                                    System.exit(-1);
                                }
                                cell.setCellValue(phoneNum);
                                cell.setCellStyle(phoneNumCell);
                            }
                            else if (zipMatcher.find() && !specificType) {
                                String zipString = zipMatcher.group();
                                double zip = 0;
                                try {
                                    zip = Double.parseDouble(zipString);
                                }
                                catch (NumberFormatException e) {
                                    logger.fatal("Unable to parse zip code.", e);
                                    System.exit(-1);
                                }
                                cell.setCellValue(zip);
                                cell.setCellStyle(zipCell);
                            }
                            else if (zipPlusFourMatcher.find() && !specificType) {
                                String zipPlusFourString = zipPlusFourMatcher.group();
                                zipPlusFourString = zipPlusFourString.replace("-", "");
                                double zipPlusFour = 0;
                                try {
                                    zipPlusFour = Double.parseDouble(zipPlusFourString);
                                }
                                catch (NumberFormatException e) {
                                    logger.fatal("Unable to parse zip code.", e);
                                    System.exit(-1);
                                }
                                cell.setCellValue(zipPlusFour);
                                cell.setCellStyle(zipPlusFourCell);
                            }
                            else if (currencyMatcher.find() && !specificType) {
                                String currencyString = currencyMatcher.group();
                                double currency = 0;
                                try {
                                    currency = Double.parseDouble(currencyString);
                                }
                                catch (NumberFormatException e) {
                                    logger.fatal("Unable to parse currency.", e);
                                    System.exit(-1);
                                }
                                cell.setCellValue(currency);
                                cell.setCellStyle(currencyCell);
                            }
                            else if (weightMatcher.find() && !specificType) {
                                String weightString = weightMatcher.group();
                                double weight = 0;
                                try {
                                    weight = Double.parseDouble(weightString);
                                }
                                catch (NumberFormatException e) {
                                    logger.fatal("Unable to parse weight.", e);
                                    System.exit(-1);
                                }
                                cell.setCellValue(weight);
                                cell.setCellStyle(weightCell);
                            }
                            else if (tableRow.get(j).equalsIgnoreCase("FALSE")) {
                                cell.setCellValue(false);
                                cell.setCellStyle(booleanCell);
                            }
                            else if (tableRow.get(j).equalsIgnoreCase("TRUE")) {
                                cell.setCellValue(true);
                                cell.setCellStyle(booleanCell);
                            }
                            else {
                                String entry = tableRow.get(j).trim();
                                if (entry.endsWith("#")) {
                                    entry = entry.substring(0, entry.length() - 1);
                                    try {
                                        double transID = Double.parseDouble(entry);
                                        cell.setCellValue(transID);
                                        cell.setCellStyle(numCell);
                                    }
                                    catch (NumberFormatException e) {
                                        logger.warn("Unable to parse number.", e);
                                    }
                                }
                                else if (entry.endsWith("@")) {
                                    entry = entry.substring(0, entry.length() - 1);
                                    cell.setCellValue(entry);
                                    cell.setCellStyle(textCell);
                                }
                                else if (entry.endsWith("$")) {
                                    entry = entry.substring(0, entry.length() - 1);
                                    double currency = 0;
                                    try {
                                        currency = Double.parseDouble(entry);
                                    }
                                    catch (NumberFormatException e) {
                                        logger.fatal("Unable to parse currency.", e);
                                        System.exit(-1);
                                    }
                                    cell.setCellValue(currency);
                                    cell.setCellStyle(currencyCell);
                                }
                                else if (entry.endsWith("^")) {
                                    entry = entry.substring(0, entry.length() - 1);
                                    if (entry.endsWith("\n.")) {
                                        entry = entry.substring(0, entry.length() - 2);
                                    }
                                    entry = entry.trim();
                                    cell.setCellValue(entry);
                                    cell.setCellStyle(memoStyle);

                                }
                                else if (entry.endsWith("%")) {
                                    entry = entry.substring(0, entry.length() - 1);
                                    double percentage = 0;
                                    try {
                                        percentage = Double.parseDouble(entry);
                                    }
                                    catch (NumberFormatException e) {
                                        logger.fatal("Unable to parse percentage.", e);
                                        System.exit(-1);
                                    }
                                    cell.setCellValue(percentage);
                                    cell.setCellStyle(percentStyle);
                                }
                                else {
                                    if (utils.isBlankString(entry)) {
                                        cell.setCellType(CellType.BLANK);
                                    }
                                    else {
                                        cell.setCellValue(entry);
                                    }
                                    cell.setCellStyle(dataCell);
                                }
                            }
                            progress += progressUpdate;
                            updateProgress(progress, 1.0);
                        }
                        i++;
                    }
                    AreaReference reference = new AreaReference(new CellReference(0, 0),
                                                                new CellReference(table.size() - 1,
                                                                                  tableHeaderRow.size() - 1
                                                                ),
                                                                SpreadsheetVersion.EXCEL2007
                    );
                    XSSFTable xssfTable = sheet.createTable(reference);
                    xssfTable.getCTTable().addNewTableStyleInfo();
                    XSSFTableStyleInfo style = (XSSFTableStyleInfo) xssfTable.getStyle();
                    style.setName("TableStyleMedium16");
                    style.setShowColumnStripes(false);
                    style.setShowRowStripes(true);
                    style.setFirstColumn(false);
                    style.setLastColumn(false);
                    CTAutoFilter autoFilter = xssfTable.getCTTable().addNewAutoFilter();
                    autoFilter.setRef(reference.formatAsString());
                    for (int j = 0; j < tableHeaderRow.size(); j++) {
                        if (isCancelled()) {
                            break;
                        }
                        if (comments != 0 && comments == j) {
                            sheet.setColumnWidth(comments, 25600);
                            comments = 0;
                            continue;
                        }
                        if (memo != 0 && memo == j) {
                            sheet.setColumnWidth(memo, 25600);
                            memo = 0;
                            continue;
                        }
                        sheet.autoSizeColumn(j);
                        progress += progressUpdate;
                        updateProgress(progress, 1.0);
                    }

                }
                if (!isCancelled()) {
                    try {
                        FileOutputStream fileOut = new FileOutputStream(filename);
                        wb.write(fileOut);
                        fileOut.close();
                        wb.close();
                        updateProgress(1.0, 1.0);
                        return null;
                    }
                    catch (IOException e) {
                        logger.fatal("Unable to write Excel Workbook", e);
                        System.exit(-1);
                    }
                }
                else {
                    try {
                        wb.close();
                        return null;
                    }
                    catch (IOException e) {
                        logger.fatal("Unable to close workbook.", e);
                        System.exit(-1);
                    }
                }
            }
            else {
                try {
                    wb.close();
                    return null;
                }
                catch (IOException e) {
                    logger.fatal("Unable to close workbook.", e);
                    System.exit(-1);
                }

            }
            return null;
        }

        /**
         * Logs the exception when the task transitions to the failure state
         */
        @Override
        protected void failed() {
            logger.fatal("Write Task failed", getException());
            System.exit(-1);
        }

        public void setTable(List<List<String>> table) {
            this.table = table;
        }
    }
}