package com.limegroup.gnutella.gui.search;

import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.limegroup.gnutella.gui.tables.ColumnSelectionMenu;
import com.limegroup.gnutella.gui.tables.DataLineModel;
import com.limegroup.gnutella.gui.tables.LimeJTable;
import com.limegroup.gnutella.gui.xml.XMLUtils;

final class SearchColumnSelectionMenu extends ColumnSelectionMenu {

    public SearchColumnSelectionMenu(LimeJTable table) {
        super(table);
    }
    
    /**
     * Overriden to add columns from the same schema to a submenu.
     */
    @Override
    protected void addTableColumnChoices(ActionListener listener,
                                         DataLineModel model,
                                         LimeJTable table) {
        String currentSchema = "";
        JMenu currentSchemaMenu = null;        
        for( int i = 0; i < model.getColumnCount(); i++) {
            JMenuItem item = createColumnMenuItem(listener, model, table, i);
            String schema = schemaOf(item);
            if(schema != null) {
                if(!schema.equals(currentSchema)) {
                    currentSchema = schema;
                    currentSchemaMenu = new JMenu(currentSchema);
                    _menu.add(currentSchemaMenu);
                }
                assert currentSchemaMenu != null;
                currentSchemaMenu.add(item);
            } else {
                _menu.add( item );
            }
        }
    }
    
    /**
     * Gets the name of the schema of a JMenuItem.
     */
    private String schemaOf(JMenuItem item) {
        String field = (String)item.getClientProperty(COLUMN_ID);
        return XMLUtils.getTitleForSchemaFromField(field);
    }
}