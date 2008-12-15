package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import ca.odell.glazedlists.EventList;

public class FriendSharingPanel extends SharingPanel {
    
    private final Friend friend;

    @AssistedInject
    public FriendSharingPanel(
            @Assisted LibraryMediator returnToLibraryPanel,
            @Assisted Friend friend,
            @Assisted EventList<LocalFileItem> wholeLibraryList, 
            @Assisted FriendFileList friendFileList,
            IconManager iconManager,
            CategoryIconManager categoryIconManager,
            LibraryTableFactory tableFactory,
            LimeHeaderBarFactory headerBarFactory,
            ButtonDecorator buttonDecorator) {
        super(wholeLibraryList, friendFileList, categoryIconManager, tableFactory, headerBarFactory);
        this.friend = friend;
        
        addButtonToHeader(new BackToLibraryAction(returnToLibraryPanel), buttonDecorator, AccentType.NONE);
        getHeaderPanel().setText(I18n.tr("Share with {0}", getFullPanelName()));
        
        createMyCategories(wholeLibraryList, friendFileList);
        selectFirst();
    }
    
    protected String getFullPanelName() {
        return friend.getRenderName();
    }
    
    protected String getShortPanelName() {
        return friend.getFirstName();
    }    
    
    private static class BackToLibraryAction extends AbstractAction {
        private final LibraryMediator basePanel;

        public BackToLibraryAction(LibraryMediator basePanel) {
            this.basePanel = basePanel;
            putValue(Action.NAME, I18n.tr("Back"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Returns to what's being shared with you."));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            basePanel.showLibraryCard();
        }
    }
    
}
