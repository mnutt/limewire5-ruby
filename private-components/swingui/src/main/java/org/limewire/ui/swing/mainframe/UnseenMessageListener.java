package org.limewire.ui.swing.mainframe;

public interface UnseenMessageListener {
    void messageReceivedFrom(String senderId, boolean chatIsVisible);
    
    void conversationSelected(String chatId);
    
    void clearUnseenMessages();
}
