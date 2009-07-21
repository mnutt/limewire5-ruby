package org.limewire.ui.swing.friends.chat;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Application;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.friend.api.FileOfferEvent;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendPresenceEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.RuntimeTopicPatternEventSubscriber;
import org.limewire.ui.swing.mainframe.UnseenMessageListener;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.sound.WavSoundPlayer;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.EnabledType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.VisibilityType;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The main frame of the chat panel. This frame must be heavy weight to
 * be seen atop the store and startup page.
 * 
 * This frame is a mediator between the ChatPanel and the rest of the
 * application. The ChatPanel is lazily created when the user first
 * displays the ChatFramePanel.
 */
@Singleton
public class ChatFramePanel extends Panel implements ChatFrame {
    private static final String ALL_CHAT_MESSAGES_TOPIC_PATTERN = MessageReceivedEvent.buildTopic(".*");
    private static final String MESSAGE_SOUND_PATH = "/org/limewire/ui/swing/mainframe/resources/sounds/friends/message.wav";

    private final Provider<ChatPanel> chatPanelProvider;
    private final TrayNotifier notifier;
    
    private final EventListenerList<VisibilityType> visibilityListenerList = new EventListenerList<VisibilityType>();
    private final EventListenerList<EnabledType> enabledListenerList = new EventListenerList<EnabledType>();

    private boolean actionEnabled = false;
    
    private UnseenMessageListener unseenMessageListener;
    private String lastSelectedConversationFriendId;
    private String mostRecentConversationFriendId;

    /** Actual panel that displays the chat/list of friends */
    private ChatPanel chatPanel;
    
    @Inject
    public ChatFramePanel(Provider<ChatPanel> chatPanelProvider, TrayNotifier notifier) {

        this.chatPanelProvider = chatPanelProvider;
        this.notifier = notifier;

        setLayout(new BorderLayout());
        setVisible(false);
          
        EventAnnotationProcessor.subscribe(this);
    }
    
