package com.limegroup.gnutella.gui.actions;

import javax.swing.Icon;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;

/**
 * Abstract class that allows the name of the action to have an ampersand to 
 * mark the mnemonic of the action in its name.
 * 
 * A call to {@link #putValue(String, Object) putValue(Action.Name, "Hello &World")}
 * will set the name of the action to "Hello World" and its menomonic to 'W'.
 * 
 * 
 */
public abstract class AbstractAction extends javax.swing.AbstractAction {

    public AbstractAction(String name, Icon icon) {
        super(name, icon);
    }

    public AbstractAction(String name) {
        super(name);
    }
    
    public AbstractAction() {
    }
    
    @Override
    public void putValue(String key, Object newValue) {
        // parse out mnemonic key for action name
        if (key.equals(NAME)) {
            String name = (String)newValue;
            newValue = GUIUtils.stripAmpersand(name);
            int mnemonicKeyCode = GUIUtils.getMnemonicKeyCode(name);
            if (mnemonicKeyCode != -1) { 
            	super.putValue(MNEMONIC_KEY, mnemonicKeyCode);
            }
        }
        super.putValue(key, newValue);
    }

    /**
     * Swing thread-safe way to enable/disable the action from any thread. 
     */
    public void setEnabledLater(final boolean enabled) {
        GUIMediator.safeInvokeLater(new Runnable() {
            public void run() {
                setEnabled(enabled);
            }
        });
    }
}
