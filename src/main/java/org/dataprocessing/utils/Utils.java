package org.dataprocessing.utils;

import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Nicholas Curl
 */
public class Utils {


    private static final Logger           logger     = LogManager.getLogger();
    private static final Utils            instance   = new Utils();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private              Window           window;

    public static Utils getInstance() {
        return instance;
    }

    public List<List<String>> convertToTableString(List<List<?>> dataList) {
        List<List<String>> data = new ArrayList<>();
        for (List<?> objects : dataList) {
            List<String> row = new ArrayList<>();
            for (Object object : objects) {
                if (object == null) {
                    row.add("");
                }
                else {
                    row.add(object.toString().trim());
                }
            }
            data.add(row);
        }
        return data;
    }

    public void shutdownExecutor(ExecutorService executorService, Logger logger) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                logger.warn("Termination Timeout");
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Creates an empty table row of specified width
     *
     * @param width The width of the empty row
     *
     * @return The empty row of specified width
     */
    public List<String> createEmptyRow(int width) {
        List<String> row = new ArrayList<>();
        for (int i = 0; i < width; i++) {
            row.add("");
        }
        return row;
    }

    public List<List<String>> createJunkTable(int rows, int cols) {
        List<List<String>> table = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            List<String> row = new ArrayList<>();
            for (int j = 0; j < cols; j++) {
                int cellData = i * j;
                row.add(cellData + "#");
            }
            table.add(row);
        }
        return table;
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

    public Date getDateFormat(String dateString) {
        try {
            return dateFormat.parse(dateString);
        }
        catch (ParseException e) {
            logger.fatal("Unable to parse date.", e);
            System.exit(-1);
            return null;
        }
    }

    public List<List<String>> parallelSortListAscending(List<List<String>> list, int index) {
        List<String[]> listTemp = Converters.convertTableToTableArrayString(list);
        String[][] listArray = listTemp.toArray(new String[listTemp.size()][]);
        Arrays.parallelSort(listArray, Comparator.comparing(o -> o[index]));
        listTemp = new ArrayList<>(Arrays.asList(listArray));
        return Converters.convertTableArrayToTableString(listTemp);
    }

    /*@SafeVarargs
    public final <T> List<List<String>> parallelSortListAscendingExcludeHeader(List<List<?>> list,
                                                                           Comparator<?>... comparators
    ){
        List<Object[]> listTemp = Converters.convertTableToTableArray(list);
        Object[][] listArray = listTemp.toArray(new Object[listTemp.size()][]);
        //Comparator<?> comparatorChain = ComparatorUtils.chainedComparator(comparatorList);
        list.parallelStream().sorted()
        Collections.sort(list,);
        Comparator<Object[]> chainedComparator = ComparatorUtils.chainedComparator(comparatorList);
        Arrays.parallelSort(listArray,1,listArray.length,(Object[] o1, o2) -> {
            int result = o1
            if()
        });
        listTemp = new ArrayList<>(Arrays.asList(listArray));
        return new ArrayList<>();
        //return Converters.convertTableArrayToTableString(listTemp);
    }*/

    public List<List<String>> parallelSortListDescending(List<List<String>> list, int index) {
        List<String[]> listTemp = Converters.convertTableToTableArrayString(list);
        String[][] listArray = listTemp.toArray(new String[listTemp.size()][]);
        Arrays.parallelSort(listArray, Comparator.comparing(o -> o[index], Comparator.reverseOrder()));
        listTemp = new ArrayList<>(Arrays.asList(listArray));
        return Converters.convertTableArrayToTableString(listTemp);
    }

    /*
    public List<List<String>> parallelSortListAscendingExcludeHeader(List<List<String>> list, int sortingIndex) {
        List<String[]> listTemp = Converters.convertTableToTableArray(list);
        String[][] listArray = listTemp.toArray(new String[listTemp.size()][]);
        Comparator<String[]> comparator = Comparator.comparing(strings -> strings[sortingIndex]);
        Arrays.parallelSort(listArray, 1, listArray.length, comparator);
        listTemp = new ArrayList<>(Arrays.asList(listArray));
        return Converters.convertTableArrayToTable(listTemp);
    }*/

    public List<List<String>> parallelSortListDescendingExcludeHeader(List<List<String>> list, int sortingIndex) {
        List<String[]> listTemp = Converters.convertTableToTableArrayString(list);
        String[][] listArray = listTemp.toArray(new String[listTemp.size()][]);
        Arrays.parallelSort(listArray,
                            1,
                            listArray.length,
                            Comparator.comparing(strings -> strings[sortingIndex], Comparator.reverseOrder())
        );
        listTemp = new ArrayList<>(Arrays.asList(listArray));
        return Converters.convertTableArrayToTableString(listTemp);
    }

    public void parseRow(Row row, FormulaEvaluator fe, DataFormatter formatter, int c, List<String> rowList) {
        Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell != null) {
            if (fe != null) {
                cell = fe.evaluateInCell(cell);
            }
            String value = formatter.formatCellValue(cell);
            if (cell.getCellType() == CellType.FORMULA) {
                value = "=" + value;
            }
            rowList.add(value.trim());
        }
        else {
            rowList.add("");
        }
    }

    public double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public float round(float value, int places) {
        return (float) round((double) value, places);
    }

    /**
     * Delays the program
     *
     * @param milliseconds The duration to delay in milliseconds (1 second = 1000 milliseconds)
     */
    public void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public <T extends Comparable<? super T>> Comparator<T[]> sortRow(int index, boolean reverse) {
        if (reverse) {
            return Comparator.comparing(ts -> ts[index], Comparator.reverseOrder());
        }
        else {
            return Comparator.comparing(ts -> ts[index]);
        }
    }
}
