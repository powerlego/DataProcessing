package org.dataprocessing.backend.tasks;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.concurrent.Task;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;
import org.dataprocessing.backend.tasks.KitMapper.KitMapping;
import org.dataprocessing.utils.FileUtils;
import org.dataprocessing.utils.Utils;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTAutoFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Nicholas Curl
 */
public class KitWriter {

    /**
     * The instance of the logger
     */
    private static final Logger        logger    = LogManager.getLogger(KitWriter.class);
    /**
     * The instance of the FileUtils class
     */
    private static final FileUtils     fileUtils = FileUtils.getInstance();
    private static final Utils         utils     = Utils.getInstance();
    private final        DoubleBinding totalProgress;
    private final        KitMapper     kitMapper;
    private final        KitMapping    kitMapping;
    private final        KitBreakouts  kitBreakouts;
    private final        KitXlsxTask   kitXlsxTask;
    /**
     * The list of sub-tasks
     */
    private final        List<Task<?>> tasks;

    public KitWriter() {
        this.kitMapper = new KitMapper();
        this.kitMapping = (KitMapping) kitMapper.getTasks().get(1);
        this.kitBreakouts = new KitBreakouts();
        this.kitXlsxTask = new KitXlsxTask(Paths.get("./mapped data/Subassembly Breakout.xlsx").toFile());
        this.tasks = new ArrayList<>();
        this.tasks.addAll(kitMapper.getTasks());
        this.tasks.add(kitBreakouts);
        this.tasks.add(kitXlsxTask);
        totalProgress = Bindings.createDoubleBinding(() -> (
                                                                   Math.max(0, kitMapper.getTotalProgress()) +
                                                                   Math.max(0, kitBreakouts.getProgress()) +
                                                                   Math.max(0, kitXlsxTask.getProgress())
                                                           ) / 3,
                                                     kitMapper.totalProgressProperty(),
                                                     kitBreakouts.progressProperty(),
                                                     kitXlsxTask.progressProperty()
        );
    }

    public List<Task<?>> getTasks() {
        return tasks;
    }

    public double getTotalProgress() {
        return totalProgress.get();
    }

    public void map(ExecutorService executorService) {
        kitMapping.setOnSucceeded(event -> {
            kitBreakouts.setSubassemblies(kitMapping.getValue());
            executorService.submit(kitBreakouts);
        });
        kitBreakouts.setOnSucceeded(event -> {
            kitXlsxTask.setKits(kitBreakouts.getValue());
            executorService.submit(kitXlsxTask);
        });
        kitMapper.map(executorService);
    }

    public DoubleBinding totalProgressProperty() {
        return totalProgress;
    }

    private static class KitXlsxTask extends Task<Void> {

        private final File                            filename;
        private       MultiValuedMap<String, List<?>> kits;


        private KitXlsxTask(File filename) {
            this.filename = filename;
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
        protected Void call() throws Exception {
            XSSFWorkbook wb = new XSSFWorkbook();
            double progress = 0.0;
            updateProgress(progress, 1.0);
            double progressUpdate = 1.0 / (2 * kits.keySet().size());
            int columnTotal = 6;
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 10);
            headerFont.setFontName("Arial");
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);
            XSSFSheet sheet = wb.createSheet("Kit Breakout");
            XSSFRow headerRow = sheet.createRow(0);
            XSSFCell column = headerRow.createCell(0);
            column.setCellValue("mainAssemblyKey");
            column.setCellStyle(headerStyle);
            column = headerRow.createCell(1);
            column.setCellValue("parentKey");
            column.setCellStyle(headerStyle);
            column = headerRow.createCell(2);
            column.setCellValue("itemKey");
            column.setCellStyle(headerStyle);
            column = headerRow.createCell(3);
            column.setCellValue("itemName");
            column.setCellStyle(headerStyle);
            column = headerRow.createCell(4);
            column.setCellValue("itemQty");
            column.setCellStyle(headerStyle);
            column = headerRow.createCell(5);
            column.setCellValue("subassemblyTotal");
            column.setCellStyle(headerStyle);
            XSSFFont dataFont = wb.createFont();
            dataFont.setFontName("Arial");
            dataFont.setColor(IndexedColors.AUTOMATIC.getIndex());
            dataFont.setFontHeightInPoints((short) 10);
            CellStyle dataCell = wb.createCellStyle();
            dataCell.setFont(dataFont);
            dataCell.setDataFormat((short) 0);
            int i = 1;
            for (String s : kits.keySet()) {
                int startRow = i + 1;
                List<List<?>> breakouts = new ArrayList<>(kits.get(s));
                int endRow = i + breakouts.size();
                for (List<?> breakout : breakouts) {
                    XSSFRow row = sheet.createRow(i);
                    for (int j = 0; j < columnTotal; j++) {
                        Object value = breakout.get(j);
                        XSSFCell cell = row.createCell(j);
                        if (value instanceof String) {
                            String string = (String) value;
                            cell.setCellValue(string);
                        }
                        else if (value instanceof Boolean) {
                            boolean aBoolean = (Boolean) value;
                            cell.setCellValue(aBoolean);
                        }
                        else if (value instanceof Date) {
                            Date date = (Date) value;
                            cell.setCellValue(date);
                        }
                        else if (value instanceof Double) {
                            double aDouble = (Double) value;
                            cell.setCellValue(aDouble);
                        }
                        cell.setCellStyle(dataCell);
                    }
                    i++;
                }
                sheet.groupRow(startRow, endRow);
                progress += progressUpdate;
                updateProgress(progress, 1.0);
                utils.sleep(1);
            }
            AreaReference reference = new AreaReference(new CellReference(0, 0),
                                                        new CellReference(sheet.getLastRowNum(),
                                                                          columnTotal - 1
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
            for (int j = 0; j < columnTotal; j++) {
                sheet.autoSizeColumn(j);
            }
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(filename);
                wb.write(fileOutputStream);
                wb.close();
                fileOutputStream.close();
                updateProgress(1.0, 1.0);
            }
            catch (IOException e) {
                logger.fatal("Unable to write xlsx workbook", e);
                System.exit(1);
            }
            return null;
        }

        @Override
        protected void failed() {
            logger.fatal("Failed to write kits to xlsx", getException());
            System.exit(1);
            super.failed();
        }

        private void setKits(MultiValuedMap<String, List<?>> kits) {
            this.kits = kits;
        }
    }
}