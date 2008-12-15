package org.limewire.ui.swing.friends.chat;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.xmpp.api.client.Presence.Mode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The top border of the chat panel, for minimizing the chat window
 * & other controls.
 */
@Singleton
public class ChatTopPanel extends JPanel {
    private JLabel friendAvailabiltyIcon;
    private JLabel friendNameLabel;
    private JLabel friendStatusLabel;
    
    private Action minimizeAction;
    
    @Inject
    public ChatTopPanel(IconLibrary icons) {        
        setBackground(Color.BLACK);
        setForeground(Color.WHITE);
        setLayout(new MigLayout("insets 0, fill", "3[]3[][]3:push[]3", "0[]0"));
        
        friendAvailabiltyIcon = new JLabel();
        add(friendAvailabiltyIcon);
        friendNameLabel = new JLabel();
        friendNameLabel.setForeground(getForeground());
        add(friendNameLabel, "wmin 0, shrinkprio 50");
        
        friendStatusLabel = new JLabel();
        friendStatusLabel.setForeground(getForeground());
        FontUtils.changeSize(friendStatusLabel, -1.8f);
        add(friendStatusLabel, "wmin 0, shrinkprio 0");
        
        RectanglePainter<JXButton> backgroundPainter = new RectanglePainter<JXButton>(getBackground(), getBackground());
        final JXButton minimizeChat = new JXButton(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                minimizeAction.actionPerformed(e);
            }
        });        
        minimizeChat.setIcon(icons.getMinimizeNormal());
        minimizeChat.setRolloverIcon(icons.getMinimizeOver());
        minimizeChat.setPressedIcon(icons.getMinimizeDown());
        minimizeChat.setBorderPainted(false);
        minimizeChat.setBackgroundPainter(backgroundPainter);
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
        
        ToolTipManager.sharedInstance().registerComponent(this);
        
        EventAnnotationProcessor.subscribe(this);
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
        if (event.isLocallyInitiated()) {
            ChatFriend chatFriend = event.getFriend();
            friendAvailabiltyIcon.setText(getAvailabilityHTML(chatFriend.getMode()));
            friendNameLabel.setText(chatFriend.getName());
            String status = chatFriend.getStatus();
            friendStatusLabel.setText(status != null && status.length() > 0 ? " - " + status : "");
        }
    }
    
    @EventSubscriber
    public void handleConversationEnded(CloseChatEvent event) {
        clearFriendInfo();
    }
    
    private void clearFriendInfo() {
        friendAvailabiltyIcon.setText("");
        friendNameLabel.setText("");
        friendStatusLabel.setText("");
    }
}
