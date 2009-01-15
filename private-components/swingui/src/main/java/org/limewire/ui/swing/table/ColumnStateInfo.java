package org.limewire.ui.swing.table;

import org.limewire.ui.swing.settings.TablesHandler;

/**
 * Keeps state about a given column. Contains the model index, unique id, default and saved values regarding
 * order, visibility and width. 
 */
public class ColumnStateInfo {
    private final int modelIndex;
    private final String id;
    private final String name;
    private final int defaultWidth;
    private final boolean isShownAtStartup;
    private final boolean isHideable;
    
    private int preferredIndex;
    private final boolean isDefaultlyShown;
    
    public ColumnStateInfo(int modelIndex, String id, String name, int defaultWidth, boolean isShownAtStartup, boolean isHideable) {
       this.id = id;
       this.name = name;
       this.isHideable = isHideable;
       this.isDefaultlyShown = isShownAtStartup;
       this.modelIndex = modelIndex;
       
       this.isShownAtStartup = TablesHandler.getVisibility(id, isShownAtStartup).getValue();
       this.defaultWidth = TablesHandler.getWidth(id, defaultWidth).getValue();
       this.preferredIndex = TablesHandler.getOrder(id, modelIndex).getValue();
    }
    
    /**
     * Returns the model index for this column
     */
    public int getModelIndex() {
        return modelIndex;
    }
    
    /**
     * Returns the preferred ViewIndex in the ColumnModel. If the column is not visible
     * this value is meaningless. If the column is in its default position, this value
     * will be the same as the ModelIndex.
     */
    public int getPreferredViewIndex() {
        return preferredIndex;
    }
    
    /**
     *	Unique identifier to read/write to disk with
	 */
    public String getId() {
        return id;
    }
    
    /**
     * Name to display in the column header
     */
    public String getName() {
        return name;
    }
    
    public int getDefaultWidth() {
        return defaultWidth;
    }
    
    /**
     * Returns the current visiblity state of this column
     */
    public boolean isShown() {
        return isShownAtStartup;
    }
    
    /**
     * Returns true if this column can be hidden, false otherwise
     */
    public boolean isHideable() {
        return isHideable;
    }
    
    /**
     * Returns the default visiblity state for this column
     */
    public boolean isDefaultlyShown() {
        return isDefaultlyShown;
    }

    public void setPreferredViewIndex(int i) {
        preferredIndex = i;
    }
}
