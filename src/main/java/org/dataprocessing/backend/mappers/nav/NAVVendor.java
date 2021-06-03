package org.dataprocessing.backend.mappers.nav;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dataprocessing.utils.MapperUtils;
import org.dataprocessing.utils.Utils;

import java.util.LinkedList;
import java.util.List;

/**
 * Maps the NAV Vendor Sheet
 *
 * @author Nicholas Curl
 */
public class NAVVendor {

    /**
     * The instance of the logger
     */
    private static final Logger             logger      = LogManager.getLogger(NAVVendor.class);
    /**
     * The instance of the Utils class
     */
    private static final Utils              utils       = Utils.getInstance();
    /**
     * The instance of the MapperUtils class
     */
    private static final MapperUtils        mapperUtils = MapperUtils.getInstance();
    /**
     * The template associated with this mapping
     */
    private static final String             template    = "/templates/Vendor Template_MFG FINAL V1.xlsx";
    /**
     * The header of the template
     */
    private final        List<String>       header;
    /**
     * The table that stores the mapped data
     */
    private final        List<List<String>> mapTable;

    /**
     * The constructor for this class that creates the template header and the table to store the mapped data
     */
    public NAVVendor() {
        header = mapperUtils.getHeader(template);
        mapTable = mapperUtils.createMapTable(template);
    }

    /**
     * Gets the table of the mapped data
     *
     * @return The table of the mapped data
     */
    public List<List<String>> getMapTable() {
        return mapTable;
    }

    /**
     * Maps the specified vendor sheet to the new vendor template
     *
     * @param sheet The sheet to map
     */
    public void mapVendor(List<List<String>> sheet) {
        if (!sheet.isEmpty()) {
            for (int i = 1; i < sheet.size(); i++) {
                List<String> row = sheet.get(i);
                List<String> mapRow = new LinkedList<>();
                for (int j = 0; j < header.size(); j++) {
                    switch (j) {
                        case 0:
                            mapRow.add(j, row.get(0));
                            break;
                        case 2:
                            String cell = row.get(72);
                            if (cell.equalsIgnoreCase("Legal Entity")) {
                                mapRow.add(j, "FALSE");
                            }
                            else {
                                mapRow.add(j, "TRUE");
                            }
                            break;
                        case 7:
                            mapRow.add(j, row.get(2));
                            break;
                        case 8:
                            mapRow.add(j, row.get(42));
                            break;
                        case 11:
                            mapRow.add(j, row.get(41));
                            break;
                        case 13:
                            mapRow.add(j, row.get(8));
                            break;
                        case 15:
                            mapRow.add(j, row.get(33));
                            break;
                        case 18:
                            mapRow.add(j, "Mahaffey USA");
                            break;
                        case 23:
                            mapRow.add(j, row.get(4));
                            break;
                        case 24:
                            mapRow.add(j, row.get(5));
                            break;
                        case 25:
                            mapRow.add(j, row.get(6));
                            break;
                        case 26:
                            mapRow.add(j, row.get(40));
                            break;
                        case 27:
                            mapRow.add(j, row.get(39));
                            break;
                        case 47:
                            mapRow.add(j, "USA");
                            break;
                        case 48:
                            mapRow.add(j, row.get(19));
                            break;
                        case 51:
                            mapRow.add(j, row.get(63));
                            break;
                        case 52:
                            if (!utils.isBlankString(row.get(67))) {
                                mapRow.add(j, "TRUE");
                            }
                            else {
                                mapRow.add(j, "FALSE");
                            }
                            break;
                        case 62:
                            mapRow.add(j, row.get(1));
                            break;
                        default:
                            mapRow.add(j, "");
                            break;
                    }
                }
                mapTable.add(mapRow);
            }
        }
    }
}
