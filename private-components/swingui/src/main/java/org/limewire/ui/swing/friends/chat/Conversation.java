package org.limewire.ui.swing.friends.chat;

import java.util.Map;

/**
 * Interface for chat window ui object.  Intended to be a way
 * to get information about and control what happens to the
 * chat window, other than by way of keyboard input and
 * received instant messages.
 *<p>
 * Make sure that methods here are called
 * from within the EDT.
 *
 */
public interface Conversation {

    /**
     * Updates the display of all the messages.
     */
    public void displayMessages();


    /**
     * @return the {@link ChatFriend} associated with this conversation
     */
    public ChatFriend getChatFriend();

    /**
     * @return a read-only map of file ID to file offer message.
     * It is a snapshot of all the file offer messages
     *
     */
    public Map<String, MessageFileOffer> getFileOfferMessages();

    
    // TODO: create a way to get/search for any message(s)
}
