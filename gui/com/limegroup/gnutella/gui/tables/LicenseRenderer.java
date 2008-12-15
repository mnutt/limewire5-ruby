package com.limegroup.gnutella.gui.tables;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.limewire.util.NameValue;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.ImageManipulator;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.licenses.LicenseFactory;

class LicenseRenderer extends DefaultTableCellRenderer implements ThemeObserver {
    
    /**
     * The CC icon, disabled.
     *
     * Lazily constructed
     */
    private Icon CC_NOT_VALIDATED = null;
    
    /**
     * The Weedshare icon, disabled.
     *
     * Lazily constructed.
     */
    private Icon WEED_NOT_VALIDATED = null;
    
    /**
     * An unknown license, disabled.
     */
    private Icon UNKNOWN_NOT_VALIDATED = null;
    
    public LicenseRenderer() { 
        setHorizontalAlignment(JLabel.CENTER);
        ThemeMediator.addThemeObserver(this);
    }
    
    public void updateTheme() {
        CC_NOT_VALIDATED = null;
        WEED_NOT_VALIDATED = null;
        UNKNOWN_NOT_VALIDATED = null;
    }
    
    private Icon getIcon(String s) {
        if(s == null)
            return null;
        if(s.equals(LicenseFactory.CC_NAME))
            return GUIMediator.getThemeImage("cc");
        else if(s.equals(LicenseFactory.WEED_NAME))
            return GUIMediator.getThemeImage("weed");
        else if(s.equals(LicenseFactory.UNKNOWN_NAME))
            return GUIMediator.getThemeImage("forms_small");
        else
            return null;
    }
    
    /**
     * Retrieves the invalid icon.
     */
    private Icon getDisabledIcon(String s) {
        if(s == null) {
            return null;
        } else if(s.equals(LicenseFactory.CC_NAME)) {
            if(CC_NOT_VALIDATED == null)
                CC_NOT_VALIDATED = ImageManipulator.gray(getIcon(s));
            return CC_NOT_VALIDATED;
        } else if(s.equals(LicenseFactory.WEED_NAME)) {
            if(WEED_NOT_VALIDATED == null)
                WEED_NOT_VALIDATED = ImageManipulator.gray(getIcon(s));
            return WEED_NOT_VALIDATED;
        } else if(s.equals(LicenseFactory.UNKNOWN_NAME)) {
            if(UNKNOWN_NOT_VALIDATED == null)
                UNKNOWN_NOT_VALIDATED = ImageManipulator.gray(getIcon(s));
            return UNKNOWN_NOT_VALIDATED;
        } else {
            return null;
        }
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value, 
                                                   boolean isSel, 
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        NameValue nv = (NameValue)value;
        super.getTableCellRendererComponent(table, value, isSel, hasFocus, row, column);
        setText(null);
        if(nv != null) {
            Integer e = (Integer)nv.getValue();
            int i = e.intValue();
            switch(i) {
            case License.VERIFIED: setIcon(getIcon(nv.getName())); break;
            case License.UNVERIFIED:   setIcon(getDisabledIcon(nv.getName())); break;
            default: setIcon(null);
            }
        } else {
            setIcon(null);
        }
        return this;
    }
}
