package org.limewire.ui.swing.friends.chat;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.impl.library.MockLibraryManager;
import org.limewire.listener.EventListenerList;
import org.limewire.ui.swing.friends.chat.ConversationPane;
import org.limewire.ui.swing.friends.chat.MessageReceivedEvent;
import org.limewire.ui.swing.friends.chat.MessageTextImpl;
import org.limewire.ui.swing.friends.chat.Message.Type;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.IconManagerStub;
import org.limewire.xmpp.api.client.ChatState;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.Presence.Mode;

/**
 *
 */
public class ConversationPaneHarness {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final JFrame frame = new JFrame();

                final MockChatFriend friend = new MockChatFriend("Will Benedict", "Just listening to some jams", Mode.available);
                final MessageWriter writer = new MessageWriter() {
                    @Override
                    public void writeMessage(String message) throws XMPPException {
                        new MessageReceivedEvent(new MessageTextImpl("me", friend, Type.Sent, message)).publish();
                    }

                    public void setChatState(ChatState chatState) {
                        // TODO
                    }
                };
                friend.writer = writer;
                final IconManager iconManager = new IconManagerStub();
                try {
                    //Sleep to give the IconManagerStub a chance to load the local mime-type icons
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ConversationPane pane = new ConversationPane(writer, friend, "me", new MockLibraryManager(), iconManager, null, null, null, new EventListenerList<FriendEvent>(), null, new EventListenerList<FeatureEvent>(),
                                new IconLibraryImpl());
                        frame.add(pane);
                        
                        for(int i = 0; i < 10; i++) {
                            new MessageReceivedEvent(
                                    new MessageTextImpl(friend.getName(), friend, Type.Received,
                                            "This is a message This is a message This is a message This is a message This is a message This is a message "
                                            )).publish();
                        }
                        
                        frame.setPreferredSize(new Dimension(470, 400));
                        
                        frame.pack();
                        frame.setVisible(true);
                    }
                });
            }
        });
    }
}
