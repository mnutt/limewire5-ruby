package org.limewire.ui.swing.action;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import org.limewire.ui.swing.components.PlainCheckBoxMenuItemUI;
import org.limewire.ui.swing.components.PlainMenuItemUI;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Allows the text of the menu to have an ampersand to 
 * mark the mnemonic of its name.
 */
public class MnemonicMenu extends javax.swing.JMenu {
    
    public MnemonicMenu(String text) {
        super(text);
    }
    
    @Override
    public void setText(String text) {
        int mnemonicKeyCode = GuiUtils.getMnemonicKeyCode(text);
        text = GuiUtils.stripAmpersand(text);
        if (mnemonicKeyCode != -1) { 
            setMnemonic(mnemonicKeyCode);
        }
        super.setText(text);
    }
    
    
    @Override
    public JMenuItem add(Action action) {
        JMenuItem item = super.add(action);
        item.setUI(new PlainMenuItemUI());
        return item;
    }
    
    @Override
    public JMenuItem add(JMenuItem item) {
        
        if (item instanceof JCheckBoxMenuItem) {
            item.setUI(new PlainCheckBoxMenuItemUI());
        }
        
        JMenuItem itemReturned = super.add(item);
        return itemReturned;
    }
}
