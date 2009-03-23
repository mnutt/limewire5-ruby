package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.login.FriendsSignInPanel;
import org.limewire.ui.swing.library.MyLibraryPanel;
import org.limewire.ui.swing.library.SelectAllable;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.library.sharing.model.MultiFileShareModel;
import org.limewire.ui.swing.library.sharing.model.MultiFileUnshareModel;
import org.limewire.ui.swing.library.table.menu.actions.DisabledFriendLoginAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPService;

/**
 * Share All combo Box. In My Library view allows the user
 * to share/unshare all the files with Gnutella or a Friend
 */
public class ShareAllComboBox extends LimeComboBox {

    @Resource
    private Color labelColor;
    @Resource
    private Font labelFont;
    @Resource
    private Color disabledColor;
    @Resource
    private Icon gnutellaShareIcon;
    @Resource
    private Icon friendShareIcon;
    @Resource
    private Icon gnutellaUnshareIcon;
    @Resource
    private Icon friendUnshareIcon;
    
    private final XMPPService xmppService;
    private final FriendsSignInPanel friendsSignInPanel;
    private final ShareWidgetFactory shareWidgetFactory;
    private final MyLibraryPanel myLibraryPanel;
    private final ShareListManager shareListManager;
    
    private JPopupMenu menu = new JPopupMenu();
    
    private AbstractAction shareAllGnutellaAction;
    private AbstractAction unshareAllGnutellaAction;
    private AbstractAction shareAllFriendAction;
    private AbstractAction unshareAllFriendAction;
    private AbstractAction signedOutAction;
    
    public ShareAllComboBox(XMPPService xmppService, ShareWidgetFactory shareWidgetFactory,
            MyLibraryPanel myLibraryPanel, FriendsSignInPanel friendsSignInPanel, ShareListManager shareListManager) {
        this.xmppService = xmppService;
        this.friendsSignInPanel = friendsSignInPanel;
        this.shareWidgetFactory = shareWidgetFactory;
        this.myLibraryPanel = myLibraryPanel;
        this.shareListManager = shareListManager;
                
        GuiUtils.assignResources(this);
        
        overrideMenu(menu);
        
        createActions();
        
        SharingListener listener = new SharingListener();
        menu.addPopupMenuListener(listener);
    }
    
    private void createActions() {
        shareAllGnutellaAction = new ShareAllAction(true);
        unshareAllGnutellaAction = new UnShareAllAction(true);
        shareAllFriendAction = new ShareAllAction(false);
        unshareAllFriendAction = new UnShareAllAction(false);
        signedOutAction = new DisabledFriendLoginAction(I18n.tr("Sign in to share with friends"), friendsSignInPanel);
    }
    
    private JMenuItem decorateItem(AbstractAction action) {
        JMenuItem item = new JMenuItem(action);
        item.setForeground(labelColor);
        item.setFont(labelFont);
        return item;
    }
    
    private JMenuItem decorateDisabledItem(AbstractAction action) {
        JMenuItem item = new JMenuItem(action);
        item.setForeground(disabledColor);
        item.setFont(labelFont);
        return item;
    }
    
    /**
     * Listens for combo box selection and creates the 
     * menu of shared lists to choose from.
     */
    private class SharingListener implements PopupMenuListener {        
        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {}
        
        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
        
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            menu.removeAll();        
            
