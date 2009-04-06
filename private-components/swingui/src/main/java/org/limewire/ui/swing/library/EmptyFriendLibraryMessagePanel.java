package org.limewire.ui.swing.library;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.friends.chat.ChatFriendListPane;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;

/**
 * Creates an empty Panel that replaces the friend table and displays a message.
 * This panel and message will only be shown if a friend library is in a state
 * where there are no files to be displayed.
 */
public class EmptyFriendLibraryMessagePanel extends JPanel implements ListEventListener<LocalFileItem> {

    /**
     * Various states the EmptyPanel can exist in. 
     */
    enum MessageTypes {
        OFFLINE, ONLINE, LW_LOADING, LW_NO_FILES, LW_CONNECTION_ERROR, ALL_FRIENDS
    }
    
    @Resource
    private Color backgroundColor;
    
    /** Message panel that will be displayed */
    private Message message;
    
    /** Friend that is currently selected */
    private Friend friend;
    
    /** EventList of files being shared with this friend */
    private EventList<LocalFileItem> friendList;
    
    private final LibraryNavigator libraryNavigator;
    private final ChatFriendListPane friendsPane;
    private final ChatFramePanel friendsPanel;
    private final ShareListManager shareListManager;
    
    @Inject
    public EmptyFriendLibraryMessagePanel(LibraryNavigator libraryNavigator, 
            ChatFriendListPane friendsPane, ChatFramePanel friendsPanel,
            ShareListManager shareListManager) {
        this.libraryNavigator = libraryNavigator;
        this.friendsPane = friendsPane;
        this.friendsPanel = friendsPanel;
        this.shareListManager = shareListManager;
        
        GuiUtils.assignResources(this);
        
        setBackground(backgroundColor);
        
        setLayout(new MigLayout("insets 0, gap 0, fill"));
    }
    
    /**
     * Sets the current friend which will update the name in the message panels.
     * If All Friends is selected, friend will be set to null. 
     * 
     * If friend is not null, the shareList of this friend will be loaded.
     */
    public void setFriend(Friend friend) {
        // remove listeners only if the friend was a non null/anonymouse friend
        if(this.friend != null && !this.friend.isAnonymous() && this.friendList != null)
            this.friendList.removeListEventListener(this);

        this.friend = friend;
        
        if(friend != null && !friend.isAnonymous()) {
            this.friendList = shareListManager.getFriendShareList(friend).getSwingModel();
            if(friendList != null)
                this.friendList.addListEventListener(this);
        } else {
            this.friendList = new BasicEventList<LocalFileItem>();
        }
    }
    
    /**
     * Sets the message to display if no files exist in the library.
     */
    public void setMessageType(MessageTypes type) {
        if(message == null) {
            message = new Message(type);
            createEmptyPanel(message.getComponent());
        } else
            message.setMessageType(type);
    }
    
    private void createEmptyPanel(JComponent component) {
        add(component, "pos 0.50al 0.4al");
    }
    
    /**
     * Floating message in the FriendLibrary. This displays feedback to the user as to
     * what state their friend is in when no files are displayed.
     */
    private class Message {                
        
        private static final String SHARE_ANCHOR = "<a href='#share'>";
        private static final String CHAT_ANCHOR = "<a href='#chat'>";
        private static final String CLOSING_ANCHOR = "</a>";
        
        
        private MessageComponent messageComponent;
        
        private MessageTypes messageType;
        private final JLabel firstLabel;
        private final HTMLLabel shareLabel;
        
