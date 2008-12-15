package org.limewire.ui.swing.library;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import ca.odell.glazedlists.EventList;
import net.miginfocom.swing.MigLayout;

public class FriendLibraryMediator extends LibraryMediator {

    private final EmptyLibraryFactory emptyFactory;
    private final FriendLibraryFactory factory;
    private final FriendSharingPanelFactory sharingFactory;
    private final LibraryManager libraryManager;
    private final ShareListManager shareListManager;

    private final Friend friend;
    private EventList<RemoteFileItem> eventList;
    
    @AssistedInject
    public FriendLibraryMediator(@Assisted Friend friend, FriendLibraryFactory factory, EmptyLibraryFactory emptyFactory,
            FriendSharingPanelFactory sharingFactory, LibraryManager libraryManager, ShareListManager shareListManager) {
        this.factory = factory;
        this.friend = friend;        
        this.sharingFactory = sharingFactory;
        this.emptyFactory = emptyFactory;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        
        setLibraryCard(emptyFactory.createEmptyLibrary(friend, this, new OffLineMessageComponent()));
    }
    
    public void showLibraryPanel(EventList<RemoteFileItem> eventList, LibraryState libraryState) {
        switch(libraryState) {
        case FAILED_TO_LOAD:
            this.eventList = null;
            setLibraryCard(emptyFactory.createEmptyLibrary(friend, this, new ConnectionErrorComponent()));
            showLibraryCard();
            break;
        case LOADED:
        case LOADING:
            if(this.eventList != eventList) {
                this.eventList = eventList;
                setLibraryCard(factory.createFriendLibrary(friend, eventList, this));
                showLibraryCard();
            }
            break;
        }
    }        
    
    /**
     * Message to display when a friend is offline
     */
    private class OffLineMessageComponent extends JPanel {
        public OffLineMessageComponent() {
            setLayout(new MigLayout());
            
            JLabel label = new JLabel(I18n.tr("{0} isn't on LimeWire", friend.getRenderName()));
            FontUtils.bold(label);
//            JLabel secondLabel = new JLabel(I18n.tr("You're sharing {0} files with {1}", "?", friend.getFirstName()));
          
            add(label, "wrap");
//            add(secondLabel, "gaptop 10");
        }
    }
    
    /**
     * Message to display when a friend is on LW but couldn't perform a browse
     */
    private class ConnectionErrorComponent extends JPanel {
        public ConnectionErrorComponent() {
            setLayout(new MigLayout());
            
            JLabel label = new JLabel(I18n.tr("{0} is on LimeWire but there were problems viewing their library", friend.getRenderName()));
            FontUtils.bold(label);
//            JLabel secondLabel = new JLabel(I18n.tr("You're sharing {0} files with {1}", "?", friend.getFirstName()));
          
            add(label, "wrap");
//            add(secondLabel, "gaptop 10");
        }
    }

    @Override
    public void showSharingCard() {
        if(!isSharingCardSet()) {
            setSharingCard(sharingFactory.createPanel(this, friend, 
                    libraryManager.getLibraryManagedList().getSwingModel(),
                    shareListManager.getOrCreateFriendShareList(friend)));
        }
        super.showSharingCard();
    }
}
