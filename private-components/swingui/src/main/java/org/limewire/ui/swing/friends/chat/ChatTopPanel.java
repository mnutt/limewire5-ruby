package org.limewire.ui.swing.friends.chat;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.ToolTipManager;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXPanel;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.Objects;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPPresence.Mode;

import com.google.inject.Inject;

/**
 * The top border of the chat panel, for minimizing the chat window
 * & other controls.
 */
public class ChatTopPanel extends JXPanel {
    @Resource(key="ChatTopPanel.buddyTextFont") private Font textFont;
    @Resource(key="ChatTopPanel.hideTextFont") private Font hideFont;
    @Resource(key="ChatTopPanel.textColor") private Color textColor;
    private JLabel friendAvailabiltyIcon;
    private JLabel friendNameLabel;
    private JLabel friendStatusLabel;
    
    private Action minimizeAction;
    private final Map<String, PropertyChangeListener> friendStatusAndModeListeners;
    
    @Inject
    public ChatTopPanel(BarPainterFactory painterFactory) {
        GuiUtils.assignResources(this);
        
        setBackgroundPainter(painterFactory.createHeaderBarPainter());
        
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 2, Color.BLACK));
        
        setLayout(new MigLayout("insets 3 2 0 5, fill", "[]2[][]:push[]5", "[19px, top]"));
        Dimension size = new Dimension(400, 19);
        setMinimumSize(size);
        setMaximumSize(size);
        setPreferredSize(size);
        
        friendAvailabiltyIcon = new JLabel();
        add(friendAvailabiltyIcon, "wmax 12, hmax 12");
        friendNameLabel = new JLabel();
        friendNameLabel.setForeground(textColor);
        friendNameLabel.setFont(textFont);
        add(friendNameLabel, "wmin 0, shrinkprio 50");
        
        friendStatusLabel = new JLabel();
        friendStatusLabel.setForeground(textColor);
        friendStatusLabel.setFont(textFont);
        add(friendStatusLabel, "wmin 0, shrinkprio 0");
        
        JXHyperlink minimizeChat = new JXHyperlink(new AbstractAction("<html><u>" + tr("Hide") + "</u></html>") {
            @Override
            public void actionPerformed(ActionEvent e) {
                minimizeAction.actionPerformed(e);
            }
        });  
        minimizeChat.setFont(hideFont);
        minimizeChat.setForeground(textColor);
        add(minimizeChat, "alignx right");
        
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (minimizeAction!= null) {
                    minimizeAction.actionPerformed(null);
                }
            }
        });
        friendStatusAndModeListeners = new HashMap<String, PropertyChangeListener>();
        ToolTipManager.sharedInstance().registerComponent(this);
        
        EventAnnotationProcessor.subscribe(this);
    }

    @Inject
    void register(ListenerSupport<XMPPConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                if (event.getType() == XMPPConnectionEvent.Type.DISCONNECTED) {
                    // when signed off, erase info about who LW was chatting with
                    clearFriendInfo();
                }
            }
        });
    }
    
    void setMinimizeAction(Action minimizeAction) {
        this.minimizeAction = minimizeAction;
    }
    
    private String getAvailabilityHTML(Mode mode) {
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
