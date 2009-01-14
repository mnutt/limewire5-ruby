package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.friends.chat.ChatFriendListPane;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

public class FriendLibraryMediator extends LibraryMediator implements EventListener<FriendEvent> {

    private MessageComponent emptyPanelMessage;
    
    private final EmptyLibraryFactory emptyFactory;
    private final FriendLibraryFactory factory;
    private final FriendSharingPanelFactory sharingFactory;
    private final LibraryManager libraryManager;
    private final ShareListManager shareListManager;
    private final ChatFriendListPane friendsPane;
    private final ChatFramePanel friendsPanel;
    
    private final Friend friend;
    private final FriendFileList friendFileList;
    private EventList<RemoteFileItem> eventList;
    
    private ListenerSupport<FriendEvent> availListeners;

    @AssistedInject
    public FriendLibraryMediator(@Assisted Friend friend, FriendLibraryFactory factory, EmptyLibraryFactory emptyFactory,
            FriendSharingPanelFactory sharingFactory, LibraryManager libraryManager, ShareListManager shareListManager,
            @Named("available") ListenerSupport<FriendEvent> availListeners, 
            ChatFriendListPane friendsPane, ChatFramePanel friendsPanel) {
        this.factory = factory;
        this.friend = friend;        
        this.sharingFactory = sharingFactory;
        this.emptyFactory = emptyFactory;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.friendsPane = friendsPane;
        this.friendsPanel = friendsPanel;
        this.friendFileList = shareListManager.getOrCreateFriendShareList(friend);
        this.availListeners = availListeners;
        this.availListeners.addListener(this);
        
        createEmptyCard();
        showEmptyCard();
    }
    
    /**
	 * Constructs the empty panel. This panel is constructed once per friend and the
	 * notification message gets updated as necesarry.
	 */
    private void createEmptyCard() {
    	// if their already online during signon, set appropriate message, otherwise their offline
        if(!friend.getFriendPresences().isEmpty()) {
            emptyPanelMessage = new MessageComponent(friendFileList.getSwingModel(), MessageTypes.ONLINE);
        } else {
            emptyPanelMessage = new MessageComponent(friendFileList.getSwingModel(), MessageTypes.OFFLINE);
        }
        setEmptyCard(emptyFactory.createEmptyLibrary(friend, friendFileList, FriendLibraryMediator.this, emptyPanelMessage));
    }
    
    /**
	 * ShowLibraryCard is called anytime a friend is selected in the left hand nav.
	 * Must ensure the appropriate empty panel/library is shown depending on
	 * the state of the friend.
	 */
    @Override
    public void showLibraryCard() {
        if(eventList == null || eventList.size() == 0) 
            showEmptyCard();
        else
            super.showLibraryCard();
    }
    
    public void updateLibraryPanel(EventList<RemoteFileItem> eventList, LibraryState libraryState) {
        if(!disposed) {
            switch(libraryState) { 
            case FAILED_TO_LOAD:
                this.eventList = null;
                emptyPanelMessage.setMessageType(MessageTypes.LW_CONNECTION_ERROR);
                if(!isSharingCardShown()) {
                    showEmptyCard();
                }
                break;
            case LOADED:
                if(eventList.size() == 0) {
                    emptyPanelMessage.setMessageType(MessageTypes.LW_NO_FILES);
                    if(!isSharingCardShown()) {
                        showEmptyCard();
                    }
                } else {
                    setLibraryCard(factory.createFriendLibrary(friend, friendFileList, eventList, this));
                    if(isEmptyCardShown()) {
                        showLibraryCard();
                    }
                }
                break;
            case LOADING:
                if(this.eventList != eventList) {
                    this.eventList = eventList;
                } 
                if(eventList.size() == 0) {
	                emptyPanelMessage.setMessageType(MessageTypes.LW_LOADING);
	                if(!isSharingCardShown()) {
	                    showEmptyCard();
	                }
                } else {
                    setLibraryCard(factory.createFriendLibrary(friend, friendFileList, eventList, this));
                    if(isEmptyCardShown()) {
                        showLibraryCard();
                    }
                }
                break;
            }
        }
    }
    
