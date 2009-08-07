package org.limewire.ui.swing.friends.chat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.Network.Type;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.miginfocom.swing.MigLayout;

/**
 * Main Chat window. This is the parent container for the chat window.
 */
@Singleton
public class ChatPanel extends JXPanel implements Displayable {
    private static final Log LOG = LogFactory.getLog(ChatPanel.class);
    private final ConversationPaneFactory conversationFactory;
    private final JPanel conversationPanel;
    private final ChatFriendListPane friendsPanel;
    private JComponent facebookPanel;
    private final Map<String, ConversationPane> chats;
    
    private boolean isFacebook = false;
    
    @Resource private Color border;
    
    @Inject
    public ChatPanel(ConversationPaneFactory conversationFactory, ChatFriendListPane friendsPanel,
            ChatTopPanel chatTopPanel) {
        GuiUtils.assignResources(this);

        setLayout(new MigLayout("gap 0, insets 0 0 0 2, fill"));
        setBorder(BorderFactory.createMatteBorder(1,1,0,1, border));
        
        this.conversationFactory = conversationFactory;
        this.friendsPanel = friendsPanel;
        this.chats = new HashMap<String, ConversationPane>();

        setPreferredSize(new Dimension(400, 240));
        add(chatTopPanel, "dock north");
        add(friendsPanel, "dock west, hidemode 3");

        conversationPanel = new JPanel(new BorderLayout());
        setConversationPanel(buildMessagesPane());
        add(conversationPanel, "dock center");
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    
    @Inject void register(ListenerSupport<FriendConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTED:
                    FriendConnection connection = event.getSource();
                    isFacebook = connection != null && connection.getConfiguration().getType() == Type.FACEBOOK;
                    
                    if(isFacebook){
                        handleFacebook();
                    } else {
                        handleSignon();
                    }
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
       if(!isFacebook){
           friendsPanel.fireConversationStarted(friendId);
       }
    }
    
    @EventSubscriber
    public void handleSelectedConversation(ConversationSelectedEvent event) {
        ChatFriend chatFriend = event.getFriend();
        LOG.debugf("ConversationSelectedEvent with friend: {0}", chatFriend.getName());
        ConversationPane chatPane = chats.get(chatFriend.getID());
        if (chatPane == null) {
            chatPane = conversationFactory.create(event.getWriter(), chatFriend);
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
        friendsPanel.setVisible(true);
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
    
    private void handleFacebook(){
        friendsPanel.setVisible(false);
        if(facebookPanel == null){
            facebookPanel = createFacebookPanel();
        }
        setConversationPanel(facebookPanel);
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
    
    private JComponent createFacebookPanel(){
        JPanel panel = new JPanel(new MigLayout("gap 10! 10!"));
        panel.setBorder(BorderFactory.createMatteBorder(1,1,0,1, Color.BLACK));
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setCaretPosition(0);
        editorPane.setSelectionColor(HTMLLabel.TRANSPARENT_COLOR);       
        editorPane.setOpaque(false);
        editorPane.setFocusable(false);
        editorPane.setText("<HTML>" + ChatSettings.FACEBOOK_CHAT_DISABLED_TEXT.get() + "</HTML>");
        
    
        StyleSheet mainStyle = ((HTMLDocument)editorPane.getDocument()).getStyleSheet();
        String rules = "h1 { font-family: dialog; color:  #313131; font-size: 12; font-weight: bold}" +
                "p {font-family: dialog; color: #313131; font-size: 11; }" ;
        StyleSheet newStyle = new StyleSheet();
        newStyle.addRule(rules);
        mainStyle.addStyleSheet(newStyle); 
        editorPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == EventType.ACTIVATED) {
                    NativeLaunchUtils.openURL("http://www.facebook.com");
                }
            }
        });        
     
        panel.add(editorPane);
        
        
        return panel;
    }
}
