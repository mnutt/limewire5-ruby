package org.limewire.ui.swing.statusbar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.friends.chat.IconLibrary;
import org.limewire.ui.swing.mainframe.UnseenMessageListener;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.ui.swing.util.VisibilityListener;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class FriendStatusPanel {

    private final JXButton chatButton;
    
    private Component mainComponent;
    
    private final ChatFramePanel friendsPanel;
    
    @Inject FriendStatusPanel(final ChatFramePanel friendsPanel, IconLibrary iconLibrary) {
        GuiUtils.assignResources(this);
        this.friendsPanel = friendsPanel;
        chatButton = new JXButton(new AbstractAction(I18n.tr("Chat")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendsPanel.toggleVisibility();
                mainComponent.invalidate();
                mainComponent.repaint();
            }
        });
        
        chatButton.setName("ChatButton");
        
        ChatButtonPainter chatButtonPainter = new ChatButtonPainter(chatButton, iconLibrary);
        chatButton.setBackgroundPainter(chatButtonPainter);
        
        chatButton.setIcon(iconLibrary.getChatting());
        chatButton.setHorizontalAlignment(AbstractButton.LEFT);
        
        chatButton.setFocusPainted(false);
        chatButton.setOpaque(false);        
        chatButton.setBorder(null);
        chatButton.setContentAreaFilled(false);
        chatButton.setFocusable(false);
        
        chatButton.setBorder(BorderFactory.createEmptyBorder(2, 10, 0, 10));
        chatButton.setPaintBorderInsets(true);
        
        friendsPanel.setUnseenMessageListener(chatButtonPainter);       
        friendsPanel.addVisibilityListener(new VisibilityListener() {
            @Override
            public void visibilityChanged(boolean visible) {
                chatButton.repaint();
            }
        });
        
        mainComponent = chatButton;
    }
    
    Component getComponent() {
        return mainComponent;
    }
    
    @Inject void register(ListenerSupport<XMPPConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTED:
                    mainComponent.setVisible(true);
                    break;
                case DISCONNECTED:
                    mainComponent.setVisible(false);
                    break;
                }
            }
        });
    }
    
    private class ChatButtonPainter extends AbstractPainter<JXButton> implements UnseenMessageListener {

        private final JXButton button;
        private final IconLibrary iconLibrary;
        @Resource private Color rolloverBackground = PainterUtils.TRASPARENT;
        @Resource private Color activeBackground = PainterUtils.TRASPARENT;
        @Resource private Color activeBorder = PainterUtils.TRASPARENT;
        @Resource private Color border = PainterUtils.TRASPARENT;
        private final Set<String> unseenSenderIds = new HashSet<String>();
        
        public ChatButtonPainter(JXButton button, IconLibrary iconLibrary) {
            GuiUtils.assignResources(this);
            this.button = button;
            this.iconLibrary = iconLibrary;
            
            setCacheable(false);
            setAntialiasing(true);
        }
        
        @Override
        protected void doPaint(Graphics2D g, JXButton object, int width, int height) {
            if (friendsPanel.isVisible()) {
                g.setPaint(activeBackground);
                g.fillRect(0, 0, width, height);
                g.setPaint(activeBorder);
                g.drawLine(0, 0, 0, height-1);
                g.drawLine(0, height-1, width-1, height-1);
                g.drawLine(width-1, 0, width-1, height-1);
                
                if (friendsPanel.getLastSelectedConversationFriendId() != null) {
                    g.setPaint(border);
                    g.drawLine(0,0,width-1,0);
                }
            } else if (object.getModel().isRollover()) {
                g.setPaint(rolloverBackground);
                g.fillRect(0, 2, width-1, height-2);
                g.setPaint(activeBorder);
                g.drawLine(0, 1, 0, height-1);
            }
        }

        @Override
        public void clearUnseenMessages() {
            unseenSenderIds.clear();
            updateButtonText();
        }

        @Override
        public void messageReceivedFrom(String senderId, boolean chatIsVisible) {
            unseenSenderIds.add(senderId);
            updateButtonText();
        }

        private void updateButtonText() {
            int unseenMessageSenderCount = unseenSenderIds.size();
            boolean hasUnseenMessages = unseenMessageSenderCount > 0;
            String buttonText = hasUnseenMessages ? I18n.tr("Chat ({0})", unseenMessageSenderCount) : I18n.tr("Chat");
            button.setText(buttonText);
            button.setIcon(hasUnseenMessages ? iconLibrary.getUnviewedMessages() : iconLibrary.getChatting());
            button.repaint();
        }
            
        @Override
        public void conversationSelected(String chatId) {
            unseenSenderIds.remove(chatId);
            updateButtonText();
        }
    }

}
