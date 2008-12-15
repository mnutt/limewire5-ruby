package org.limewire.ui.swing.search.resultpanel;

import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.VisibleTableFormat;

import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;


/**
 * This class is the base class for each of the TableFormat classes
 * that describe the various table views of search results.
 */
public abstract class ResultsTableFormat<E> implements VisibleTableFormat<E>, AdvancedTableFormat<E>, WritableTableFormat<E> {

    protected VisualSearchResult vsr;

    @Override
    public Class getColumnClass(int index) {
        return String.class;
    }

    public Comparator getColumnComparator(int index) {
        return GlazedLists.comparableComparator();
    }

    /**
     * Gets the value of a given property.
     * @param key the property key or name
     * @return the property value
     */
    protected Object getProperty(FilePropertyKey key) {
        return vsr.getProperty(key);
    }

    /**
     * Gets the String value of a given property.
     * @param key the property key or name
     * @return the String property value
     */
    protected String getString(FilePropertyKey key) {
        Object value = vsr.getProperty(key);
        return value == null ? "?" : value.toString();
    }
    
    abstract public int getNameColumn();

    abstract public boolean isEditable(VisualSearchResult vsr, int column);// {

    public VisualSearchResult setColumnValue(
        VisualSearchResult vsr, Object value, int index) {
        // do nothing with the new value
        return vsr;
    }
    
    public FromComparator getFromComparator() {
        return new FromComparator();
    }
    
    /**
     * Compares the number of files being shared. 
     */
    public static class FromComparator implements Comparator<VisualSearchResult> {
        @Override
        public int compare(VisualSearchResult o1, VisualSearchResult o2) {
            int size1 = o1.getSources().size();
            int size2 = o2.getSources().size();
            
            if(size1 == size2)
                return 0;
            else if(size1 > size2)
                return 1;
            else 
                return -1;
        }
    }

}