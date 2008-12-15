/**
 * 
 */
package com.limegroup.gnutella.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.library.LibraryMediator;

public final class ShowInLibraryAction extends AbstractAction {

    private File file;

    public ShowInLibraryAction(File file) {
        this.file = file;

        putValue(Action.NAME, I18n.tr
                ("Show in Library"));
        putValue(Action.SHORT_DESCRIPTION,
                I18n.tr("Show Download in Library"));
    }

    public void actionPerformed(ActionEvent ae) {
        GUIMediator.instance().setWindow(GUIMediator.Tabs.LIBRARY);
        LibraryMediator.setSelectedFile(file);
    }
}