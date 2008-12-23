package org.limewire.ui.swing.library;

import javax.swing.JLabel;

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
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import net.miginfocom.swing.MigLayout;

public class FriendLibraryMediator extends LibraryMediator implements EventListener<FriendEvent> {

    private final EmptyLibraryFactory emptyFactory;
    private final FriendLibraryFactory factory;
    private final FriendSharingPanelFactory sharingFactory;
    private final LibraryManager libraryManager;
    private final ShareListManager shareListManager;

    private final Friend friend;
    private final FriendFileList friendFileList;
    private EventList<RemoteFileItem> eventList;
    
    private ListenerSupport<FriendEvent> availListeners;

    @AssistedInject
    public FriendLibraryMediator(@Assisted Friend friend, FriendLibraryFactory factory, EmptyLibraryFactory emptyFactory,
            FriendSharingPanelFactory sharingFactory, LibraryManager libraryManager, ShareListManager shareListManager,
            @Named("available") ListenerSupport<FriendEvent> availListeners) {
        this.factory = factory;
        this.friend = friend;        
        this.sharingFactory = sharingFactory;
        this.emptyFactory = emptyFactory;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.friendFileList = shareListManager.getOrCreateFriendShareList(friend);
        this.availListeners = availListeners;
        this.availListeners.addListener(this);
        
        if(!friend.getFriendPresences().isEmpty())
            setLibraryCard(emptyFactory.createEmptyLibrary(friend, friendFileList, FriendLibraryMediator.this, new OnLineMessageComponent(friendFileList.getSwingModel())));
        else
            setLibraryCard(emptyFactory.createEmptyLibrary(friend, friendFileList, this, new OffLineMessageComponent(friendFileList.getSwingModel())));
    }
    
    public void showLibraryPanel(EventList<RemoteFileItem> eventList, LibraryState libraryState) {
        if(!disposed) {
            switch(libraryState) { 
            case FAILED_TO_LOAD:
                this.eventList = null;
                setLibraryCard(emptyFactory.createEmptyLibrary(friend, friendFileList, this, new ConnectionErrorComponent(friendFileList.getSwingModel())));
                showLibraryCard();
                break;
            case LOADED:
            case LOADING:
                if(this.eventList != eventList) {
                    this.eventList = eventList;
                    setLibraryCard(factory.createFriendLibrary(friend, friendFileList, eventList, this));
                    showLibraryCard();
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
                    setLibraryCard(emptyFactory.createEmptyLibrary(friend, friendFileList, FriendLibraryMediator.this, new OnLineMessageComponent(friendFileList.getSwingModel())));
                }
                break;
            case REMOVED: 
                //if this friend signed off, show offline view
                if(event.getSource().getId().equals(friend.getId())) {
                    setLibraryCard(emptyFactory.createEmptyLibrary(friend, friendFileList, FriendLibraryMediator.this, new OffLineMessageComponent(friendFileList.getSwingModel())));
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
     * Message to display when a friend is offline
     */
    public class OffLineMessageComponent extends MessageComponent {
        public OffLineMessageComponent(EventList<LocalFileItem> friendList) {
            super(friendList, I18n.tr("{0} is offline", friend.getFirstName()));
        }
        
        @Override
        protected void setMessage() {
            if(friendList.size() > 0)
                secondLabel.setText(I18n.tr("You're sharing {0} files with {1}.", friendFileList.size(), friend.getFirstName()));
            else
                secondLabel.setText(I18n.tr("Share with {0} for when they sign on LimeWire.", friend.getFirstName()));
        }
    }
    
    /**
     * Message to display when a friend is online but not using LW
     */
    public class OnLineMessageComponent extends MessageComponent {
        public OnLineMessageComponent(EventList<LocalFileItem> friendList) {
            super(friendList, I18n.tr("{0} is online", friend.getFirstName()));
        }
        
        @Override
        protected void setMessage() {
            if(friendList.size() > 0)
                secondLabel.setText(I18n.tr("You're sharing {0} files with {1}.", friendFileList.size(), friend.getFirstName()));
            else
                secondLabel.setText(I18n.tr("Share with {0} for when they sign on LimeWire.", friend.getFirstName()));
        }
    }
    
    /**
     * Message to display when a friend is on LW but couldn't perform a browse
     */
    private class ConnectionErrorComponent extends MessageComponent {
        public ConnectionErrorComponent(EventList<LocalFileItem> friendList) {
            super(friendList, I18n.tr("{0} is on LimeWire but there were problems viewing their library", friend.getFirstName()));
        }
        
        @Override
        protected void setMessage() {
            if(friendList.size() > 0)
                secondLabel.setText(I18n.tr("You're sharing {0} files with {1}. Chat about signing into LimeWire 5.", friendFileList.size(), friend.getFirstName()));
            else
                secondLabel.setText(I18n.tr("Share files with {0} and chat about signing into LimeWire 5.", friend.getFirstName()));
        }
    }
    
    private abstract class MessageComponent extends JXPanel implements ListEventListener<LocalFileItem>, Disposable {
        protected EventList<LocalFileItem> friendList;
        protected JLabel secondLabel;
        
        public MessageComponent(EventList<LocalFileItem> friendList, String title) {
            this.friendList = friendList;
            this.friendList.addListEventListener(this);
            
            setLayout(new MigLayout("insets 16"));
            
            JLabel label = new JLabel(title);
            FontUtils.bold(label);
            secondLabel = new JLabel();
            setMessage();
            
            add(label, "wrap");
            add(secondLabel, "gaptop 10");
        }
        
        abstract protected void setMessage();

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

}
