package org.limewire.ui.swing.friends.chat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Inject;

/**
 * Main Chat window. This is the parent container for the chat window.
 */
public class ChatPanel extends JXPanel implements Displayable {
    private static final Log LOG = LogFactory.getLog(ChatPanel.class);
    private final ConversationPaneFactory conversationFactory;
    private final JPanel conversationPanel;
    private final ChatFriendListPane friendsPanel;
    private final Map<String, ConversationPane> chats;
    private final ChatTopPanel chatTopPanel;
    
    @Resource private Color border;
    
    @Inject
    public ChatPanel(ConversationPaneFactory conversationFactory, ChatFriendListPane friendsPanel,
            ChatTopPanel chatTopPanel) {
        GuiUtils.assignResources(this);

        setLayout(new MigLayout("gap 0, insets 0 0 0 2, fill"));
        setBorder(BorderFactory.createMatteBorder(1,1,0,1, border));
        
        this.chatTopPanel = chatTopPanel;
        this.conversationFactory = conversationFactory;
        this.friendsPanel = friendsPanel;
        this.chats = new HashMap<String, ConversationPane>();

        setPreferredSize(new Dimension(400, 240));
        add(chatTopPanel, "dock north");
        add(friendsPanel, "dock west");

        conversationPanel = new JPanel(new BorderLayout());
        setConversationPanel(buildMessagesPane());
        add(conversationPanel, "dock center");
        
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
    
    /**
	 * Starts a conversation chat with this friend.
	 */
    public void fireConversationStarted(String friendId) {
        friendsPanel.fireConversationStarted(friendId);
    }
    
    @EventSubscriber
    public void handleSelectedConversation(ConversationSelectedEvent event) {
        ChatFriend chatFriend = event.getFriend();
        LOG.debugf("ConversationSelectedEvent with friend: {0}", chatFriend.getName());
        ConversationPane chatPane = chats.get(chatFriend.getID());
        if (chatPane == null) {
            chatPane = conversationFactory.create(event.getWriter(), chatFriend, friendsPanel.getLoggedInId());
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

    public void markActiveConversationRead() {
        friendsPanel.markActiveConversationRead();
    }
}
