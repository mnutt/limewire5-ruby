package com.limegroup.gnutella.gui.actions;

import java.awt.event.ActionEvent;

import com.limegroup.gnutella.gui.GUIMediator;

/**
 * Opens the given url in a browser.
 */
public class OpenLinkAction extends AbstractAction {

    private final String url;
    
    public OpenLinkAction(String url, String name) {
        this(url, name, null);
    }
    
    public OpenLinkAction(String url, String name, String description) {
        super(name);
        this.url = url;
        putValue(LONG_DESCRIPTION, description);
    }
    
    public void actionPerformed(ActionEvent e) {
        GUIMediator.openURL(url);
    }

}
