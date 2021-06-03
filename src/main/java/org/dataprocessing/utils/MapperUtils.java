package org.dataprocessing.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Nicholas Curl
 */
public class MapperUtils extends Utils {


    private static final Logger      logger   = LogManager.getLogger(MapperUtils.class);
    private static final MapperUtils instance = new MapperUtils();

    public static MapperUtils getInstance() {
        return instance;
    }

    public List<List<String>> createMapTable(String template) {
        List<List<String>> mapTable = new ArrayList<>();
        List<String> header = getHeader(template);
        mapTable.add(header);
        return mapTable;
    }

    public Map<String, String> getCorrections(String correctionsFile) {
        String line;
        Map<String, String> corrections = new HashMap<>();
        try {
            Path path = Paths.get(correctionsFile);
            Path parentPath = path.getParent();
            try {
                Files.createDirectories(parentPath);
                Files.createFile(path);
            }
            catch (FileAlreadyExistsException ignored) {
            }
            InputStreamReader streamReader = new InputStreamReader(new FileInputStream(path.toFile()));
            BufferedReader fileBuff = new BufferedReader(streamReader);
            while ((line = fileBuff.readLine()) != null) {
                String[] split = line.split(" : ");
                corrections.put(split[0], split[1]);
            }
            fileBuff.close();
        }
        catch (IOException e) {
            logger.fatal("Unable to get corrections.", e);
        }
        return corrections;
    }

    private List<String> getHeaderFromFile(Workbook wb) {
        FormulaEvaluator fe = wb.getCreationHelper().createFormulaEvaluator();
        DataFormatter formatter = new DataFormatter();
        Sheet sheet = wb.getSheetAt(1);
        Row row = sheet.getRow(0);
        List<String> rowList = new ArrayList<>();
        int columnCount = sheet.getRow(0).getLastCellNum();
        if (row == null) {
            return createEmptyRow(columnCount);
        }
        for (int c = 0, cn = row.getLastCellNum(); c < cn; c++) {
            parseRow(row, fe, formatter, c, rowList);
        }
        return rowList;
    }

    /**
     * Get the header from the specified template
     *
     * @param template The template to get the header from
     *
     * @return The header from the template
     */
    public List<String> getHeader(String template) {
        List<String> header;
        try {
            Workbook wb = WorkbookFactory.create(Objects.requireNonNull(getClass().getResourceAsStream(template)));
            header = getHeaderFromFile(wb);
            wb.close();
        }
        catch (IOException exception) {
            logger.fatal("Unable to get header", exception);
            header = new ArrayList<>();
        }
        return header;
    }

    public void writeCorrections(Map<String, String> corrections, String correctionsFile) {
        try {
            PrintWriter writer = new PrintWriter(correctionsFile);
            for (String key : corrections.keySet()) {
                String output = key + " : " + corrections.get(key);
                writer.println(output);
                writer.flush();
            }
            writer.close();
        }
        catch (IOException e) {
            logger.fatal("Unable to write corrections", e);
        }
    }
}
