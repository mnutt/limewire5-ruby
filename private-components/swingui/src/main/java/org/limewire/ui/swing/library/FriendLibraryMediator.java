package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;

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
import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.friends.chat.ChatFriendListPane;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

public class FriendLibraryMediator extends LibraryMediator implements EventListener<FriendEvent> {

    private Message emptyPanelMessage;
    
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
    
    private final ListenerSupport<FriendEvent> availListeners;

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
            emptyPanelMessage = new Message(friendFileList.getSwingModel(), MessageTypes.ONLINE);
        } else {
            emptyPanelMessage = new Message(friendFileList.getSwingModel(), MessageTypes.OFFLINE);
        }
        setEmptyCard(emptyFactory.createEmptyLibrary(friend, friendFileList, FriendLibraryMediator.this, emptyPanelMessage, emptyPanelMessage.getComponent()));
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
                // must do this here also, may skip loading step all together
                if(this.eventList != eventList) {
                    this.eventList = eventList;
                } 
                if(eventList.size() == 0) {
                    emptyPanelMessage.setMessageType(MessageTypes.LW_NO_FILES);
                    if(!isSharingCardShown()) {
                        showEmptyCard();
                    }
                } else {
                    setLibraryCard(factory.createFriendLibrary(friend, friendFileList, eventList, this));
                    if(isEmptyCardShown()) {
                        super.showLibraryCard();
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
                        super.showLibraryCard();
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
    private class Message implements ListEventListener<LocalFileItem>, Disposable {                
        private MessageComponent messageComponent;
        
        private EventList<LocalFileItem> friendList;
        private MessageTypes messageType;
        private final JLabel firstLabel;
        private final JLabel chatLabel;
        private final JLabel shareLabel;;
        private final HyperlinkButton shareLink;
        private final HyperlinkButton chatLink;
        
        public Message(EventList<LocalFileItem> friendList, MessageTypes messageType) {
            this.friendList = friendList;
            this.friendList.addListEventListener(this);
            this.messageType = messageType;
            
            messageComponent = new MessageComponent();
            
            firstLabel = new JLabel();
            messageComponent.decorateHeaderLabel(firstLabel);

            shareLink = new HyperlinkButton(new ShowSharingAction());
            messageComponent.decorateFont(shareLink);

            shareLabel = new JLabel();
            messageComponent.decorateSubLabel(shareLabel);

            chatLink = new HyperlinkButton(new ChatWithAction());
            messageComponent.decorateFont(chatLink);

            chatLabel = new JLabel();
            messageComponent.decorateSubLabel(chatLabel);
            
            setMessage();
            
            messageComponent.addComponent(firstLabel, "span, gapbottom 0, wrap");
            messageComponent.addComponent(shareLink,"");
            messageComponent.addComponent(shareLabel,"");
            messageComponent.addComponent(chatLink,"");
            messageComponent.addComponent(chatLabel,"");
            
            setMessage();
        }
        
        public JComponent getComponent() {
            return messageComponent;
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
            firstLabel.setText(I18n.tr("{0} is offline", friend.getRenderName()));
            if(friendList.size() == 1)
                setShareText(I18n.tr("Sharing"), I18n.tr("1 file with your friend."));
            else if(friendList.size() > 1)
                setShareText(I18n.tr("Sharing"), I18n.tr("{0} files with your friend.", friendFileList.size()));
            else
                setShareText(I18n.tr("Share"), I18n.tr("files with your friend."));
            setChatText(null, null);
        }
        
        /**
		 * Friend is online but not on LimeWire.
		 */
        private void setOnlineMessage() {
            firstLabel.setText(I18n.tr("{0} isn't on LimeWire", friend.getRenderName()));
            if(friendList.size() == 1){
                setShareText(I18n.tr("Sharing"), I18n.tr("1 file with your friend. "));
                setChatText(I18n.tr("Chat"), I18n.tr("about using LimeWire 5."));
            } else if(friendList.size() > 1){
                setShareText(I18n.tr("Sharing"), I18n.tr("{0} files with your friend. ", friendFileList.size()));
                setChatText(I18n.tr("Chat"), I18n.tr("about using LimeWire 5."));
            } else{
                setShareText(I18n.tr("Share"), I18n.tr("files with your friend and"));
                setChatText(I18n.tr("chat"), I18n.tr("about using LimeWire 5."));
            }
        }
        
        /**
		 * Friend signed onto LimeWire and currently retrieving their Library.
		 */
        private void setLWLoading() {
            if(!friend.isAnonymous()) {
                firstLabel.setText(I18n.tr("Loading {0}'s files...", friend.getRenderName()));
                setShareText(null, null);
                setChatText(null, null);
            } else {
                firstLabel.setText(I18n.tr("Loading files..."));
                setShareText(null, null);
                setChatText(null, null);
            }
        }
        
        /**
		 * Friend signed onto LimeWire, browse completed and not sharing any files with you.
		 */
        private void setLWNoFiles() {
            if(!friend.isAnonymous()) {
                firstLabel.setText(I18n.tr("{0} isn't sharing with you", friend.getRenderName()));
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
                firstLabel.setText(I18n.tr("There was a problem viewing {0}'s files.", friend.getRenderName()));
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
