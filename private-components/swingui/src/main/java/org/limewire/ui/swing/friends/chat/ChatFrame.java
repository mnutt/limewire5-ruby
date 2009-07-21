package org.limewire.ui.swing.friends.chat;

import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.mainframe.UnseenMessageListener;
import org.limewire.ui.swing.util.VisibleComponent;

/**
 * The main panel where conversations with friends are displayed. 
 */
public interface ChatFrame extends Resizable, VisibleComponent {

    /**
     * Starts a new conversation with this friend.
     */
    public void fireConversationStarted(String friendId);
    
    /**
     * Adds a listener for changes in messages that are displayed.
     */
    public void setUnseenMessageListener(UnseenMessageListener unseenMessageListener);
    
    /**
     * Returns the friendId of the conversation that is currently being shown.
     */
    public String getLastSelectedConversationFriendId();
}