            // if document category is selected and document sharing with p2p is disabled
            if(myLibraryPanel.getCategory() == Category.DOCUMENT && !LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue()) {
                // if not logged in
                if(!xmppService.isLoggedIn()) {
                    menu.add(decorateDisabledItem(new DisabledDocumentAction()));
                } else {                   
                    menu.add(decorateItem(shareAllFriendAction));
                    menu.add(decorateItem(unshareAllFriendAction));
                    
                    menu.addSeparator();
                    
                    menu.add(decorateDisabledItem(new DisabledDocumentAction()));
                }
            } else {
                // if not logged in don't show options for friends.
                if(!xmppService.isLoggedIn()) { 
                    if(myLibraryPanel.getCurrentFriend() != null && myLibraryPanel.getCurrentFriend().getId().equals(SharingTarget.GNUTELLA_SHARE.getFriend().getId()))
                        menu.add(decorateDisabledItem(shareAllGnutellaAction));
                    else
                        menu.add(decorateItem(shareAllGnutellaAction));
                    menu.add(decorateItem(unshareAllGnutellaAction));
                    
                    menu.addSeparator();
                    
                    menu.add(decorateDisabledItem(signedOutAction));
                } else {
                    if(myLibraryPanel.getCurrentFriend() != null && myLibraryPanel.getCurrentFriend().getId().equals(SharingTarget.GNUTELLA_SHARE.getFriend().getId()))
                        menu.add(decorateDisabledItem(shareAllGnutellaAction));
                    else
                        menu.add(decorateItem(shareAllGnutellaAction));
                    menu.add(decorateItem(shareAllFriendAction));
                    
                    menu.addSeparator();
                    
                    menu.add(decorateItem(unshareAllGnutellaAction));
                    menu.add(decorateItem(unshareAllFriendAction));
                }
            }
        }
    }
    
    /**
	 *	If Gnutella is chosen, shares all the files in the current table with Gnutella.
	 *  If a friend is selected, opens the Multi-file share widget
	 */
    private class ShareAllAction extends AbstractAction {    
        private final boolean isGnutella;
        
        public ShareAllAction(boolean isGnutella) {
            this.isGnutella = isGnutella;
            
            if(!isGnutella) {
                putValue(Action.NAME, I18n.tr("Share all with Friend..."));
                putValue(Action.SMALL_ICON, friendShareIcon);
            } else {
                putValue(Action.NAME, I18n.tr("Share all with the P2P Network"));
                putValue(Action.SMALL_ICON, gnutellaShareIcon);
            }
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            Category currentCategory = myLibraryPanel.getCategory();
            
            // if category sharing is enabled, show the category sharing widget
            if((currentCategory == Category.AUDIO || currentCategory == Category.IMAGE || currentCategory == Category.VIDEO)
                    && LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue() != true) {
                ShareWidget<Category> shareWidget = shareWidgetFactory.createCategoryShareWidget();
                shareWidget.setShareable(currentCategory);
                shareWidget.show(GuiUtils.getMainFrame());
            } else {  
                SelectAllable<LocalFileItem> selectAllable = myLibraryPanel.getTable();
                if(!isGnutella) {                
                    selectAllable.selectAll();
                    List<LocalFileItem> selectedItems = selectAllable.getSelectedItems();                
                    if (selectedItems.size() > 0) {
                        ShareWidget<LocalFileItem[]> shareWidget = shareWidgetFactory.createMultiFileShareWidget();
                        shareWidget.setShareable(selectedItems.toArray(new LocalFileItem[selectedItems.size()]));
                        shareWidget.show(GuiUtils.getMainFrame());
                    } 
                } else {
                    List<LocalFileItem> items = selectAllable.getAllItems();
                    MultiFileShareModel model = new MultiFileShareModel(shareListManager, items.toArray(new LocalFileItem[items.size()]));
                    model.shareFriend(SharingTarget.GNUTELLA_SHARE);
                }
            }
        }
    }
    
    /**
	 *	If Gnutella is chosen, unshares all the files in the current table with Gnutella.
	 *  If a friend is selected, opens the Multi-file unshare widget
	 */
    private class UnShareAllAction extends AbstractAction {
        private final boolean isGnutella;
        
        public UnShareAllAction(boolean isGnutella) {
            this.isGnutella = isGnutella;
            
            if(!isGnutella) {
                putValue(Action.NAME, I18n.tr("Unshare all with Friend..."));
                putValue(Action.SMALL_ICON, friendUnshareIcon);
            } else {
                putValue(Action.NAME, I18n.tr("Unshare all with the P2P Network"));
                putValue(Action.SMALL_ICON, gnutellaUnshareIcon);
            }
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {            
            Category currentCategory = myLibraryPanel.getCategory();
            
            if(LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue() != true && 
                    (currentCategory == Category.AUDIO || currentCategory == Category.IMAGE || currentCategory == Category.VIDEO)) {
                ShareWidget<Category> shareWidget = shareWidgetFactory.createCategoryShareWidget();
                shareWidget.setShareable(currentCategory);
                shareWidget.show(GuiUtils.getMainFrame());
            } else {    
                SelectAllable<LocalFileItem> selectAllable = myLibraryPanel.getTable();
                if(!isGnutella) {                
                    selectAllable.selectAll();
                    List<LocalFileItem> selectedItems = selectAllable.getSelectedItems();                
                    if (selectedItems.size() > 0) {
                        ShareWidget<LocalFileItem[]> shareWidget = shareWidgetFactory.createMultiFileUnshareWidget();
                        shareWidget.setShareable(selectedItems.toArray(new LocalFileItem[selectedItems.size()]));
                        shareWidget.show(GuiUtils.getMainFrame());
                    }
                } else {
                    List<LocalFileItem> items = selectAllable.getAllItems();                
                    MultiFileUnshareModel model = new MultiFileUnshareModel(shareListManager, items.toArray(new LocalFileItem[items.size()]));
                    model.unshareFriend(SharingTarget.GNUTELLA_SHARE);   
                }
            }
        }
    }
    
    /**
     * Action when P2P Document sharing is disabled.
     */
    private class DisabledDocumentAction extends AbstractAction {
        public DisabledDocumentAction() {
            putValue(Action.NAME, I18n.tr("You can't share documents with the P2P Network. Enable this at Tools > Options > Security > Unsafe Categories"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
        }
    }
}
