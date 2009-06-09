package org.limewire.ui.swing.friends.chat;

import org.limewire.core.api.friend.client.MessageWriter;


/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public interface ConversationPaneFactory {
    ConversationPane create(MessageWriter writer, ChatFriend chatFriend, String loggedInID);
}
