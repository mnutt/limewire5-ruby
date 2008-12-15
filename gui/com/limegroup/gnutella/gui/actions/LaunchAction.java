/**
 * 
 */
package com.limegroup.gnutella.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.limewire.inspection.InspectablePrimitive;

import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;

public final class LaunchAction extends AbstractAction {

    private File file;
        
    @InspectablePrimitive("filesLaunched")
    private static int filesLaunched = 0;

    public LaunchAction (File file) {
        this.file = file;
        putValue(Action.NAME, I18n.tr
                ("Launch"));
        putValue(Action.SHORT_DESCRIPTION,
                I18n.tr("Launch Selected Files"));
        putValue(LimeAction.ICON_NAME, "LIBRARY_LAUNCH");
    }

    public void actionPerformed(ActionEvent ae) {
        ++filesLaunched;
        GUIUtils.launchOrEnqueueFile(file, false);
    }
}