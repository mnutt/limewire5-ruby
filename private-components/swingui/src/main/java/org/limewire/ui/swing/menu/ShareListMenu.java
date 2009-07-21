package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.JSeparator;

import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.library.navigator.CreateListAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ShareListMenu extends MnemonicMenu {

    @Inject
    public ShareListMenu(EventBean<FriendConnectionEvent> friendConnectionEventBean,
            SharedFileListManager shareListManager,
            CreateListAction createListAction,
            final Provider<LibraryMediator> libraryMediatorProvider) {
        
        super(I18n.tr("&Share List"));
        
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        if (friendConnection != null && friendConnection.isLoggedIn()) {
            for ( final SharedFileList list : shareListManager.getModel() ) {
                if (!list.isPublic()) {
                    add(new AbstractAction(I18n.tr(list.getCollectionName())) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            libraryMediatorProvider.get().showSharedFileList(list, true);
                        }
                    });
                }
            }
            add(new JSeparator());
            add(createListAction);
        } 
        else {
            setEnabled(false);
        }
    }

}
