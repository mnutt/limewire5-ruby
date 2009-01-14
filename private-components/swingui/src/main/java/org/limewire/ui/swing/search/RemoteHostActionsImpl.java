package org.limewire.ui.swing.search;

import javax.swing.SwingUtilities;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.friends.chat.ChatFriendListPane;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.Navigator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RemoteHostActionsImpl implements RemoteHostActions {
    private static final Log LOG = LogFactory.getLog(RemoteHostActionsImpl.class);

    private final RemoteLibraryManager remoteLibraryManager;

    private final ChatFramePanel friendsPanel;
    private final ChatFriendListPane friendsPane;

    private final LibraryNavigator libraryNavigator;

    @Inject
    public RemoteHostActionsImpl(RemoteLibraryManager remoteLibraryManager,
            ChatFriendListPane friendsPane,
            ChatFramePanel friendsPanel,
            LibraryNavigator libraryNavigator, Navigator navigator) {
        this.remoteLibraryManager = remoteLibraryManager;
        this.friendsPane = friendsPane;
        this.friendsPanel = friendsPanel;
        this.libraryNavigator = libraryNavigator;
    }

    @Override
    public void chatWith(RemoteHost person) {
        LOG.debugf("chatWith: {0}", person.getFriendPresence().getFriend());
        Friend friend = person.getFriendPresence().getFriend();
        friendsPanel.setChatPanelVisible(true);
        friendsPane.fireConversationStarted(friend.getId());

        // TODO make sure the input box for chat gets focus, the code is
        // calling requestFocusInWindow, but I think it is gettting some
        // weirdness because the search window is currently the active one, not
        // the chat
    }

    @Override
    public void showFilesSharedBy(RemoteHost person) {
        LOG.debugf("showFilesSharedBy: {0}", person.getFriendPresence().getFriend());
        Friend friend = person.getFriendPresence().getFriend();
        libraryNavigator.selectFriendShareList(friend);
    }

    @Override
    public void viewLibraryOf(final RemoteHost person) {
        LOG.debugf("viewLibraryOf: {0}", person.getFriendPresence().getFriend());
        remoteLibraryManager.addPresenceLibrary(person.getFriendPresence());
        // Run this later, to allow the library a bit of time to render the friend.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
				libraryNavigator.selectFriendLibrary(person.getFriendPresence().getFriend());
            }
        });
    }
}
