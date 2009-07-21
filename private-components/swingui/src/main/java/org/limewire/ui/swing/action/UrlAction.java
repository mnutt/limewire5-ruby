package org.limewire.ui.swing.action;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.ui.swing.util.NativeLaunchUtils;

public class UrlAction extends AbstractAction {
    private final String url;
    
    public UrlAction(String url) {
        this(url, url);
    }
    
    public UrlAction(String name, String url) {
        super(name);
        this.url = url;
        putValue(Action.SHORT_DESCRIPTION, url);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        NativeLaunchUtils.openURL(url);
    }
}