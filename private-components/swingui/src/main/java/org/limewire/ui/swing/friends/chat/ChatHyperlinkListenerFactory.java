package org.limewire.ui.swing.friends.chat;

/**
 * Factory interface for {@link ChatHyperlinkListener}.
 */
public interface ChatHyperlinkListenerFactory {

    ChatHyperlinkListener create(Conversation chat);
}
