package org.limewire.ui.swing.friends.login;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.settings.XMPPSettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.StatusActions;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.LimeComboBoxFactory;
import org.limewire.ui.swing.friends.AddFriendDialog;
import org.limewire.ui.swing.friends.chat.IconLibrary;
import org.limewire.ui.swing.painter.BarPainterFactory;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.xmpp.activity.XmppActivityEvent;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;

class LoggedInPanel extends JXPanel {

    /**
     * The currentUser variable is used to display the logged in user. Also the users status is displayed as an icon set in the label.
     */
    private final JLabel currentUser;
    private final JLabel loggingInLabel;
    private final JLabel statusMenuLabel;
    private final JXButton signInButton;
    private final JXButton switchUserButton;
    private final LimeComboBox optionsBox;
    private final LimeComboBox signoutBox;
    private final FriendActions friendActions;
    private final IconLibrary iconLibrary;

    @Inject
    LoggedInPanel(LimeComboBoxFactory comboFactory,
            FriendActions friendActions, BarPainterFactory barPainterFactory,
            ButtonDecorator buttonDecorator,
            StatusActions statusActions, XMPPService xmppService, IconLibrary iconLibrary) {
        GuiUtils.assignResources(this);
        setLayout(new MigLayout("insets 0, gap 0, hidemode 3, fill"));

        this.friendActions = friendActions;
        this.iconLibrary = iconLibrary;
        optionsBox = comboFactory.createMiniComboBox();
        signoutBox = comboFactory.createMiniComboBox();
        statusMenuLabel = new JLabel();        
        currentUser = new JLabel(iconLibrary.getEndChat());
        loggingInLabel = new JLabel(I18n.tr("Signing in..."));
        signInButton = new JXButton();
        buttonDecorator.decorateMiniButton(signInButton);
        switchUserButton = new JXButton();
        buttonDecorator.decorateMiniButton(switchUserButton);
        setBackgroundPainter(barPainterFactory.createFriendsBarPainter()); 

        initComponents(statusActions, xmppService);
    }

