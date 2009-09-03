package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.friends.chat.ChatMediator;
import org.limewire.ui.swing.friends.login.AutoLoginService;
import org.limewire.ui.swing.friends.login.LoginPopupPanel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class ViewMenu extends MnemonicMenu {
    
    private static final String visibleText = I18n.tr("Hide &Chat Window");
    private static final String notVisibleText = I18n.tr("Show &Chat Window");
    
    private final Provider<LoginPopupPanel> friendsSignInPanelProvider;
    private final Provider<AutoLoginService> autoLoginServiceProvider;
    private final EventBean<FriendConnectionEvent> friendConnectionEventBean;
    
    private final Provider<ShowHideDownloadTrayAction> showHideDownloadTrayActionProvider;
    private final Provider<ChatMediator> chatFrameProvider;
    
    @Inject
    public ViewMenu(Provider<LoginPopupPanel> friendsSignInPanel, 
            Provider<AutoLoginService> autoLoginServiceProvider,
            EventBean<FriendConnectionEvent> friendConnectionEventBean,
            Provider<ShowHideDownloadTrayAction> showHideDownloadTrayAction,
            Provider<ChatMediator> chatFrameProvider) {
        
        super(I18n.tr("&View"));
        
        this.friendsSignInPanelProvider = friendsSignInPanel;
        this.autoLoginServiceProvider = autoLoginServiceProvider;
        this.friendConnectionEventBean = friendConnectionEventBean;
        
        this.showHideDownloadTrayActionProvider = showHideDownloadTrayAction;
        this.chatFrameProvider = chatFrameProvider;
    }
    
    @Override
    public void createMenuItems() {
        add(buildShowHideChatWindowAction(chatFrameProvider));
        add(showHideDownloadTrayActionProvider.get());
    }
    
    /**
     * @return if there is a connection that is either logged in, logging in or
     * a login service provider is attempting to log in.
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

    private Action buildShowHideChatWindowAction(final Provider<ChatMediator> chatFrameProvider) {
        
        Action action = new AbstractAction(chatFrameProvider.get().isVisible() ? visibleText : notVisibleText) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!chatFrameProvider.get().isVisible() && !hasActiveConnection()) {
                        friendsSignInPanelProvider.get().setVisible(true);
                } else {
                    // TODO: nothing happens if we are logging in, seems strange.
                    if (!autoLoginServiceProvider.get().isAttemptingLogin() && !isLoggingIn()) {
                        chatFrameProvider.get().setVisible(!chatFrameProvider.get().isVisible());
                    }
                }
                
            }
        };
        
        return action;
    }
}
