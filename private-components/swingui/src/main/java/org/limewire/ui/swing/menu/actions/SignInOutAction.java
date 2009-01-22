package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.login.FriendActions;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Inject;

public class SignInOutAction extends AbstractAction {
    
    
    private static final String SIGN_INTO_FRIENDS_TEXT = I18n.tr("&Sign into Friends");
    
    private static final String SIGN_OUT_OF_FRIENDS_TEXT = I18n.tr("&Sign out of Friends");
    
    private final FriendActions friendActions;

    @Inject SignInOutAction(FriendActions friendActions) {
        super(SIGN_INTO_FRIENDS_TEXT);
        this.friendActions = friendActions;
    }
    
    @Inject void register(FriendActions actions, ListenerSupport<XMPPConnectionEvent> event) {
        event.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTED:
                case CONNECTING:
                    putValue(Action.NAME, SIGN_OUT_OF_FRIENDS_TEXT);
                    break;                    
                case CONNECT_FAILED:
                case DISCONNECTED:
                    putValue(Action.NAME, SIGN_INTO_FRIENDS_TEXT);
                    break;
                }
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!friendActions.isSignedIn()) {
            friendActions.signIn();
        } else {
            friendActions.signOut(false);
        }
    }
}