package org.dataprocessing.utils;

import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Nicholas Curl
 */
public class Utils {


    private static final Logger logger = LogManager.getLogger();
    private static final Utils instance = new Utils();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private Window window;

    public static Utils getInstance() {
        return instance;
    }

    /**
     * Creates an empty table row of specified width
     *
     * @param width The width of the empty row
     *
     * @return The empty row of specified width
     */
    public List<String> createEmptyRow(int width) {
        List<String> row = new LinkedList<>();
        for (int i = 0; i < width; i++) {
            row.add("");
        }
        return row;
    }

    public List<List<String>> createJunkTable(int rows, int cols) {
        List<List<String>> table = new LinkedList<>();
        for (int i = 0; i < rows; i++) {
            List<String> row = new LinkedList<>();
            for (int j = 0; j < cols; j++) {
                int cellData = i * j;
                row.add(cellData + "#");
            }
            table.add(row);
        }
        return table;
    }

    public Date getDateFormat(String dateString) {
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            logger.fatal("Unable to parse date.", e);
            System.exit(-1);
            return null;
        }
    }

    public Window getWindow() {
        return window;
    }

    public void setWindow(Window window) {
        this.window = window;
    }

    public boolean isBlankString(String string) {
        return string == null || string.trim().isEmpty();
    }

    public void parseRow(Row row, FormulaEvaluator fe, DataFormatter formatter, int c, List<String> rowList) {
        Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell != null) {
            if (fe != null) cell = fe.evaluateInCell(cell);
            String value = formatter.formatCellValue(cell);
            if (cell.getCellType() == CellType.FORMULA) {
                value = "=" + value;
            }
            rowList.add(value.trim());
        } else {
            rowList.add("");
        }
    }

    public float round(float value, int places) {
        return (float) round((double) value, places);
    }

    public double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Delays the program
     *
     * @param milliseconds The duration to delay in milliseconds (1 second = 1000 milliseconds)
     */
    public void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
