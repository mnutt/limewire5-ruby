package org.limewire.ui.swing.friends.chat;


/**
 * A unit of communication between 2 chat clients that is displayed
 * in the chat window.  Messages stay in the chat window
 * for the duration of the chat.
 * 
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public interface Message {
    /**
     * Sent     == message sent with destination of friend
     * Received == message received from friend
     * Server   == server chat status related message
     */
    enum Type { Sent, Received, Server };

    /**
     * @return the sender display name
     */
    String getSenderName();

    /**
     * @return the friend ID (usually the login id)
     */
    String getFriendID();

    /**
     * @return message type
     */
    Type getType();

    /**
     * @return message timestamp in milliseconds
     */
    long getMessageTimeMillis();

    /**
     *
     * @return HTML formatted String representation of the message
     */
    String format();
}
