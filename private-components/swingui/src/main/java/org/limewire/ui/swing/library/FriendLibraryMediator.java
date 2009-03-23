package org.limewire.ui.swing.library;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.friends.chat.ChatFriendListPane;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
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
    private final LibraryNavigator libraryNavigator;
    private final ChatFriendListPane friendsPane;
    private final ChatFramePanel friendsPanel;
    
    private final Friend friend;
    private final FriendFileList friendFileList;
    private EventList<RemoteFileItem> eventList;
    
    private final ListenerSupport<FriendEvent> availListeners;
    
    private ShowLibraryListener showLibraryListener;

    @AssistedInject
    public FriendLibraryMediator(@Assisted Friend friend, FriendLibraryFactory factory, EmptyLibraryFactory emptyFactory,
                                 ShareListManager shareListManager,
                                 @Named("available")ListenerSupport<FriendEvent> availListeners,
                                 ChatFriendListPane friendsPane, ChatFramePanel friendsPanel,
                                 LibraryNavigator libraryNavigator) {
        this.factory = factory;
        this.friend = friend;        
        this.emptyFactory = emptyFactory;
        this.friendsPane = friendsPane;
        this.friendsPanel = friendsPanel;
        this.friendFileList = shareListManager.getOrCreateFriendShareList(friend);
        this.availListeners = availListeners;
        this.availListeners.addListener(this);
        this.libraryNavigator = libraryNavigator;
        
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
        setEmptyCard(emptyFactory.createEmptyLibrary(friend, friendFileList, emptyPanelMessage, emptyPanelMessage.getComponent()));
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
                removeEventListener(true);
                this.eventList = null;
                emptyPanelMessage.setMessageType(MessageTypes.LW_CONNECTION_ERROR);
                    showEmptyCard();
                break;
            case LOADED:
                // must do this here also, may skip loading step all together
                if(this.eventList != eventList) {
                    removeEventListener(true);
                    this.eventList = eventList;
                } 
                if(eventList.size() == 0) {
                    emptyPanelMessage.setMessageType(MessageTypes.LW_NO_FILES);
                        showEmptyCard();
                    registerEventListener();
                } else {
                    showLibrary();
                }
                break;
            case LOADING:
                if(this.eventList != eventList) {
                    removeEventListener(true);
                    this.eventList = eventList;
                } 
                if(eventList.size() == 0) {
	                emptyPanelMessage.setMessageType(MessageTypes.LW_LOADING);
	                    showEmptyCard();
	                registerEventListener();
                } else {
                    showLibrary();
                }
                break;
            }
        }
    }
    
    private void registerEventListener() {
        if(eventList != null && showLibraryListener == null) {
            showLibraryListener = new ShowLibraryListener();
            eventList.addListEventListener(showLibraryListener);
        }
    }
    
    private void removeEventListener(boolean cancel) {
        if(eventList != null && showLibraryListener != null) {
            if(cancel) {
                showLibraryListener.cancelled = true;
            }
            eventList.removeListEventListener(showLibraryListener);
            showLibraryListener = null;
        }
    }
    
    private void showLibrary() {
        if(isEmptyCardShown()) {
            removeEventListener(true);
            setLibraryCard(factory.createFriendLibrary(friend, friendFileList, eventList));
            super.showLibraryCard();
        }
    }
    
    @Override
    @SwingEDTEvent
    public void handleEvent(FriendEvent event) {
        if(!disposed) {
            switch(event.getType()) {
            case ADDED:
                //if friend signed on, show online view
                if(event.getData().getId().equals(friend.getId())) {
                    emptyPanelMessage.setMessageType(MessageTypes.ONLINE);
                        showEmptyCard();
                    }
                break;
            case REMOVED:
                //if this friend signed off, show offline view
                if(event.getData().getId().equals(friend.getId())) {
                    emptyPanelMessage.setMessageType(MessageTypes.OFFLINE);
                        showEmptyCard();
                    }
                break;
            }
        }
    }
    
    @Override
    public void dispose() {
        removeEventListener(true);
        availListeners.removeListener(this);
        super.dispose();
    }
       
    /**
     * Various states the EmptyPanel can exist in. 
     */
    enum MessageTypes {
        OFFLINE, ONLINE, LW_LOADING, LW_NO_FILES, LW_CONNECTION_ERROR
    }
    
    /**
     * Listens to library loads. When first file from friend is displayed, show
     * the library and remove this listener.
     */
    private class ShowLibraryListener implements ListEventListener<RemoteFileItem> {
        private boolean cancelled = false;
        
        @Override
        public void listChanged(ListEvent<RemoteFileItem> listChanges) {
            if(listChanges.getSourceList().size() > 0 ) {
                removeEventListener(false);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if(!cancelled) {
                            showLibrary();
                        }
                    }
                });
            }
        }
    }
    
    /**
	 * Hover over message panel in the EmptyLibrary. This displays feedback to the user as to
	 * what state their friend is in. 
	 */
    private class Message implements ListEventListener<LocalFileItem>, Disposable {                
        
        private static final String SHARE_ANCHOR = "<a href='#share'>";
        private static final String CHAT_ANCHOR = "<a href='#chat'>";
        private static final String CLOSING_ANCHOR = "</a>";
        
        
        private MessageComponent messageComponent;
        
        private EventList<LocalFileItem> friendList;
        private MessageTypes messageType;
        private final JLabel firstLabel;
        private final HTMLLabel shareLabel;
        
        public Message(EventList<LocalFileItem> friendList, MessageTypes messageType) {
            this.friendList = friendList;
            this.friendList.addListEventListener(this);
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

            setMessage();
            
            messageComponent.addComponent(firstLabel, "span, gapbottom 0, wrap");
            messageComponent.addComponent(shareLabel, "");
            
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
            // {0}, {1}: html tags, {2}: number of files shared
            setShareText(I18n.trn("You're {0}sharing {1} file{2} with your friend.", "You're {0}sharing {1} files{2} with your friend.", friendList.size(), SHARE_ANCHOR, friendList.size(), CLOSING_ANCHOR));
        }
        
        /**
		 * Friend is online but not on LimeWire.
		 */
        private void setOnlineMessage() {
            firstLabel.setText(I18n.tr("{0} isn't on LimeWire", friend.getRenderName()));
            // {0}, {1}: html tags, {2}: number of files shared
            setShareText(I18n.trn("You're {0}sharing {1} file{2} with your friend.", "You're {0}sharing {1} files{2} with your friend.", friendList.size(), SHARE_ANCHOR, friendList.size(), CLOSING_ANCHOR) + " "
                    // {0}, {1}: html tags
                    + I18n.tr("{0}Chat{1} about signing into LimeWire 5.", CHAT_ANCHOR, CLOSING_ANCHOR));
        }
        
        /**
		 * Friend signed onto LimeWire and currently retrieving their Library.
		 */
        private void setLWLoading() {
            hideShareText();
            if(!friend.isAnonymous()) {
                firstLabel.setText(I18n.tr("Loading {0}'s files...", friend.getRenderName()));
            } else {
                firstLabel.setText(I18n.tr("Loading files..."));
            }
        }
        
        /**
		 * Friend signed onto LimeWire, browse completed and not sharing any files with you.
		 */
        private void setLWNoFiles() {
            if(!friend.isAnonymous()) {
                firstLabel.setText(I18n.tr("{0} isn't sharing with you", friend.getRenderName()));
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
        
        @Override
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            setMessage();
        }

        @Override
        public void dispose() {
            friendList.removeListEventListener(this);
        }
    }
}