    private void initComponents(final StatusActions statusActions,
                                final XMPPService xmppService) {
        JPopupMenu optionsMenu = new JPopupMenu(); 
        optionsMenu.add(optionsBox.createMenuItem(new AbstractAction(I18n.tr("Add Friend")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new AddFriendDialog(LoggedInPanel.this,
                        xmppService.getActiveConnection());
            }
        }));
        optionsMenu.addSeparator();
        optionsMenu.add(optionsBox.decorateMenuComponent(new JLabel(I18n.tr("Show:"))));
        final JCheckBoxMenuItem showOfflineFriends = new JCheckBoxMenuItem(I18n.tr("Offline Friends"));
        showOfflineFriends.setSelected(SwingUiSettings.XMPP_SHOW_OFFLINE.getValue());
        showOfflineFriends.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUiSettings.XMPP_SHOW_OFFLINE.setValue(showOfflineFriends.isSelected());
            } 
        });

        optionsMenu.add(optionsBox.decorateMenuComponent(showOfflineFriends));
        optionsMenu.addSeparator();
        optionsMenu.add(optionsBox.decorateMenuComponent(statusMenuLabel));
        optionsMenu.add(optionsBox.decorateMenuComponent(statusActions.getAvailableMenuItem()));
        optionsMenu.add(optionsBox.decorateMenuComponent(statusActions.getDnDMenuItem()));
        optionsBox.overrideMenu(optionsMenu);
        optionsBox.setText(I18n.tr("Options"));

        signoutBox.setText(I18n.tr("Sign Out"));
        signoutBox.addAction(new AbstractAction(I18n.tr("Switch User")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendActions.signOut(true);
            }
        });
        signoutBox.addAction(new AbstractAction(I18n.tr("Sign Out")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendActions.signOut(false);
            }
        });

        signInButton.setAction(new AbstractAction(I18n.tr("Sign In")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendActions.signIn();
            }
        });

        switchUserButton.setAction(new AbstractAction(I18n.tr("Switch User")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendActions.signOut(true);
            }
        });

        add(currentUser, "gapleft 9, gaptop 2, wmin 0, wrap");

        add(optionsBox, "gapleft 2, alignx left, gapbottom 2, split");
        add(signoutBox, "gapleft push, gapbottom 2, gapright 2, wrap");

        add(signInButton, "gapleft 2, alignx left, gapbottom 2, split");
        add(switchUserButton, "gapleft push, gapbottom 2, gapright 2, wrap");

        add(loggingInLabel, "alignx left, gapleft 9, gaptop 2, gapbottom 2");

        optionsBox.setVisible(false);
        signoutBox.setVisible(false);
        signInButton.setVisible(false);
        switchUserButton.setVisible(false);
        loggingInLabel.setVisible(false);
    }
    
    /**
     * Registers this panel with xmpp connection events as well as changes to the XMPP_DO_NO_DISTURB setting.
     * It uses these events to keep the status icon for the user up to date. 
     */
    @Inject
    void register(FriendActions actions, ListenerSupport<XMPPConnectionEvent> event) {
        event.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch (event.getType()) {
                case CONNECTED:
                case CONNECTING:
                    currentUser.setIcon(XMPPSettings.XMPP_DO_NOT_DISTURB.getValue() ? iconLibrary.getDoNotDisturb() : iconLibrary.getAvailable());
                    break;
                case CONNECT_FAILED:
                case DISCONNECTED:
                    currentUser.setIcon(iconLibrary.getEndChat());
                    break;
                }
            }
        });
        
        /**
         * TODO - Using a setting listener is really not the correct approach here. We need to build up the capability to get status
         * changes as events coming from the core xmpp code.
         */
        XMPPSettings.XMPP_DO_NOT_DISTURB.addSettingListener(new SettingListener() {
           @Override
            public void settingChanged(SettingEvent evt) {
               SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        currentUser.setIcon(XMPPSettings.XMPP_DO_NOT_DISTURB.getValue() ? iconLibrary.getDoNotDisturb() : iconLibrary.getAvailable());
                    } 
               });
            } 
        });
    }
    
    /**
     * Updates the connection status icon based on ActivityEvent messages
     * indicating the current availability of the user
     * @param listenerSupport
     */
    @Inject
    void register(ListenerSupport<XmppActivityEvent> listenerSupport) {
        listenerSupport.addListener(new EventListener<XmppActivityEvent>() {
            @SwingEDTEvent
            @Override
            public void handleEvent(XmppActivityEvent event) {
                switch(event.getSource()) {
                case Idle:
                    currentUser.setIcon(iconLibrary.getAway());
                    break;
                case Active:
                    currentUser.setIcon(XMPPSettings.XMPP_DO_NOT_DISTURB.getValue() ? iconLibrary.getDoNotDisturb() : iconLibrary.getAvailable());
                }
            }
        });
    }

    private void setConfig(XMPPConnectionConfiguration config) {
        if(config != null) {
            currentUser.setText(config.getUserInputLocalID());
            statusMenuLabel.setText(I18n.tr("Set {0} Status:", config.getLabel()));
        }
    }

    void autoLogin(XMPPConnectionConfiguration config) {
        setConfig(config);
        optionsBox.setVisible(false);
        signoutBox.setVisible(false);
        signInButton.setVisible(false);
        switchUserButton.setVisible(false);
        loggingInLabel.setVisible(true);
    }

    void connected(XMPPConnectionConfiguration config) {
        setConfig(config);
        optionsBox.setVisible(true);
        signoutBox.setVisible(true);
        signInButton.setVisible(false);
        switchUserButton.setVisible(false);
        loggingInLabel.setVisible(false);
    }

    void disconnected(XMPPConnectionConfiguration config) {
        setConfig(config);
        optionsBox.setVisible(false);
        signoutBox.setVisible(false);
        signInButton.setVisible(true);
        switchUserButton.setVisible(true);
        loggingInLabel.setVisible(false);
    }

    void connecting(XMPPConnectionConfiguration config) {
        autoLogin(config);
    }

}
