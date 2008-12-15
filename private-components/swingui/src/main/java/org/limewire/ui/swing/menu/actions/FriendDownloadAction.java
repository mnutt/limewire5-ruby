package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Switches to the library view of the selected friend.
 */
@Singleton
public class FriendDownloadAction extends AbstractAction {

    @Inject
    public FriendDownloadAction() {
        super(I18n.tr("Download"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        
    }
}