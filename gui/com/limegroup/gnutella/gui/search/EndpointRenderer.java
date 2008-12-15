package com.limegroup.gnutella.gui.search;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;

/** Draws EndpointHolder's appropriately colorized */
class EndpointRenderer extends DefaultTableCellRenderer 
                                                     implements ThemeObserver {

	private static Color _nonPrivateColor;

	private static Color _privateColor;
    
    private static Color _selectedPrivateColor;


    public EndpointRenderer() {
        updateTheme();
        ThemeMediator.addThemeObserver(this);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value, 
                                                   boolean isSel, 
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        EndpointHolder e = (EndpointHolder)value;
        
        Component ret = super.getTableCellRendererComponent(
            table, e, isSel, hasFocus, row, column);

        //Render private IP addresses in red, leave the others alone.
        if (e != null && e.isPrivateAddress()) {
            if (!isSel)
                ret.setForeground(_privateColor);
            else
                ret.setForeground(_selectedPrivateColor);            
        } else if(!isSel) {
            // leave selected cells alone.
            ret.setForeground(_nonPrivateColor);
        }

        return ret;
    }

    public void updateTheme() {
        _nonPrivateColor = ThemeFileHandler.WINDOW8_COLOR.getValue();
        _privateColor = ThemeFileHandler.SEARCH_PRIVATE_IP_COLOR.getValue();
        _selectedPrivateColor = ThemeFileHandler.SEARCH_SELECTED_PRIVATE_IP_COLOR.getValue();
    }
}
