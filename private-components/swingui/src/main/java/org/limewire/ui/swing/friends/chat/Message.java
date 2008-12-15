package org.limewire.ui.swing.friends.chat;


/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public interface Message {
    enum Type { Sent, Received };
    
    String getSenderName();
    
    String getFriendName();
    
    String getFriendID();
    
    Type getType();
    
    long getMessageTimeMillis();

    String format();
}
