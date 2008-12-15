package com.limegroup.gnutella.gui.tables;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.limegroup.gnutella.gui.GUIUtils;


/** Renderer that can display {@link Linkable} objects in HTML. */
public class LinkRenderer extends DefaultTableCellRenderer {
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        
        if(value instanceof Linkable && ((Linkable)value).isLink()) {
            StringBuilder sb = new StringBuilder(30);
            sb.append("<html><a href=\"")
              .append(((Linkable)value).getLinkUrl())
              .append("\"");
            if(isSelected) {
                sb.append("color=\"")
                  .append(GUIUtils.colorToHex(table.getSelectionForeground()))
                  .append("\"");
            }
            sb.append(">")
              .append(value.toString().replaceAll(" ", "&NBSP;"))
              .append("</a></html>");
            value = sb.toString();
        }
        
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

}
