package org.limewire.ui.swing.friends.chat;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.limewire.friend.api.MessageWriter;
import org.limewire.inject.LazySingleton;

import com.google.inject.Inject;

/**
 * Contains all the ConversationPanes. Each friend that has an open conversation
 * will have a unique ConversationPane. Only one ConversationPane can be open at 
 * a given time. 
 */
@LazySingleton
class ConversationPanel {
    private final ConversationPaneFactory conversationFactory;
    private final JPanel component;
    /**  Map of friendId's to the conversation pane. */
    private final Map<String, ConversationPane> chats;
    
    /**
     * Friend who's conversation is currently displayed, null if no conversation is
     * being displayed.
     */
    private ChatFriend selectedConversation = null;
    
    @Inject
    public ConversationPanel(ConversationPaneFactory conversationFactory) {       
        this.conversationFactory = conversationFactory;
        component = new JPanel(new BorderLayout());
        this.chats = new HashMap<String, ConversationPane>();
    }
    
    /**
     * Returns the panel containing the conversations.
     */
    public JComponent getComponent() {
        return component;
    }
    
    private void setConversationPanel(JComponent chatComponent) {
        component.removeAll();
        component.add(chatComponent, BorderLayout.CENTER);
        component.repaint();
    }
    
    /**
     * Displays the conversation with the given ChatFriend.
     */
    public void displayConverstaion(ChatFriend chatFriend) {
        ConversationPane chatPane = chats.get(chatFriend.getID());
        selectedConversation = chatFriend;
        selectedConversation.setHasUnviewedMessages(false);
        setConversationPanel(chatPane);
    }
    
    /**
     * Returns the ChatFriend whose conversation is currently 
     * displayed. If no conversation is selected returns null.
     */
    public ChatFriend getCurrentConversationFriend() {
        return selectedConversation;
    }
    
    /**
     * Returns true if a conversation already exists with the given friend,
     * false otherwise.
     */
    public boolean hasConversation(ChatFriend chatFriend) {
        return chats.containsKey(chatFriend.getID());
    }
    
    /**
     * Destroys a conversation with a given friend.
     */
    public void removeConversation(ChatFriend chatFriend) {
        if(chatFriend.equals(selectedConversation)) {
            selectedConversation = null;
            setConversationPanel(new JPanel());
        }        
        if(hasConversation(chatFriend)) {
            chatFriend.stopChat();
            ConversationPane conversation = chats.remove(chatFriend.getID());
            conversation.dispose();
        }
    }
    
    /**
     * Destroys all conversations with all friends.
     */
    public void removeAllConversations() {
        selectedConversation = null;
        setConversationPanel(new JPanel());
        
        for(String key : chats.keySet()) {
            chats.get(key).dispose();
        }
        chats.clear();
    }
    
    /**
     * Starts a new chat with the given friend.
     */
    public void startNewChat(ChatFriend chatFriend, MessageWriter messageWriter) {
        ConversationPane chatPane = conversationFactory.create(messageWriter, chatFriend);
        chats.put(chatFriend.getID(), chatPane);
        selectedConversation = chatFriend;
        selectedConversation.setHasUnviewedMessages(false);
        setConversationPanel(chatPane);
    }
}
