package org.limewire.ui.swing.friends.chat;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.ToolTipManager;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.xmpp.api.client.Presence.Mode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The top border of the chat panel, for minimizing the chat window
 * & other controls.
 */
@Singleton
public class ChatTopPanel extends JXPanel {
    @Resource(key="ChatTopPanel.backgroundGradientTop") private Color gradientTop; 
    @Resource(key="ChatTopPanel.backgroundGradientBottom") private Color gradientBottom; 
    @Resource(key="ChatTopPanel.borderBottom") private Color borderBottom;
    @Resource(key="ChatTopPanel.buddyTextFont") private Font textFont;
    @Resource(key="ChatTopPanel.hideTextFont") private Font hideFont;
    @Resource(key="ChatTopPanel.textColor") private Color textColor;
    private JLabel friendAvailabiltyIcon;
    private JLabel friendNameLabel;
    private JLabel friendStatusLabel;
    
    private Action minimizeAction;
    
    @Inject
    public ChatTopPanel() {        
        GuiUtils.assignResources(this);
        
        RectanglePainter painter = new RectanglePainter();
        painter.setFillPaint(new GradientPaint(50.0f, -1.0f, gradientTop, 50.0f, 20.0f, gradientBottom));
        painter.setBorderPaint(borderBottom);
        painter.setBorderWidth(1f);
        
        setBackgroundPainter(painter);
        
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
        
        JXHyperlink minimizeChat = new JXHyperlink(new AbstractAction(tr("<html><u>Hide Chat</u></html>")) {
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
