package com.limegroup.gnutella.gui.tables;

import javax.swing.Icon;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * A personalized TableColumn for storing extended information.
 * This class provides support for storing:
 * <ul>
 * <li> The model number of the column
 *      (this doubles as the default order number) </li>
 * <li> The messageBundle ID of the column </li>
 * <li> The default width of the column
 *      (as opposed to the current preferredSize) </li>
 * <li> The default visibility of the column
 *      (as opposed to the current visibility) </li>
 * <li> The class of this column </li>
 * </ul>
 */
public class LimeTableColumn extends TableColumn {

    /**
     * Variable for the HeaderRenderer for all components.
     */
    public static final TableCellRenderer HEADER_RENDERER =
        new SortHeaderRenderer();
        
    /**
     * Variable for an invisible HeaderRenderer.
     */
    public static final TableCellRenderer INVIS_RENDERER;
    
    static {
        SortHeaderRenderer rnd = new SortHeaderRenderer();
        rnd.setAllowIcon(false);
        INVIS_RENDERER = rnd;
    }

    private final boolean defaultVisibility;
    private final int defaultWidth;
    private final String messageId;
    private final String name;
    private final Icon icon;
    private final Class<?> clazz;
    
    private boolean initialized = false;

    @Override
    public String toString() {
        return messageId;
    }

    /**
     * Creates a new column.
     */
    public LimeTableColumn(int model, final String id, final String name,
                    int width, boolean vis, Class<?> clazz) {
        this(model, id, name, null, width, vis, clazz);
    }
    
    /**
     * Creates a new column.
     */
    public LimeTableColumn(int model, final String id, final String name,
                    final Icon icon, int width, boolean vis, Class<?> clazz) {
        super(model);
        initialized = true;

        this.defaultVisibility = vis;

        this.defaultWidth = width;
        if( defaultWidth != -1 )
            super.setPreferredWidth(width);

        this.messageId = id;
        super.setIdentifier(id);

        this.name = name;
        this.icon = icon;

        this.clazz = clazz;

        setHeaderVisible(true);
    }

    /**
     * Sets the visibility of the header.
     *
     * Returns this so that it can be used easily for assigning
     * variables.
     */
    public LimeTableColumn setHeaderVisible(boolean vis) {
        if(vis) {
            super.setHeaderRenderer(HEADER_RENDERER);
            if(icon != null) {
                super.setHeaderValue(icon);
            } else if(name != null) {
                super.setHeaderValue(name);
            } else {
                super.setHeaderValue("");
            }
        } else {
            super.setHeaderRenderer(INVIS_RENDERER);
            super.setHeaderValue("");
        }
        return this;
    }

    /**
     * Gets the default visibility for this column.
     */
    public boolean getDefaultVisibility() {
        return defaultVisibility;
    }

    /**
     * Gets the default width for this column.
     */
    public int getDefaultWidth() {
        return defaultWidth;
    }

    /**
     * Gets the default order for this column.
     */
    public int getDefaultOrder() {
        return getModelIndex();
    }

    /**
     * Get the name, as a string.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the Icon.
     */
    public Icon getIcon() {
        return icon;
    }
     
    /**
     * Gets the class of this column.
     */
    public Class<?> getColumnClass() {
        return clazz;
    }

    /**
     * Gets the Id as a string.
     */
    public String getId() {
        return messageId;
    }
    
    /**
     * The following methods are overridden to ensure that we never
     * accidentally change the default values.  This is absolutely
     * necessary so that the DefaultColumnPreferenceHandler can correctly
     * write the default values to the settings.
     */

    /**
     * Disallows changing of model number
     */
    @Override
    public void setModelIndex(int idx) {
        if(!initialized) return;
        throw new IllegalStateException("cannot change model index");
    }

    /**
     * Disallows changing of header value
     */
    @Override
    public void setHeaderValue(Object val) {
        if(!initialized) return;
        throw new IllegalStateException("cannot change header value");
    }

    /**
     * Disallows changing of identifier
     */
    @Override
    public void setIdentifier(Object id) {
        if(!initialized) return;
        throw new IllegalStateException("cannot change id");
    }
}

        