    @Override
    @SwingEDTEvent
    public void handleEvent(FriendEvent event) {
        if(!disposed) {
            switch(event.getType()) {
            case ADDED:
                //if friend signed on, show online view
                if(event.getSource().getId().equals(friend.getId())) {
                    emptyPanelMessage.setMessageType(MessageTypes.ONLINE);
                    if(!isSharingCardShown()) {
                        showEmptyCard();
                    }
                }
                break;
            case REMOVED:
                //if this friend signed off, show offline view
                if(event.getSource().getId().equals(friend.getId())) {
                    emptyPanelMessage.setMessageType(MessageTypes.OFFLINE);
                    if(!isSharingCardShown()) {
                        showEmptyCard();
                    }
                }
                break;
            }
        }
    }
    
    @Override
    public void dispose() {
        availListeners.removeListener(this);
        super.dispose();
    }
       
    /**
     * Various states the EmptyPanel can exist in. 
     */
    enum MessageTypes {
        OFFLINE, ONLINE, LW_LOADING, LW_NO_FILES, LW_CONNECTION_ERROR;
    };
    
    /**
	 * Hover over message panel in the EmptyLibrary. This displays feedback to the user as to
	 * what state their friend is in. 
	 */
    private class MessageComponent extends JXPanel implements ListEventListener<LocalFileItem>, Disposable {
        private EventList<LocalFileItem> friendList;
        private MessageTypes messageType;
        private final JLabel firstLabel;
        private final JLabel chatLabel;
        private final JLabel shareLabel;;
        private final HyperlinkButton shareLink;
        private final HyperlinkButton chatLink;
        
        public MessageComponent(EventList<LocalFileItem> friendList, MessageTypes messageType) {
            this.friendList = friendList;
            this.friendList.addListEventListener(this);
            this.messageType = messageType;
            
            
            setLayout(new MigLayout("insets 15 20 15 20, hidemode 3"));
            
            firstLabel = new JLabel();
            FontUtils.bold(firstLabel);

            shareLink = new HyperlinkButton(new ShowSharingAction());
            shareLabel = new JLabel();
            chatLink = new HyperlinkButton(new ChatWithAction());
            chatLabel = new JLabel();
            
            setMessage();
            
            add(firstLabel, "span, gapbottom 10, wrap");
            add(shareLink);
            add(shareLabel);
            add(chatLink);
            add(chatLabel);
            
            setMessage();
        }
        
        /**
	     * Update the message about this friend. 
		 */
        public void setMessageType(MessageTypes messageType) {
            this.messageType = messageType;
            setMessage();
            revalidate();
        }
        
        private void setMessage() {
            switch(messageType) {
                case OFFLINE: setOfflineMessage(); break;
                case ONLINE: setOnlineMessage(); break;
                case LW_LOADING: setLWLoading(); break;
                case LW_NO_FILES: setLWNoFiles(); break;
                case LW_CONNECTION_ERROR: setLWConnectionError(); break;
            }
        }
        
        /**
		 * Friend is offline.
		 */
        private void setOfflineMessage() {
            firstLabel.setText(I18n.tr("{0} is offline", friend.getFirstName()));
            if(friendList.size() == 1)
                setShareText(I18n.tr("Sharing"), I18n.tr("1 file with {0}.", friend.getFirstName()));
            else if(friendList.size() > 1)
                setShareText(I18n.tr("Sharing"), I18n.tr("{0} files with {1}.", friendFileList.size(), friend.getFirstName()));
            else
                setShareText(I18n.tr("Share"), I18n.tr("files with {0}.", friend.getFirstName()));
            setChatText(null, null);
        }
        
