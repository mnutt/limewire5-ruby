/**
 * 
 */
package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;

import javax.swing.ActionMap;

import org.jdesktop.application.Application;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class ExitAfterTransferAction extends AbstractAction {
    
    @Inject
    public ExitAfterTransferAction() {
        // TODO fberger
        // super(I18n.tr("Exit After &Transfers"));
        super(I18n.tr("Exit After Transfers"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ActionMap map = Application.getInstance().getContext().getActionManager()
                .getActionMap();
        map.get("shutdownAfterTransfers").actionPerformed(e);
    }
}