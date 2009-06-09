package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.chat.ChatFrame;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/** Starts a chat with the selected user in the left panel if possible. */
public class ChatAction extends AbstractAction {

    private Friend friend;
    private final ChatFrame chatFrame;

    @Inject
    ChatAction(ChatFrame chatFrame) {
        super(I18n.tr("&Chat"));
        this.chatFrame = chatFrame;

        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Friend friend = getFriend();
        if (friend != null) {
            chatFrame.setVisibility(true);
            chatFrame.fireConversationStarted(friend.getId());
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