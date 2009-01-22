package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.friends.chat.ChatFriendListPane;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/** Starts a chat with the selected user in the left panel if possible. */
public class ChatAction extends AbstractAction {

    private Friend friend;
    private final ChatFriendListPane friendsPane;
    private final ChatFramePanel friendsPanel;

    @Inject
    ChatAction(ChatFriendListPane friendsPane, ChatFramePanel friendsPanel) {
        // TODO fberger
        // super(I18n.tr("&Chat"));
        super(I18n.tr("Chat"));
        this.friendsPane = friendsPane;
        this.friendsPanel = friendsPanel;
        setEnabled(false);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Friend friend = getFriend();
        if (friend != null) {
            friendsPanel.setChatPanelVisible(true);
            friendsPane.fireConversationStarted(friend.getId());
        }
    }

    /** Sets a new friend for this action and updates the enabledness. */
    public void setFriend(Friend friend) {
        this.friend = friend;
        setEnabled(friend != null && !friend.isAnonymous() && !friend.getFriendPresences().isEmpty());
    }

    protected Friend getFriend() {
        return friend;
    }
}