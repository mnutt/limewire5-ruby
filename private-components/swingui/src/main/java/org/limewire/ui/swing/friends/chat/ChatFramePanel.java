package org.limewire.ui.swing.friends.chat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXPanel;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.settings.UISettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.PanelDisplayedEvent;
import org.limewire.ui.swing.event.RuntimeTopicPatternEventSubscriber;
import org.limewire.ui.swing.mainframe.UnseenMessageListener;
import org.limewire.ui.swing.sound.WavSoundPlayer;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.GuiUtils;
import static org.limewire.ui.swing.util.I18n.tr;
import org.limewire.ui.swing.util.VisibilityListener;
import org.limewire.ui.swing.util.VisibilityListenerList;
import org.limewire.ui.swing.util.VisibleComponent;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The main frame of the chat panel.
 * 
 * All visible aspects of chat are rendered in this panel.
 */
@Singleton
public class ChatFramePanel extends JXPanel implements Resizable, VisibleComponent {
    private static final String ALL_CHAT_MESSAGES_TOPIC_PATTERN = MessageReceivedEvent.buildTopic(".*");
    private static final Log LOG = LogFactory.getLog(ChatFramePanel.class);
    private static final String MESSAGE_SOUND_PATH = "/org/limewire/ui/swing/mainframe/resources/sounds/friends/message.wav";
    private final ChatPanel chatPanel;
    private final TrayNotifier notifier;
    //Heavy-weight component so that it can appear above other heavy-weight components
    private final java.awt.Panel mainPanel;
    
    private final VisibilityListenerList visibilityListenerList = new VisibilityListenerList();
    
    private UnseenMessageListener unseenMessageListener;
    
    @Inject
    public ChatFramePanel(ChatPanel chatPanel, TrayNotifier notifier) {
        super(new BorderLayout());
        this.chatPanel = chatPanel;
        this.notifier = notifier;
        this.mainPanel = new java.awt.Panel();
        
        mainPanel.setVisible(false);
        mainPanel.setBackground(getBackground());

        Border lineBorder = BorderFactory.createLineBorder(Color.BLACK);
        chatPanel.setBorder(lineBorder);
        add(mainPanel);
        setVisible(false);
        
        chatPanel.setMinimizeAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setChatPanelVisible(false);
            }
        });
          
        EventAnnotationProcessor.subscribe(this);
    }
    
    @Inject void register(ListenerSupport<XMPPConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTED:
                    handleConnectionEstablished(event);
                    break;
                case DISCONNECTED:
                    handleLogoffEvent();
                    break;
                }
            }
        });
    }
    
    public void setUnseenMessageListener(UnseenMessageListener unseenMessageListener) {
        this.unseenMessageListener = unseenMessageListener;
    }
    
    @Override
    public void toggleVisibility() {
        boolean shouldDisplay = !isVisible();
        setVisibility(shouldDisplay);
    }
    
    public void setChatPanelVisible(boolean shouldDisplay) {
        if(shouldDisplay) {
           resetBounds();
        }

        mainPanel.setVisible(shouldDisplay);
        setVisible(shouldDisplay);
        if (shouldDisplay) {
            unseenMessageListener.clearUnseenMessages();
            ((Displayable)mainPanel.getComponent(0)).handleDisplay();
            new PanelDisplayedEvent(this).publish();
        }
        visibilityListenerList.visibilityChanged(shouldDisplay);
    }
    
    /**
     * Hides FriendsPanel when another panel is shown in the same layer.
     */
    @EventSubscriber
    public void handleOtherPanelDisplayed(PanelDisplayedEvent event){
        if(event.getDisplayedPanel() != this){
            setVisible(false);
            mainPanel.setVisible(false);
        }
    }
    
    @RuntimeTopicPatternEventSubscriber(methodName="getMessagingTopicPatternName")
    public void handleMessageReceived(String topic, MessageReceivedEvent event) {
        unseenMessageListener.messageReceivedFrom(event.getMessage().getFriendID(), isVisible());
        
        if (event.getMessage().getType() != Message.Type.Sent && !GuiUtils.getMainFrame().isActive()) {
            LOG.debug("Sending a message to the tray notifier");
            notifier.showMessage(new Notification(getNoticeForMessage(event)));
            
            URL soundURL = ChatFramePanel.class.getResource(MESSAGE_SOUND_PATH);
            if (soundURL != null && UISettings.PLAY_NOTIFICATION_SOUND.getValue()) {
                ThreadExecutor.startThread(new WavSoundPlayer(soundURL.getFile()), "newmessage-sound");
            }
        } 
    }
    
    @EventSubscriber
    public void handleConversationSelected(ConversationSelectedEvent event) {
        if (event.isLocallyInitiated()) {
            unseenMessageListener.conversationSelected(event.getFriend().getID());
        }
    }

    private String getNoticeForMessage(MessageReceivedEvent event) {
        Message message = event.getMessage();
        return tr("Chat from {0} - LimeWire 5", message.getSenderName());
    }
    
    private void handleConnectionEstablished(XMPPConnectionEvent event) {
        mainPanel.add(chatPanel);
        chatPanel.setLoggedInID(event.getSource().getConfiguration().getCanonicalizedLocalID());
        resetBounds();
    }
    
    private void handleLogoffEvent() {
        mainPanel.remove(chatPanel);
        resetBounds();
        setChatPanelVisible(false);
    }
    
    public String getMessagingTopicPatternName() {
        return ALL_CHAT_MESSAGES_TOPIC_PATTERN;
    }
    
    private void resetBounds() {
        Rectangle parentBounds = getParent().getBounds();
        Dimension childPreferredSize = mainPanel.getPreferredSize();
        int w = (int) childPreferredSize.getWidth();
        int h = (int) childPreferredSize.getHeight();
        setBounds(parentBounds.width - w, parentBounds.height - h, w, h);
    }

    @Override
    public void resize() {
        if (isVisible()) {
            resetBounds();
        }
    }

    @Override
    public void addVisibilityListener(VisibilityListener listener) {
        visibilityListenerList.addVisibilityListener(listener);
    }

    @Override
    public void removeVisibilityListener(VisibilityListener listener) {
        visibilityListenerList.removeVisibilityListener(listener);
    }

    @Override
    public void setVisibility(boolean visible) {
        setChatPanelVisible(visible);
        visibilityListenerList.visibilityChanged(visible);
    }
}
