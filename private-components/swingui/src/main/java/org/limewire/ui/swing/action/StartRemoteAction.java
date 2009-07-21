package org.limewire.ui.swing.action;

import java.awt.event.ActionEvent;

import org.limewire.http.mongrel.MongrelManager;
import org.limewire.ui.swing.util.I18n;

public class StartRemoteAction extends AbstractAction {
    
    private MongrelManager mongrelManager;
    
    public StartRemoteAction(MongrelManager mongrelManager) {
        super(I18n.tr("Start Remote"), null);
        this.mongrelManager = mongrelManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        mongrelManager.start();
    }
}
