package org.limewire.ui.swing.friends.chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.impl.library.MockLibraryManager;
import org.limewire.listener.EventListenerList;
import org.limewire.ui.swing.friends.chat.ChatFriend;
import org.limewire.ui.swing.friends.chat.ChatPanel;
import org.limewire.ui.swing.friends.chat.ConversationPane;
import org.limewire.ui.swing.friends.chat.ConversationPaneFactory;
import org.limewire.ui.swing.friends.chat.ChatFriendListPane;
import org.limewire.ui.swing.friends.chat.IconLibraryImpl;
import org.limewire.ui.swing.friends.chat.MessageReceivedEvent;
import org.limewire.ui.swing.friends.chat.ChatTopPanel;
import org.limewire.ui.swing.friends.chat.Message.Type;
import org.limewire.ui.swing.util.IconManagerStub;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.Presence.Mode;

public class ChatPanelHarness {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame();
                final IconLibraryImpl icons = new IconLibraryImpl();
                final MockLibraryManager libraryManager = new MockLibraryManager();
                EventListenerList<FriendPresenceEvent> presenceSupport = new EventListenerList<FriendPresenceEvent>();
                final EventListenerList<FriendEvent> friendSupport = new EventListenerList<FriendEvent>();
                final EventListenerList<FeatureEvent> featureSupport = new EventListenerList<FeatureEvent>();
                ChatFriendListPane friendsPane = new ChatFriendListPane(icons, libraryManager, null, presenceSupport);
                frame.add(new ChatPanel(new ConversationPaneFactory() {
                    @Override
                    public ConversationPane create(MessageWriter writer, ChatFriend chatFriend, String loggedInID) {
                        return new ConversationPane(writer, chatFriend, loggedInID, libraryManager, new IconManagerStub(), null, null, null, friendSupport, null, featureSupport);
                    }
                }, icons, friendsPane, new ChatTopPanel(icons)));
                
                frame.pack();
                frame.setVisible(true);
                
                JFrame frame2 = new JFrame();
                frame2.add(addFriendPanel(presenceSupport, friendSupport));
                frame2.pack();
                frame2.setVisible(true);
            }
        });
    }
    
    private static JPanel addFriendPanel(final EventListenerList<FriendPresenceEvent> presenceSupport, 
                                         final EventListenerList<FriendEvent> friendSupport) {
        JPanel panel = new JPanel(new MigLayout("", "[][]", ""));
        
        panel.add(new JLabel("Id:"));
        final JTextField idField = new JTextField(20);
        panel.add(idField, "wrap");
        panel.add(new JLabel("Name:"));
        final JTextField nameField = new JTextField(20);
        panel.add(nameField, "wrap");
        panel.add(new JLabel("Mood:"));
        final JTextField moodField = new JTextField(20);
        panel.add(moodField, "wrap");
        JButton addFriend = new JButton("Add Friend");
        addFriend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                User user = new MockUser(idField.getText(), nameField.getText());
                friendSupport.broadcast(new FriendEvent(user, FriendEvent.Type.ADDED));
                presenceSupport.broadcast(new FriendPresenceEvent(
                        new MockPresence(user, Mode.available, moodField.getText(), idField.getText()), FriendPresenceEvent.Type.ADDED));
            }
        });
        panel.add(addFriend, "span, wrap");
        JButton fillWithMessageBuddies = new JButton("Fill with Message Awaiting Buddies");
        fillWithMessageBuddies.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fillWithUnseenMessageAwaitingFriends(presenceSupport, friendSupport);
            }
        });
        panel.add(fillWithMessageBuddies, "span");
        return panel;
    }
    
    private static void fillWithUnseenMessageAwaitingFriends(
            final EventListenerList<FriendPresenceEvent> presenceSupport, 
            final EventListenerList<FriendEvent> friendSupport) {
        for(int i = 0; i < 50; i++) {
            String id = "foo" + i;
            User user = new MockUser(id, "foo");
            friendSupport.broadcast(new FriendEvent(user, FriendEvent.Type.ADDED));
            presenceSupport.broadcast(new FriendPresenceEvent(
                    new MockPresence(user, Mode.available,"hey", id), FriendPresenceEvent.Type.ADDED));
        }
        
        for(int i = 0; i < 50; i++) {
            String id = "foo" + i;
            new MessageReceivedEvent(new MockMessage(new MockChatFriend(id, "hey", Mode.available), "yo", System.currentTimeMillis(), "me", Type.Received)).publish();
        }
    }
}
