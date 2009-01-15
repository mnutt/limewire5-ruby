package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Icon;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class FriendSharingPanel extends SharingPanel {
    
    private final Friend friend;
    
    // TODO: Resource injection to leaf nodes here does not work. (because we are not singleton?)
    @Resource(key="FriendSharingPanel.backButton.icon") Icon icon;
    @Resource(key="FriendSharingPanel.backButton.rolloverIcon") Icon rolloverIcon;
    @Resource(key="FriendSharingPanel.backButton.pressedIcon") Icon pressedIcon;
    
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
            GhostDragGlassPane ghostPane) {

        super(wholeLibraryList, friendFileList, categoryIconManager, tableFactory, headerBarFactory, ghostPane, friend);
        
        setInnerNavLayout(new MigLayout("insets 0, gap 0, fill, wrap, hidemode 3", "[138!]", ""));
        
        GuiUtils.assignResources(this);
        
        this.friend = friend;
        
        getHeaderPanel().setText(I18n.tr("Share with {0}", getFullPanelName()));
        
        IconButton backButton = new IconButton(new BackToLibraryAction(returnToLibraryPanel));
        
        // TODO: See above todo -- This isn't working because name assignement only works
        //                         if the component exists on app startup.  Since these
        //                         components are created on the fly, only @Resource works.
        //backButton.setName("FriendSharingPanel.backButton");
        backButton.setIcon(icon);
        backButton.setPressedIcon(pressedIcon);
        backButton.setRolloverIcon(rolloverIcon);
        backButton.removeActionHandListener();
        
        backButton.setFocusPainted(false);
        addBackButton(backButton);
                
        createMyCategories(wholeLibraryList, friendFileList);
        selectFirstVisible();
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
            
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Return to what's being shared with you."));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            basePanel.showLibraryCard();
        }
    }
    
}
