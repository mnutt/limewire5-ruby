package org.limewire.ui.swing.statusbar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.friends.login.FriendActions;
import org.limewire.ui.swing.mainframe.UnseenMessageListener;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class FriendStatusPanel {

    private final JXButton friendsButton;
    
    private Component mainComponent;

    @Inject FriendStatusPanel(final FriendActions friendActions, final ChatFramePanel friendsPanel) {
        Color whiteBackground = Color.WHITE;
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createLineBorder(new Color(159, 159, 159)));
        chatPanel.setOpaque(true);
        chatPanel.setMinimumSize(new Dimension(0, 20));
        chatPanel.setMaximumSize(new Dimension(150, 20));
        chatPanel.setBackground(whiteBackground);
        
        friendsButton = new JXButton(new AbstractAction(I18n.tr("Chat")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!friendActions.isSignedIn()) {
                    friendActions.signIn();
                } else {
                    friendsPanel.toggleVisibility();
                }
            }
        });
        friendsButton.setBackgroundPainter(new RectanglePainter<JXButton>(whiteBackground, whiteBackground));
        chatPanel.add(friendsButton, BorderLayout.EAST);
        chatPanel.setVisible(false);
        
        friendsPanel.setUnseenMessageListener(new UnseenMessageFlasher(friendsButton));       
        
        mainComponent = chatPanel;
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
    
    private static class UnseenMessageFlasher implements UnseenMessageListener {
        private static Painter<JXButton> BLACK_BACKGROUND_PAINTER = new RectanglePainter<JXButton>(Color.BLACK, Color.BLACK);
        private boolean hasFlashed;
        private final JXButton flashingButton;
        private final Color originalForeground;
        private final Painter<JXButton> originalBackgroundPainter;
        private final Set<String> unseenSenderIds = new HashSet<String>();
        private final String originalButtonText;
        
        public UnseenMessageFlasher(JXButton flashingButton) {
            this.flashingButton = flashingButton;
            this.originalForeground = flashingButton.getForeground();
            this.originalBackgroundPainter = flashingButton.getBackgroundPainter();
            this.originalButtonText = flashingButton.getText();
        }
        
        @Override
        public void clearUnseenMessages() {
            reset();
        }

        @Override
        public void messageReceivedFrom(String senderId, boolean chatIsVisible) {
            unseenSenderIds.add(senderId);
            if (!chatIsVisible) {
                flash();
            }
            
            updateButtonText();
        }

        private void updateButtonText() {
            int unseenMessageSenderCount = unseenSenderIds.size();
            flashingButton.setText(originalButtonText + (unseenMessageSenderCount > 0 ? " (" + unseenMessageSenderCount + ")" : ""));
        }
            
        @Override
        public void conversationSelected(String chatId) {
            unseenSenderIds.remove(chatId);
            updateButtonText();
        }

        public void flash() {
            if (!hasFlashed) {
                new Timer(1500, new ActionListener() {
                    private int flashCount;
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (flashCount++ < 5) {
                            flashingButton.setForeground(toggle(flashingButton.getForeground()));
                            flashingButton.setBackgroundPainter(toggle(flashingButton.getBackgroundPainter()));
                        } else {
                            Timer timer = (Timer)e.getSource();
                            timer.stop();
                        }
                    }
                    
                    private Color toggle(Color color) {
                        return color.equals(Color.WHITE) ? Color.BLACK : Color.WHITE;
                    }
                    
                    private Painter<JXButton> toggle(Painter<JXButton> painter) {
                        return painter.equals(originalBackgroundPainter) ? BLACK_BACKGROUND_PAINTER : originalBackgroundPainter;
                    }
                }).start();
                hasFlashed = true;
            }
        }
        
        public void reset() {
            flashingButton.setForeground(originalForeground);
            flashingButton.setBackgroundPainter(originalBackgroundPainter);
            hasFlashed = false;
        }
    }
}
