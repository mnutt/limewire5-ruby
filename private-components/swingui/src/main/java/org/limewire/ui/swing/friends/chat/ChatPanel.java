package org.limewire.ui.swing.friends.chat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ChatPanel extends JPanel implements Displayable {
    private static final Log LOG = LogFactory.getLog(ChatPanel.class);
    private final ConversationPaneFactory conversationFactory;
    private final JPanel conversationPanel;
    private final ChatFriendListPane friendsPanel;
    private final Map<String, ConversationPane> chats;
    private final ChatTopPanel chatTopPanel;
    
    @Inject
    public ChatPanel(ConversationPaneFactory conversationFactory, IconLibrary icons, ChatFriendListPane friendsPanel,
            ChatTopPanel chatTopPanel) {
        super(new BorderLayout());
        
        this.chatTopPanel = chatTopPanel;
        this.conversationFactory = conversationFactory;
        this.friendsPanel = friendsPanel;
        this.chats = new HashMap<String, ConversationPane>();

        //Dimensions according to the spec
        setPreferredSize(new Dimension(400, 240));
        add(friendsPanel, BorderLayout.WEST);
        add(chatTopPanel, BorderLayout.NORTH);
        conversationPanel = new JPanel(new BorderLayout());
        setConversationPanel(buildMessagesPane());
        add(conversationPanel, BorderLayout.CENTER);
        
        EventAnnotationProcessor.subscribe(this);
        
    }
    
    void setMinimizeAction(Action minimizeAction) {
        chatTopPanel.setMinimizeAction(minimizeAction);
    }
    
    @Inject void register(ListenerSupport<XMPPConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTED:
                    handleSignon();
                    break;
                case DISCONNECTED:
                    handleSignoff();
                    break;
                }
            }
        });
    }

    private JPanel buildMessagesPane() {
        JPanel panel = new JPanel(new BorderLayout());
        JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        pane.setContentType("text/html");
        pane.addHyperlinkListener(new HyperlinkHandler());
        panel.add(pane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * A listener for hyperlink events in the chat messages pane.
     */
    private static class HyperlinkHandler implements HyperlinkListener {
        
        public HyperlinkHandler() {
        }
        
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (EventType.ACTIVATED == e.getEventType()) {
                LOG.debugf("Hyperlink clicked: {0}", e.getDescription());
            }
        }
    }
    
    @EventSubscriber
    public void handleSelectedConversation(ConversationSelectedEvent event) {
        ChatFriend chatFriend = event.getFriend();
        LOG.debugf("ConversationSelectedEvent with friend: {0}", chatFriend.getName());
        ConversationPane chatPane = chats.get(chatFriend.getID());
        if (chatPane == null) {
            chatPane = conversationFactory.create(event.getWriter(), chatFriend, getLoggedInID());
            chats.put(chatFriend.getID(), chatPane);
        }
        
        if (conversationPanel.getComponent(0) != chatPane && event.isLocallyInitiated()) {
            setConversationPanel(chatPane);
        }
        
        chatPane.handleDisplay();

        event.unlock();
        LOG.debug("unlocked");
    }

    private void setConversationPanel(JComponent comp) {
        conversationPanel.removeAll();
        conversationPanel.add(comp, BorderLayout.CENTER);
        conversationPanel.validate();
    }
    
    private void handleSignon() {
        setConversationPanel(buildMessagesPane());
    }
    
    private void handleSignoff() {
        //close all open chats when we sign-off
        String[] chatKeys = chats.keySet().toArray(new String[chats.size()]);
        for(String key : chatKeys) {
            closeChat(key);
        }
        setConversationPanel(new JPanel());
    }

    @EventSubscriber
    public void handleCloseChat(CloseChatEvent event) {
        ConversationPane conversationPane = chats.get(event.getFriend().getID());
        if(conversationPane != null) {
            conversationPane.setChatStateGone();
            setConversationPanel(buildMessagesPane());
        }
    }
    
    private void closeChat(String chatKey) {
        LOG.debugf("Closing chat panel for {0}", chatKey);
        ConversationPane conversation = chats.remove(chatKey);
        conversation.destroy();
    }

    @Override
    public void handleDisplay() {
        Component component = conversationPanel.getComponent(0);
        if (component instanceof Displayable) {
            ((Displayable)component).handleDisplay();
        }
    }
    
    public void setLoggedInID(String id) {
        friendsPanel.setLoggedInID(id);    
    }
    
    private String getLoggedInID() {
        return friendsPanel.getLoggedInID();
    }
}
