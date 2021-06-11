package org.dataprocessing.backend.objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nicholas Curl
 */
public class SalesOrder {

    /**
     * The instance of the logger
     */
    private static final Logger             logger = LogManager.getLogger(SalesOrder.class);
    private final        String             salesOrderNum;
    private              List<List<String>> itemLines;
    private              List<String>       mainStructure;

    public SalesOrder(String salesOrderNum) {
        this.itemLines = new ArrayList<>();
        this.salesOrderNum = salesOrderNum;
        this.mainStructure = new ArrayList<>();

    }

    public synchronized void addItemLine(List<String> itemLine) {
        this.itemLines.add(itemLine);
    }

    public synchronized List<List<String>> getItemLines() {
        return itemLines;
    }

    public void setItemLines(List<List<String>> itemLines) {
        this.itemLines = itemLines;
    }

    public synchronized List<String> getMainStructure() {
        return mainStructure;
    }

    public void setMainStructure(List<String> mainStructure) {
        this.mainStructure = mainStructure;
    }

    public synchronized String getSalesOrderNum() {
        return salesOrderNum;
    }

    @Override
    public String toString() {
        String salesOrderSting = "SalesOrder{" +
                                 "salesOrderNum=" + salesOrderNum;
        try {
            salesOrderSting += ", mainStructure=" + mainStructure.get(5) +
                               ", series=" + mainStructure.get(37) +
                               '}';
        }
        catch (IndexOutOfBoundsException e) {
            salesOrderSting += ", mainStructure=Unknown" + ", series=Unknown" + '}';
        }
        return salesOrderSting;
    }
}
