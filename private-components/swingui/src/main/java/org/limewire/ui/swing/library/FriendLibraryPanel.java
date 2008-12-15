package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class FriendLibraryPanel extends AbstractFriendLibraryPanel {

    private final Friend friend;
    
    @AssistedInject
    public FriendLibraryPanel(@Assisted Friend friend,
                    @Assisted EventList<RemoteFileItem> eventList, 
                    @Assisted FriendLibraryMediator mediator,
                    CategoryIconManager categoryIconManager, 
                    LibraryTableFactory tableFactory,
                    DownloadListManager downloadListManager,
                    LibraryManager libraryManager,
                    LimeHeaderBarFactory headerBarFactory,
                    ButtonDecorator buttonDecorator) {
        super(eventList, categoryIconManager, tableFactory, downloadListManager, libraryManager, headerBarFactory);
        
        this.friend = friend;

        //don't show share button for browse hosts
        if(!friend.isAnonymous()) {
            addButtonToHeader(new ViewSharedLibraryAction(mediator), buttonDecorator);
        }
        
        createMyCategories(eventList, friend);
        selectFirst();
        getHeaderPanel().setText(I18n.tr("Download from {0}", getFullPanelName()));
    }
    
    protected String getFullPanelName() {
        return friend.getRenderName();
    }
    
    protected String getShortPanelName() {
        return friend.getFirstName();
    }
    
    private class ViewSharedLibraryAction extends AbstractAction {
        private final FriendLibraryMediator friendLibraryMediator;

        public ViewSharedLibraryAction(FriendLibraryMediator friendLibraryMediator) {
            this.friendLibraryMediator = friendLibraryMediator;
            putValue(Action.NAME, I18n.tr("Share with {0}", getShortPanelName()));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Show files you're sharing with {0}", getShortPanelName()));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            friendLibraryMediator.showSharingCard();
        }
    }
}