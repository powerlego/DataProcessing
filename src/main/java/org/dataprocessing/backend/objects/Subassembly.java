package org.dataprocessing.backend.objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Nicholas Curl
 */
public class Subassembly {

    /**
     * The instance of the logger
     */
    private static final Logger             logger = LogManager.getLogger(Subassembly.class);
    private final        String             assemblyKey;
    private final        String             name;
    private              List<AssemblyItem> assemblyItems;


    public Subassembly(String assemblyKey, String name, AssemblyItem... items) {
        this.assemblyKey = assemblyKey;
        this.name = name;
        this.assemblyItems = new ArrayList<>();
        assemblyItems.addAll(Arrays.asList(items));
    }

    public Subassembly(String assemblyKey, String name, List<AssemblyItem> items) {
        this.assemblyKey = assemblyKey;
        this.name = name;
        this.assemblyItems = items;
    }

    public void addItem(AssemblyItem item) {
        this.assemblyItems.add(item);
    }

    public String getName() {
        return name;
    }

    public boolean hasItem(String itemKey) {
        boolean containsItem = false;
        for (AssemblyItem assemblyItem : assemblyItems) {
            if (assemblyItem.itemKey.equalsIgnoreCase(itemKey)) {
                containsItem = true;
                break;
            }
        }
        return containsItem;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAssemblyKey(), getAssemblyItems());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Subassembly that = (Subassembly) o;
        return getAssemblyKey().equals(that.getAssemblyKey()) && getAssemblyItems().equals(that.getAssemblyItems());
    }

    public String getAssemblyKey() {
        return assemblyKey;
    }

    public List<AssemblyItem> getAssemblyItems() {
        return assemblyItems;
    }

    @Override
    public String toString() {
        return "Subassembly{" +
               "assemblyKey='" + assemblyKey + '\'' +
               ", name='" + name + '\'' +
               ", assemblyItems=" + assemblyItems +
               '}';
    }

    public void sortAssemblyItems() {
        assemblyItems = assemblyItems.parallelStream()
                                     .sorted((o1, o2) -> Boolean.compare(o1.isSubassembly(), o2.isSubassembly()))
                                     .collect(Collectors.toList());
    }

    public static class AssemblyItem {

        private final String  itemKey;
        private final String  name;
        private final int     qty;
        private final boolean subassembly;


        public AssemblyItem(String itemKey, int qty, String name, boolean subassembly) {
            this.itemKey = itemKey;
            this.qty = qty;
            this.name = name;
            this.subassembly = subassembly;
        }

        public String getItemKey() {
            return itemKey;
        }

        public String getName() {
            return name;
        }

        public int getQty() {
            return qty;
        }

        public boolean isSubassembly() {
            return subassembly;
        }

        @Override
        public String toString() {
            return "AssemblyItem{" +
                   "itemKey='" + itemKey + '\'' +
                   ", name='" + name + '\'' +
                   ", qty=" + qty +
                   ", subassembly=" + subassembly +
                   '}';
        }
    }
}
