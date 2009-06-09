package org.limewire.ui.swing.search;

import javax.swing.SwingUtilities;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.inject.LazySingleton;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.friends.chat.ChatFrame;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.Navigator;

import com.google.inject.Inject;

@LazySingleton
public class RemoteHostActionsImpl implements RemoteHostActions {
    private static final Log LOG = LogFactory.getLog(RemoteHostActionsImpl.class);

    private final RemoteLibraryManager remoteLibraryManager;

    private final ChatFrame chatFrame;
    private final LibraryNavigator libraryNavigator;

    @Inject
    public RemoteHostActionsImpl(RemoteLibraryManager remoteLibraryManager,
            ChatFrame chatFrame,
            LibraryNavigator libraryNavigator, Navigator navigator) {
        this.remoteLibraryManager = remoteLibraryManager;
        this.chatFrame = chatFrame;
        this.libraryNavigator = libraryNavigator;
    }

    @Override
    public void chatWith(RemoteHost person) {
        LOG.debugf("chatWith: {0}", person.getFriendPresence().getFriend());
        Friend friend = person.getFriendPresence().getFriend();
        chatFrame.setVisibility(true);
        chatFrame.fireConversationStarted(friend.getId());

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
