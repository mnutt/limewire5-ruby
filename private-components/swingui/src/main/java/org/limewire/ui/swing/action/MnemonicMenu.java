package org.limewire.ui.swing.action;

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
}
