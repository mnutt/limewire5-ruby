package org.limewire.ui.swing.action;

import org.limewire.ui.swing.util.GuiUtils;

/**
 * Allows the name of the menu to have an ampersand to 
 * mark the mnemonic of its name.
 */
public class MnemonicMenu extends javax.swing.JMenu {
    
    public MnemonicMenu(String name) {
        super();
        int mnemonicKeyCode = GuiUtils.getMnemonicKeyCode(name);
        name = GuiUtils.stripAmpersand(name);
        if (mnemonicKeyCode != -1) { 
            setMnemonic(mnemonicKeyCode);
        }
        setText(name);
    }
}
