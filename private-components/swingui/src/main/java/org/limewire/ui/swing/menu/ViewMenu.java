package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventUtils;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.friends.chat.ChatFrame;
import org.limewire.ui.swing.friends.login.AutoLoginService;
import org.limewire.ui.swing.friends.login.LoginPopupPanel;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.VisibilityType;
import org.limewire.ui.swing.util.VisibleComponent;

import com.google.inject.Inject;
import com.google.inject.Provider;

class ViewMenu extends MnemonicMenu {
    private final Provider<LoginPopupPanel> friendsSignInPanel;
    private final Provider<AutoLoginService> autoLoginServiceProvider;
    private final EventBean<FriendConnectionEvent> friendConnectionEventBean;
    
    @Inject
    public ViewMenu(final ChatFrame chatFrame, Provider<LoginPopupPanel> friendsSignInPanel, 
            Provider<AutoLoginService> autoLoginServiceProvider,
            EventBean<FriendConnectionEvent> friendConnectionEventBean, ShowHideDownloadTrayAction showHideDownloadTrayAction) {
        super(I18n.tr("&View"));
        this.friendsSignInPanel = friendsSignInPanel;
        this.autoLoginServiceProvider = autoLoginServiceProvider;
        this.friendConnectionEventBean = friendConnectionEventBean;
        add(buildShowHideChatWindowAction(chatFrame, I18n.tr("Hide &Chat Window"), I18n.tr("Show &Chat Window")));
        add(showHideDownloadTrayAction);
    }
    /**
     * @return if there is a connection that is either logged in, logging in or
     * aut login service provider is attempting to log in.
     */
    private boolean hasActiveConnection() {
        if (autoLoginServiceProvider.get().isAttemptingLogin()) {
            return true;
        }
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        if (friendConnection != null) {
            return friendConnection.isLoggedIn() || friendConnection.isLoggingIn();
        }
        return false;
    }
    
    private boolean isLoggingIn() {
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        return friendConnection != null && friendConnection.isLoggingIn();
    }

    private Action buildShowHideChatWindowAction(final ChatFrame component, final String visibleName,
            final String notVisibleName) {
        final Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!component.isVisible() && !hasActiveConnection()) {
                        friendsSignInPanel.get().setVisible(true);
                } else {
                    // TODO: nothing happens if we are logging in, seems strange.
                    if (!autoLoginServiceProvider.get().isAttemptingLogin() && !isLoggingIn()) {
                        component.toggleVisibility();
                    }
                }
                
            }
        };

        addVisibilityListener(component, action, visibleName, notVisibleName);
        setInitialText(component, action, visibleName, notVisibleName);

        return action;
    }

    private void addVisibilityListener(VisibleComponent component, final Action action,
            final String visibleName, final String notVisibleName) {
        EventListener<VisibilityType> listener = new EventListener<VisibilityType>() {
            @Override
            public void handleEvent(VisibilityType visibilityType) {
                if (visibilityType == VisibilityType.VISIBLE) {
                    action.putValue(Action.NAME, visibleName);
                } else {
                    action.putValue(Action.NAME, notVisibleName);
                }
            }
        };
        component.addVisibilityListener(listener);
    }

    private void setInitialText(VisibleComponent component, final Action action,
            final String visibleName, final String notVisibleName) {
        if (component.isVisible()) {
            action.putValue(Action.NAME, visibleName);
        } else {
            action.putValue(Action.NAME, notVisibleName);
        }
    }
}
