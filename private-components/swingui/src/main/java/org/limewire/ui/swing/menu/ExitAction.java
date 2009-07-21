/**
 * 
 */
package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.event.ExitApplicationEvent;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

class ExitAction extends AbstractAction {
    
    @Inject
    public ExitAction() {
        super(I18n.tr("E&xit"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new ExitApplicationEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                "Shutdown")).publish();
    }
}