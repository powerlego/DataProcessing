package org.dataprocessing.utils;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Nicholas Curl
 */
public class Reader extends Utils {


    private static final Logger logger = LogManager.getLogger();
    private static final Converters converters = Converters.getInstance();

    public Reader() {

    }

    public List<List<String>> readCSV(File csv) {
        try {
            CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(csv), StandardCharsets.UTF_8));
            return converters.convertTableArrayToTable(reader.readAll());
        } catch (IOException e) {
            logger.fatal("Unable to read CSV.", e);
            System.exit(-1);
            return null;
        } catch (CsvException e) {
            logger.fatal("Invalid validator.", e);
            System.exit(-1);
            return null;
        }
    }

    public List<List<String>> readSheet(File dataFile, int sheetNum) {
        try {
            Workbook wb = WorkbookFactory.create(dataFile);
            Sheet sheet = wb.getSheetAt(sheetNum);
            return readSheet(wb, sheet);
        } catch (IOException e) {
            logger.fatal("Unable to create workbook.", e);
            return new LinkedList<>();
        }
    }

    private List<List<String>> readSheet(Workbook wb, Sheet sheet) {
        FormulaEvaluator fe = wb.getCreationHelper().createFormulaEvaluator();
        DataFormatter formatter = new DataFormatter();
        List<List<String>> sheetConvert = new LinkedList<>();
        {
            int startRow = 0;
            if (sheet.getRow(1) == null) {
                startRow = 2;
            }
            int columnCount = sheet.getRow(0).getLastCellNum();
            for (int r = startRow, rn = sheet.getLastRowNum(); r <= rn; r++) {
                Row row = sheet.getRow(r);
                List<String> rowList = new LinkedList<>();
                if (row == null) {
                    sheetConvert.add(createEmptyRow(columnCount));
                    continue;
                }
                for (int c = 0, cn = row.getLastCellNum(); c < cn; c++) {
                    parseRow(row, fe, formatter, c, rowList);
                }
                sheetConvert.add(rowList);
            }
        }
        try {
            wb.close();
        } catch (IOException e) {
            logger.fatal("Unable to close workbook.", e);
        }
        return sheetConvert;
    }

    public List<List<String>> readSheet(File dataFile, String sheetTitle) {
        try {
            Workbook wb = WorkbookFactory.create(dataFile);
            Sheet sheet = wb.getSheet(sheetTitle);
            return readSheet(wb, sheet);
        } catch (IOException e) {
            logger.fatal("Unable to create workbook.", e);
            return new LinkedList<>();
        }
    }
}
