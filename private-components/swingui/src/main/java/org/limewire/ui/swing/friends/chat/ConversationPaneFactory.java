package org.limewire.ui.swing.friends.chat;

import org.limewire.friend.api.MessageWriter;


/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public interface ConversationPaneFactory {
    ConversationPane create(MessageWriter writer, ChatFriend chatFriend);
}
