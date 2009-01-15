package org.limewire.ui.swing.util;

import java.util.List;

/**
 * Defines the sorting behavior for tables using event lists.
 */
public interface EventListTableSortFormat {

    /**
     * Returns the default sort column.  The method returns -1 if the default
     * sort column is not specified.
     * 
     * @return the model index of the default column
     */
    int getDefaultSortColumn();

    /**
     * Returns a List of the secondary sort columns associated with the 
     * specified column.  The columns are listed in order of significance, so
     * index 0 is most significant.  An empty list is returned if there are no
     * secondary sort columns; the method never returns null.
     * 
     * @param column the model index of the primary column
     * @return List of model indices for secondary columns
     */
    List<Integer> getSecondarySortColumns(int column);

}