    @Inject void register(ListenerSupport<FriendConnectionEvent> connectionSupport,
            ListenerSupport<FriendPresenceEvent> presenceSupport,
            ListenerSupport<FileOfferEvent> fileOfferEventListenerSupport) {
        connectionSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTED:
                    handleLogin(event);
                    break;
                case DISCONNECTED:
                    handleLogoff();
                    break;
                }
            }
        });
    }
    
    public void setUnseenMessageListener(UnseenMessageListener unseenMessageListener) {
        this.unseenMessageListener = unseenMessageListener;
    }
    
    @Override
    public void fireConversationStarted(String friendId) {
        if(!isVisible())
            setVisibility(true);
        chatPanel.fireConversationStarted(friendId);
    }
    
    @RuntimeTopicPatternEventSubscriber(methodName="getMessagingTopicPatternName")
    public void handleMessageReceived(String topic, MessageReceivedEvent event) {
        if (event.getMessage().getType() != Message.Type.SENT) {
            String messageFriendID = event.getMessage().getFriendID();
            mostRecentConversationFriendId = messageFriendID;
            notifyUnseenMessageListener(event);
            if(isVisible()) {
                chatPanel.markActiveConversationRead();
            }
        }
        if (event.getMessage().getType() != Message.Type.SENT &&
             (!GuiUtils.getMainFrame().isActive() || !isVisible())) {
            notifier.showMessage(getNoticeForMessage(event));
            
            URL soundURL = ChatFramePanel.class.getResource(MESSAGE_SOUND_PATH);
            if (soundURL != null && SwingUiSettings.PLAY_NOTIFICATION_SOUND.getValue()) {
                ThreadExecutor.startThread(new WavSoundPlayer(soundURL.getFile()), "newmessage-sound");
            }
        } 
    }
    
    private void notifyUnseenMessageListener(MessageReceivedEvent event) {
        String messageFriendID = event.getMessage().getFriendID();
        if (!messageFriendID.equals(lastSelectedConversationFriendId) || !isVisible()) {
            unseenMessageListener.messageReceivedFrom(messageFriendID, isVisible());
        }
    }
    
    @EventSubscriber
    public void handleConversationSelected(ConversationSelectedEvent event) {
        if (event.isLocallyInitiated()) {
            lastSelectedConversationFriendId = event.getFriend().getID();
            unseenMessageListener.conversationSelected(lastSelectedConversationFriendId);
        }
        
        visibilityListenerList.broadcast(VisibilityType.VISIBLE);
    }
    
    @EventSubscriber
    public void handleChatClosed(CloseChatEvent event) {
        lastSelectedConversationFriendId = null;
        
        if (event.getFriend().getID().equals(mostRecentConversationFriendId)) {
            mostRecentConversationFriendId = null;
        }
        
        visibilityListenerList.broadcast(VisibilityType.VISIBLE);
    }

    private Notification getNoticeForMessage(MessageReceivedEvent event) {
        final Message message = event.getMessage();

        // todo: each message type should know how to display itself as a notification
        String title = message.getType() == Message.Type.SERVER ?
                tr("Message from the chat server") :
                tr("Chat from {0}", message.getSenderName());
        String messageString = message.toString();
        Notification notification = new Notification(title, messageString, new AbstractAction(I18n.tr("Reply")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActionMap map = Application.getInstance().getContext().getActionManager().getActionMap();
                map.get("restoreView").actionPerformed(e);
                fireConversationStarted(message.getFriendID());
            }
        });
        return notification;
    }

	/**
	 * Updates the chat panel upon logging on.
	 */    
    private void handleLogin(FriendConnectionEvent event) {
        setActionEnabled(true);
    }
    
    /**
	 * Clears saved state when logging off.
	 */
    private void handleLogoff() {
        setVisibility(false);
        setActionEnabled(false);
        lastSelectedConversationFriendId = null;
        mostRecentConversationFriendId = null;
        unseenMessageListener.clearUnseenMessages();
    }
    
    public String getMessagingTopicPatternName() {
        return ALL_CHAT_MESSAGES_TOPIC_PATTERN;
    }

    @Override
    public void resize() {
        Rectangle parentBounds = getParent().getBounds();
        Dimension childPreferredSize = getPreferredSize();
        int w = (int) childPreferredSize.getWidth();
        int h = (int) childPreferredSize.getHeight();
        setBounds(parentBounds.width - w, parentBounds.height - h, w, h);
    }

    @Override
    public void addVisibilityListener(EventListener<VisibilityType> listener) {
        visibilityListenerList.addListener(listener);
    }

    @Override
    public void removeVisibilityListener(EventListener<VisibilityType> listener) {
        visibilityListenerList.removeListener(listener);
    }
    
    @Override
    public void toggleVisibility() {
        setVisibility(!isVisible());
    }

    @Override
    public void setVisibility(boolean visible) {
        if(chatPanel != null) {
            setVisible(visible);
        } else if(visible){
            createChatPanel();
            validate();
            setVisible(true);
        }
        if(visible) {
            resize();
            //make the most recent conversation the active one when opening the chat window
            if(mostRecentConversationFriendId != null) {
                chatPanel.fireConversationStarted(mostRecentConversationFriendId);
            }
            chatPanel.markActiveConversationRead();
        }
        visibilityListenerList.broadcast(VisibilityType.valueOf(visible));
    }
        
    public void createChatPanel() {
        if(chatPanel == null) {
            // create the chat panel if its the first time being visible
            chatPanel = chatPanelProvider.get();
            add(chatPanel, BorderLayout.CENTER);
        }
    }

    @Override
    public void addEnabledListener(EventListener<EnabledType> listener) {
        enabledListenerList.addListener(listener);
    }

    @Override
    public void removeEnabledListener(EventListener<EnabledType> listener) {
        enabledListenerList.removeListener(listener);
    }

    /**
     * Returns true if the component is enabled for use. 
     */
    @Override
    public boolean isActionEnabled() {
        return actionEnabled;
    }

    /**
     * Sets an indicator to determine whether the component is enabled for use,
     * and notifies all registered EnabledListener instances. 
     */
    private void setActionEnabled(boolean enabled) {
        // Get old value, and save new value.
        boolean oldValue = actionEnabled;
        actionEnabled = enabled;
        
        // Notify listeners if value changed.
        if (enabled != oldValue) {
            enabledListenerList.broadcast(EnabledType.valueOf(enabled));
        }
    }
    
    /**
     * Returns the last selected friend id. Or null if none is selected. 
     */
    public String getLastSelectedConversationFriendId() {
        return lastSelectedConversationFriendId;
    }
}
