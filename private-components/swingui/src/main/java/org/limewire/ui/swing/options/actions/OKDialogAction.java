package org.limewire.ui.swing.options.actions;

import java.awt.Container;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

public class OKDialogAction extends AbstractAction {

    public OKDialogAction() {
        putValue(Action.NAME, I18n.tr("OK"));
        putValue(Action.SHORT_DESCRIPTION, I18n.tr("Keep any changes made"));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        JComponent comp = (JComponent)e.getSource();
        Container dialog = comp.getRootPane().getParent();
        if(dialog != null && dialog instanceof JDialog) {
            ((JDialog)dialog).dispose();
        }
    }
}