        public Message(MessageTypes messageType) {
            this.messageType = messageType;
            
            messageComponent = new MessageComponent();
            
            firstLabel = new JLabel();
            messageComponent.decorateHeaderLabel(firstLabel);

            shareLabel = new HTMLLabel();
            shareLabel.setOpaque(false);
            shareLabel.setBorder(null);
            shareLabel.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == EventType.ACTIVATED) {
                        String target = e.getDescription();
                        if (target.equals("#share")) {
                            libraryNavigator.selectFriendShareList(friend);
                        } else if (target.equals("#chat")) {
                            friendsPanel.setChatPanelVisible(true);
                            friendsPane.fireConversationStarted(friend.getId());
                        } else {
                            throw new IllegalStateException("unknown target: " + target);
                        }
                    }
                }
            });
            messageComponent.decorateSubLabel(shareLabel);
            
            messageComponent.addComponent(firstLabel, "span, gapbottom 0, wrap");
            messageComponent.addComponent(shareLabel, "");
            
            setMessage();
        }
        
        /**
		 * Returns the panel that is displayed.
	     */
        public JComponent getComponent() {
            return messageComponent;
        }
        
        /**
         * Update the message about this friend. 
         */
        public void setMessageType(MessageTypes messageType) {
            this.messageType = messageType;
            setMessage();
        }
        
        private void setMessage() {
            switch(messageType) {
                case OFFLINE: setOfflineMessage(); break;
                case ONLINE: setOnlineMessage(); break;
                case LW_LOADING: setLWLoading(); break;
                case LW_NO_FILES: setLWNoFiles(); break;
                case LW_CONNECTION_ERROR: setLWConnectionError(); break;
                case ALL_FRIENDS: setAllFriends(); break;
            }
        }
        
        /**
         * Friend is offline.
         */
        private void setOfflineMessage() {
            firstLabel.setText(I18n.tr("{0} is offline", friend.getRenderName()));
            // {1}: number of files, {0,2}: html tags
            setShareText(I18n.trn("You're {0}sharing {1} file{2} with your friend.", "You're {0}sharing {1} files{2} with your friend.", friendList.size(), SHARE_ANCHOR, friendList.size(), CLOSING_ANCHOR));
        }
        
        /**
         * Friend is online but not on LimeWire.
         */
        private void setOnlineMessage() {
            firstLabel.setText(I18n.tr("{0} isn't on LimeWire", friend.getRenderName()));
            // {1}: number of files, {0,2}: html tags
            setShareText(I18n.trn("You're {0}sharing {1} file{2} with your friend.", "You're {0}sharing {1} files{2} with your friend.", friendList.size(), SHARE_ANCHOR, friendList.size(), CLOSING_ANCHOR) + " "
                    // {0}, {1}: html tags
                    + I18n.tr("{0}Chat{1} about signing into LimeWire 5.", CHAT_ANCHOR, CLOSING_ANCHOR));
        }
        
        /**
         * Friend signed onto LimeWire and currently retrieving their Library.
         */
        private void setLWLoading() {
            hideShareText();
            if(friend != null && !friend.isAnonymous()) {
                firstLabel.setText(I18n.tr("Loading {0}'s files...", friend.getRenderName()));
            } else {
                firstLabel.setText(I18n.tr("Loading files..."));
            }
        }
        
        /**
         * Friend signed onto LimeWire, browse completed and not sharing any files with you.
         */
        private void setLWNoFiles() {
            if(friend != null && !friend.isAnonymous()) {
                firstLabel.setText(I18n.tr("{0} isn't sharing with you", friend.getRenderName()));
                // {1}: number of files, {0,2}: html tags
                setShareText(I18n.trn("You're {0}sharing {1} file{2} with your friend.", "You're {0}sharing {1} files{2} with your friend.", friendList.size(), SHARE_ANCHOR, friendList.size(), CLOSING_ANCHOR) + " "
                        // {0}, {1}: html tags
                        + I18n.tr("{0}Chat{1} about LimeWire 5.", CHAT_ANCHOR, CLOSING_ANCHOR));
            } else {
                //this should never happen
                firstLabel.setText(I18n.tr("This person isn't sharing any files with you."));
                hideShareText();
            }
        }
        
        /**
         * Friend signed onto LimeWire, browse failed.
         */
        private void setLWConnectionError() {
            hideShareText();
            if(!friend.isAnonymous()) {
                firstLabel.setText(I18n.tr("There was a problem viewing {0}'s files.", friend.getRenderName()));
            } else {
                firstLabel.setText(I18n.tr("There was a problem viewing this person's files."));
            }
        }
        
        /**
         * All Friends view when no files being shared with user.
         */
        private void setAllFriends() {
            hideShareText();
            firstLabel.setText(I18n.tr("There are no files to browse."));
        }
        
        /**
         * Sets the text of the share link and share label. If the share text is null,
         * both the link and the label are hidden.
         */
        private void setShareText(String shareText) {
            shareLabel.setVisible(true);
            shareLabel.setText("<body>" + shareText + "</body>");
        }
        
        private void hideShareText() {
            shareLabel.setVisible(false);
        }
    }
    
    /**
     * Update the message if the number of shared files with this friend
     * has changed.
     */
    @Override
    public void listChanged(ListEvent<LocalFileItem> listChanges) {
        message.setMessage();
    }
}
