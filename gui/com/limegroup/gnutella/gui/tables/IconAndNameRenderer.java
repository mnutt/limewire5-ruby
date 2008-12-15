package com.limegroup.gnutella.gui.tables;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renders an icon along with a label.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class IconAndNameRenderer extends DefaultTableCellRenderer {
	
	/**
	 * Constructs a new IconAndNameRenderer with the Icon aligned to the left
	 * of the text, with a text gap of 5 between the icon and text.
	 */
	public IconAndNameRenderer() {
	    super();
        setHorizontalAlignment(LEFT);
        setIconTextGap(5);
        setHorizontalTextPosition(RIGHT);
	}

	/**
	 * Returns the <tt>Component</tt> that displays the icons & names
	 * based on the <tt>IconAndNameHolder</tt> object.
	 */
	@Override
    public Component getTableCellRendererComponent
		(JTable table,Object value,boolean isSelected,
		 boolean hasFocus,int row,int column) {
		    
        IconAndNameHolder in = (IconAndNameHolder)value;
        Icon icon = null;
        String name = null;
        if(in != null) {
            icon = in.getIcon();
            name = in.getName();
        }   
        setIcon(icon);
        return super.getTableCellRendererComponent(
            table, name, isSelected, hasFocus, row, column);
	}
}
