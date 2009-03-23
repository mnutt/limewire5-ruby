package org.limewire.ui.swing.library.table.menu.actions;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.login.FriendsSignInPanel;
import org.limewire.ui.swing.library.SelectAllable;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SharingActionFactoryImpl implements SharingActionFactory {

    private final ShareListManager shareListManager;
    private final ShareWidgetFactory shareWidgetFactory;
    private final FriendsSignInPanel friendsSignInPanel;
    
    @Inject
    public SharingActionFactoryImpl(ShareListManager shareListManager, ShareWidgetFactory shareWidgetFactory,
            FriendsSignInPanel friendsSignInPanel) {
        this.shareListManager = shareListManager;
        this.shareWidgetFactory = shareWidgetFactory;
        this.friendsSignInPanel = friendsSignInPanel;
    }
    
    @Override
    public AbstractAction createShareFriendAction(boolean isShareAll, SelectAllable<LocalFileItem> librarySelectable) {
        return new ShareFriendAction(shareWidgetFactory, librarySelectable, isShareAll);
    }

    @Override
    public AbstractAction createShareGnutellaAction(boolean isShareAll, SelectAllable<LocalFileItem> librarySelectable) {
        return new ShareGnutellaAction(shareListManager, librarySelectable, isShareAll);
    }

    @Override
    public AbstractAction createUnshareFriendAction(boolean isShareAll, SelectAllable<LocalFileItem> librarySelectable) {
        return new UnshareFriendAction(shareWidgetFactory, librarySelectable, isShareAll);
    }

    @Override
    public AbstractAction createUnshareGnutellaAction(boolean isShareAll, SelectAllable<LocalFileItem> librarySelectable) {
        return new UnshareGnutellaAction(shareListManager, librarySelectable, isShareAll);
    }

    @Override
    public AbstractAction createDisabledFriendAction(String text) {
        return new DisabledFriendLoginAction(text, friendsSignInPanel);
    }
}
