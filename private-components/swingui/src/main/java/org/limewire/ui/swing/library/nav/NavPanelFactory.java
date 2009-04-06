package org.limewire.ui.swing.library.nav;

import javax.swing.Action;

import org.limewire.core.api.friend.Friend;

/**
 * Creates a NavPanel for a given Friend.
 */
interface NavPanelFactory {
    
    NavPanel createNavPanel(Action action, Friend friend);
}
