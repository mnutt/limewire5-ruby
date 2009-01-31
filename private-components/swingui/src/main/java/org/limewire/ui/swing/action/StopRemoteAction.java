package org.limewire.ui.swing.action;

import java.awt.event.ActionEvent;

import org.limewire.http.mongrel.MongrelManager;
import org.limewire.ui.swing.util.I18n;

public class StopRemoteAction extends AbstractAction {
    
    private MongrelManager mongrelManager;

    public StopRemoteAction(MongrelManager mongrelManager) {
        super(I18n.tr("Stop Remote"), null);
        this.mongrelManager = mongrelManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        mongrelManager.stop();
    }
}
