package org.limewire.ui.swing.friends.chat;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.ToolTipManager;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendPresence;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.PopupCloseButton;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.util.Objects;

import com.google.inject.Inject;

/**
 * The top border of the chat panel, for minimizing the chat window
 * & other controls.
 */
public class ChatTopPanel extends JXPanel {
    @Resource private Font textFont;
    @Resource private Color textColor;
    @Resource private Color background;

    private JLabel friendAvailabiltyIcon;
    private JLabel friendNameLabel;
    private JLabel friendStatusLabel;
    
    private final Map<String, PropertyChangeListener> friendStatusAndModeListeners;
    
    @Inject
    public ChatTopPanel(final ChatFramePanel chatFramePanel) {
        
        GuiUtils.assignResources(this);
        
        setBackground(background);
        
        setLayout(new MigLayout("insets 0, gap 0, fill"));
        
        ResizeUtils.forceHeight(this, 21);
        
        friendAvailabiltyIcon = new JLabel();
        add(friendAvailabiltyIcon, "gapleft 4, gapright 2, dock west");
        friendNameLabel = new JLabel();
        friendNameLabel.setForeground(textColor);
        friendNameLabel.setFont(textFont);
        add(friendNameLabel, "gapright 2, dock west");
        
        friendStatusLabel = new JLabel();
        friendStatusLabel.setForeground(textColor);
        friendStatusLabel.setFont(textFont);
        add(friendStatusLabel, "dock west");
       
        friendStatusAndModeListeners = new HashMap<String, PropertyChangeListener>();
        ToolTipManager.sharedInstance().registerComponent(this);
        
        IconButton closeButton = new PopupCloseButton();
        closeButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chatFramePanel.setVisibility(false);
            }
        });
        add(closeButton, "gapright 3, dock east");
        
        EventAnnotationProcessor.subscribe(this);
    }

    @Inject
    void register(ListenerSupport<FriendConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                if (event.getType() == FriendConnectionEvent.Type.DISCONNECTED) {
                    // when signed off, erase info about who LW was chatting with
                    clearFriendInfo();
                }
            }
        });
    }
    
    private String getAvailabilityHTML(FriendPresence.Mode mode) {
        return "<html><img src=\"" + ChatFriendsUtil.getIconURL(mode) + "\" /></html>";
    }
    
    @Override
    public String getToolTipText() {
        String name = friendNameLabel.getText();
        String label = friendStatusLabel.getText();
        String tooltip = name + label;
        return tooltip.length() == 0 ? null : friendAvailabiltyIcon.getText().replace("</html>", "&nbsp;" + tooltip + "</html>");
    }
    
    @EventSubscriber
    public void handleConversationStarted(ConversationSelectedEvent event) {
        ChatFriend chatFriend = event.getFriend();
        if (event.isLocallyInitiated()) {
            update(chatFriend);
        }
        addChatFriendStatusListener(chatFriend);
    }

    private void addChatFriendStatusListener(final ChatFriend chatFriend) {
        PropertyChangeListener statusAndModeListener = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if (("status".equals(propertyName) || "mode".equals(propertyName)) &&
                        (!Objects.equalOrNull(evt.getNewValue(), evt.getOldValue())) ) {
                    update(chatFriend);
                }
            }
        };
        chatFriend.addPropertyChangeListener(statusAndModeListener);
        friendStatusAndModeListeners.put(chatFriend.getID(), statusAndModeListener);
    }

    private void removeChatFriendStatusListener(ChatFriend finishedFriend) {
        PropertyChangeListener statusAndModeListener = friendStatusAndModeListeners.remove(finishedFriend.getID());
        finishedFriend.removePropertyChangeListener(statusAndModeListener);
    }

    private void update(ChatFriend chatFriend) {
        friendAvailabiltyIcon.setText(getAvailabilityHTML(chatFriend.getMode()));
        friendNameLabel.setText(chatFriend.getName());
        String status = chatFriend.getStatus();
        friendStatusLabel.setText(status != null && status.length() > 0 ? " - " + status : "");
    }
    
    @EventSubscriber
    public void handleConversationEnded(CloseChatEvent event) {
        clearFriendInfo();
        removeChatFriendStatusListener(event.getFriend());
    }
    
    private void clearFriendInfo() {
        friendAvailabiltyIcon.setText("");
        friendNameLabel.setText("");
        friendStatusLabel.setText("");
    }
}
