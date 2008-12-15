package com.limegroup.gnutella.gui.xml.editor;

import java.util.List;

import javax.swing.JPanel;

import org.limewire.util.NameValue;


public abstract class AbstractMetaEditorPanel extends JPanel {
    
    public AbstractMetaEditorPanel() {
        super();
        setOpaque(false);
    }
    
    public abstract boolean checkInput();
    
    public abstract List<NameValue<String>> getInput();
    
}