        /**
		 * Friend is online but not on LimeWire.
		 */
        private void setOnlineMessage() {
            firstLabel.setText(I18n.tr("{0} isn't on LimeWire", friend.getFirstName()));
            if(friendList.size() == 1){
                setShareText(I18n.tr("Sharing"), I18n.tr("1 file with {0}. ", friend.getFirstName()));
                setChatText(I18n.tr("Chat"), I18n.tr("about signing into LimeWire 5."));
            } else if(friendList.size() > 1){
                setShareText(I18n.tr("Sharing"), I18n.tr("{0} files with {1}. ", friendFileList.size(), friend.getFirstName()));
                setChatText(I18n.tr("Chat"), I18n.tr("about signing into LimeWire 5."));
            } else{
                setShareText(I18n.tr("Share"), I18n.tr("files with {0} and", friend.getFirstName()));
                setChatText(I18n.tr("chat"), I18n.tr("about signing into LimeWire 5."));
            }
        }
        
        /**
		 * Friend signed onto LimeWire and currently retrieving their Library.
		 */
        private void setLWLoading() {
            if(!friend.isAnonymous()) {
                firstLabel.setText(I18n.tr("Loading {0}'s files...", friend.getFirstName()));
                setShareText(null, null);
                setChatText(null, null);
            } else {
                firstLabel.setText(I18n.tr("Loading files...", friend.getFirstName()));
                setShareText(null, null);
                setChatText(null, null);
            }
        }
        
        /**
		 * Friend signed onto LimeWire, browse completed and not sharing any files with you.
		 */
        private void setLWNoFiles() {
            if(!friend.isAnonymous()) {
                firstLabel.setText(I18n.tr("{0} isn't sharing with you", friend.getFirstName()));
                setShareText(null, null);
                setChatText(I18n.tr("Chat"), I18n.tr("about sharing with you."));
            } else {
                //this should never happen
                firstLabel.setText(I18n.tr("This person isn't sharing any files with you."));
                setShareText(null, null);
                setChatText(null, null);
            }
        }
        
        /**
		 * Friend signed onto LimeWire, browse failed.
		 */
        private void setLWConnectionError() {
            if(!friend.isAnonymous()) {
                firstLabel.setText(I18n.tr("There was a problem viewing {0}'s files.", friend.getFirstName()));
                setShareText(null, null);
                setChatText(null, null);
            } else {
                firstLabel.setText(I18n.tr("There was a problem viewing this person's files."));
                setShareText(null, null);
                setChatText(null, null);
            }
        }
        
        /**
         * Sets the text of the share link and share label. If the share text is null,
         * both the link and the label are hidden.
         */
        private void setShareText(String shareLinkText, String shareText) {
            shareLink.setVisible(shareText != null);
            shareLabel.setVisible(shareText != null);
            
            shareLink.setText(shareLinkText);
            shareLabel.setText(shareText);
        }
        
        /**
         * Sets the text of the chat link and chat label. If the chat text is null,
         * both the link and the label are hidden.
         */
        private void setChatText(String chatLinkText, String chat) {
            chatLink.setVisible(chat != null);
            chatLabel.setVisible(chat != null);
            
            chatLink.setText(chatLinkText);
            chatLabel.setText(chat);
        }

        @Override
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            setMessage();
        }

        @Override
        public void dispose() {
            friendList.removeListEventListener(this);
        }
    }

    @Override
    public void showSharingCard() {
        if(!disposed) {
            if(!isSharingCardSet()) {
                setSharingCard(sharingFactory.createPanel(this, friend, 
                        libraryManager.getLibraryManagedList().getSwingModel(),
                        shareListManager.getOrCreateFriendShareList(friend)));
            }
            super.showSharingCard();            
        }
    }
    
    /**
     * Action which displays the sharing view for this friend
     */
    private class ShowSharingAction extends AbstractAction {
        public ShowSharingAction() {
            putValue(AbstractAction.NAME, I18n.tr("Share"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            showSharingCard();
        }
    }
    
    /**
     * Action which displays the chat window and starts a chat with
     * this friend.
     */
    private class ChatWithAction extends AbstractAction {
        public ChatWithAction() {
            putValue(AbstractAction.NAME, I18n.tr("Chat"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            friendsPanel.setChatPanelVisible(true);
            friendsPane.fireConversationStarted(friend.getId());
        }
    }
}
