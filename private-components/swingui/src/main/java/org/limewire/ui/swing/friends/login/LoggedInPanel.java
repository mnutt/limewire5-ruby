package org.limewire.ui.swing.friends.login;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.VerticalLayout;
import org.limewire.core.settings.XMPPSettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.StatusActions;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.friends.AddFriendDialog;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.LanguageUtils;
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

    @Resource private Font headingFont;
    @Resource private Font itemFont;
    @Resource private Color headingColor;
    @Resource private Color itemColor;
    @Resource private Icon offlineIcon;
    @Resource private Icon availableIcon;
    @Resource private Icon doNotDisturbIcon;
    @Resource private Icon awayIcon;

    @Inject
    LoggedInPanel(ComboBoxDecorator comboDecorator,
            FriendActions friendActions, BarPainterFactory barPainterFactory,
            ButtonDecorator buttonDecorator,
            StatusActions statusActions, XMPPService xmppService) {
        GuiUtils.assignResources(this);
        setLayout(new MigLayout("insets 0, gapx 8:8:8, hidemode 3, fill"));

        this.friendActions = friendActions;
        optionsBox = new LimeComboBox();
        comboDecorator.decorateMiniComboBox(optionsBox);
        signoutBox = new LimeComboBox();
        comboDecorator.decorateMiniComboBox(signoutBox);
        statusMenuLabel = new JLabel();
        currentUser = new JLabel(offlineIcon);
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
        optionsMenu.setLayout(new VerticalLayout());
        optionsMenu.add(decorateItem(optionsBox.createMenuItem(new AbstractAction(I18n.tr("Add Friend")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new AddFriendDialog(LoggedInPanel.this,
                        xmppService.getActiveConnection());
            }
        })));
        
        
        final JCheckBoxMenuItem showOfflineFriends = new JCheckBoxMenuItem();
        showOfflineFriends.setSelected(SwingUiSettings.XMPP_SHOW_OFFLINE.getValue());
        showOfflineFriends.setAction(new AbstractAction(I18n.tr("Offline Friends")) {
            {
                putValue(Action.SMALL_ICON, offlineIcon);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUiSettings.XMPP_SHOW_OFFLINE.setValue(showOfflineFriends.isSelected());
            }
        });
        
        optionsMenu.add(decorateHeading(new JLabel(I18n.tr("SHOW"))));
        optionsMenu.add(decorateItem(showOfflineFriends));
        optionsMenu.add(decorateHeading(statusMenuLabel));
        optionsMenu.add(decorateItem(statusActions.getAvailableMenuItem()));
        optionsMenu.add(decorateItem(statusActions.getDnDMenuItem()));
        optionsBox.overrideMenu(optionsMenu);
        optionsBox.setText(I18n.tr("Options"));

        signoutBox.setText(I18n.tr("Sign Out"));
        JPopupMenu signoutMenu = new JPopupMenu();
        signoutMenu.add(decorateItem(new JMenuItem(new AbstractAction(I18n.tr("Switch User")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendActions.signOut(true);
            }
        })));
        signoutMenu.add(decorateItem(new JMenuItem(new AbstractAction(I18n.tr("Sign Out")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendActions.signOut(false);
            }
        })));
        signoutBox.overrideMenu(signoutMenu);

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
                    currentUser.setIcon(XMPPSettings.XMPP_DO_NOT_DISTURB.getValue() ? doNotDisturbIcon : availableIcon);
                    break;
                case CONNECT_FAILED:
                case DISCONNECTED:
                    currentUser.setIcon(offlineIcon);
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
                        currentUser.setIcon(XMPPSettings.XMPP_DO_NOT_DISTURB.getValue() ? doNotDisturbIcon : availableIcon);
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
                    currentUser.setIcon(awayIcon);
                    break;
                case Active:
                    currentUser.setIcon(XMPPSettings.XMPP_DO_NOT_DISTURB.getValue() ? doNotDisturbIcon : availableIcon);
                }
            }
        });
    }

    private void setConfig(XMPPConnectionConfiguration config) {
        if(config != null) {
            currentUser.setText(config.getUserInputLocalID());
            statusMenuLabel.setText(I18n.tr("SET {0} STATUS", config.getLabel().toUpperCase(LanguageUtils.getCurrentLocale())));
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

    private JComponent decorateHeading(JComponent component){
        component.setForeground(headingColor);
        component.setFont(headingFont);
        return component;
    }
    
  private JComponent decorateItem(JComponent component) {
        component.setForeground(itemColor);
        component.setFont(itemFont);
        return component;
    }
}
