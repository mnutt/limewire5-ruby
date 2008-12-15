package com.limegroup.gnutella.gui.search;

import javax.swing.Icon;

import org.limewire.core.settings.TablesHandler;

import com.limegroup.gnutella.gui.tables.LimeTableColumn;

/**
 * Extends LimeTableColumn to store current width/order/visibility information.
 *
 * Necessary for SearchColumnPreferenceHandler, to store data in memory instead
 * of disk, since multiple tables are active at once.
 */
class SearchColumn extends LimeTableColumn {
    
    private int _width;
    private int _order;
    private boolean _visible;
    
//    /**
//     * Creates a new column.
//     */
//    public SearchColumn(int model, final String id,
//                    int width, boolean vis, Class<?> clazz) {
//        this(model, id, GUIMediator.getStringResource(id),
//             width, vis, clazz);
//    }
//    
//    /**
//     * Creates a new column.
//     */
//    public SearchColumn(int model, final String id, final Icon icon,
//                    int width, boolean vis, Class<?> clazz) {
//        this(model, id, GUIMediator.getStringResource(id), icon,
//             width, vis, clazz);
//    }
    
    /**
     * Creates a new column.
     */
    public SearchColumn(int model, final String id, final String name,
                    int width, boolean vis, Class<?> clazz) {
        this(model, id, name, null, width, vis, clazz);
    }
    
    /**
     * Creates a new column.
     */
    public SearchColumn(int model, final String id, final String name,
                    Icon icon, int width, boolean vis, Class<?> clazz) {
        super(model, id, name, icon, width, vis, clazz);
        
        _visible = TablesHandler.getVisibility(id, vis).getValue();
        _order = TablesHandler.getOrder(id, model).getValue();
        _width = TablesHandler.getWidth(id, width).getValue();
    }
    
    void setCurrentWidth(int width) {
        _width = width;
    }
    
    void setCurrentOrder(int order) {
        _order = order;
    }
    
    void setCurrentVisibility(boolean visible) {
        _visible = visible;
    }
    
    int getCurrentWidth() {
        return _width;
    }
    
    int getCurrentOrder() {
        return _order;
    }
    
    boolean getCurrentVisibility() {
        return _visible;
    }
}
    
    