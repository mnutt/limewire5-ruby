package com.limegroup.gnutella.gui.search;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;

/** Draws ResultSpeed's appropriately colorized */
class ResultSpeedRenderer extends DefaultTableCellRenderer 
	implements ThemeObserver {
    /** The color to show measured speeds in.  This is dark green,
     *  similar to the color on the connected status icon. */
    public static Color _measuredColor;
    public static Color _unmeasuredColor;

	/**
	 * Creates a new <tt>ResultSpeedRenderer</tt> instance, making it a
	 * theme listener.
	 */
    ResultSpeedRenderer() {
		updateTheme();
		ThemeMediator.addThemeObserver(this);
	}

	// inherit doc comment
	public void updateTheme() {
		_measuredColor = ThemeFileHandler.SEARCH_RESULT_SPEED_COLOR.getValue();
		_unmeasuredColor = ThemeFileHandler.WINDOW8_COLOR.getValue();			
	}

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value, 
                                                   boolean isSel, 
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {        
        //Note that we don't display all the information in the column.
        ResultSpeed speed=(ResultSpeed)value;
        String tag = (speed == null ? "" : speed.stringValue());
        Component ret=super.getTableCellRendererComponent(
            table, tag, isSel, hasFocus, row, column);                           
        //Render measured speeds in green, others in black.  The second call is
        //necessary to prevent everything from turning green, since one renderer
        //is shared among all cells.
        if (speed != null && speed.isMeasured())
            ret.setForeground(_measuredColor);
        else
            ret.setForeground(_unmeasuredColor);
        return ret;
    }
}
