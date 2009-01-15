package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class FriendLibraryPanel extends AbstractFriendLibraryPanel {

    @Resource(key="LibraryPanel.selectionPanelBackgroundOverride") 
    private Color selectionPanelBackgroundOverride = null;    
    
    private final Friend friend;
    
    @AssistedInject
    public FriendLibraryPanel(@Assisted Friend friend,
                    @Assisted FriendFileList friendFileList,
                    @Assisted EventList<RemoteFileItem> eventList, 
                    @Assisted FriendLibraryMediator mediator,
                    CategoryIconManager categoryIconManager, 
                    LibraryTableFactory tableFactory,
                    DownloadListManager downloadListManager,
                    LibraryManager libraryManager,
                    LimeHeaderBarFactory headerBarFactory,
                    ButtonDecorator buttonDecorator,
                    GhostDragGlassPane ghostPane,
                    LibraryNavigator libraryNavigator) {
        super(friend, friendFileList, categoryIconManager, tableFactory, downloadListManager, libraryManager, headerBarFactory, ghostPane, libraryNavigator);

        GuiUtils.assignResources(this);
        
        this.friend = friend;

        if (selectionPanelBackgroundOverride != null) { 
            getSelectionPanel().setBackground(selectionPanelBackgroundOverride);
        }
        
        //don't show share button for browse hosts
        if(!friend.isAnonymous()) {
            addButtonToHeader(new ViewSharedLibraryAction(mediator), buttonDecorator);
        }
        
        createMyCategories(eventList);
        selectFirstVisible();
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
            putValue(Action.NAME, I18n.tr("Share"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Share your files with {0}", friend.getRenderName()));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            friendLibraryMediator.showSharingCard();
        }
    }
}