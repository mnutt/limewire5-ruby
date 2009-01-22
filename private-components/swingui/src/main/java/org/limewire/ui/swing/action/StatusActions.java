package org.limewire.ui.swing.action;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import org.limewire.core.settings.XMPPSettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.friends.chat.IconLibrary;
import org.limewire.ui.swing.friends.login.FriendActions;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.api.client.Presence.Mode;

import com.google.inject.Inject;

/**
 * Provides JMenuItems to be used for setting available or disabled status for
 * the users. These items are backed by a button group and JCheckBoxMenuItems
 */
public class StatusActions {

    private final JCheckBoxMenuItem available;

    private final JCheckBoxMenuItem dnd;
    
    private final XMPPService xmppService;

    @Inject
    public StatusActions(final XMPPService xmppService, final IconLibrary iconLibrary) {
        this.xmppService = xmppService;
        
        available = new JCheckBoxMenuItem(new AbstractAction(I18n.tr("&Available")) {
            { 
                putValue(Action.SMALL_ICON, iconLibrary.getAvailable());
                setEnabled(false);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                xmppService.setMode(Mode.available);
                XMPPSettings.XMPP_DO_NOT_DISTURB.setValue(false);
            }
        });

        
        dnd = new JCheckBoxMenuItem(new AbstractAction(I18n.tr("&Do Not Disturb")) {
            {
                putValue(Action.SMALL_ICON, iconLibrary.getDoNotDisturb());
                setEnabled(false);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                xmppService.setMode(Mode.dnd);
                XMPPSettings.XMPP_DO_NOT_DISTURB.setValue(true);
            }
        });
        
        
        updateSelection();
        
        XMPPSettings.XMPP_DO_NOT_DISTURB.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateSelection();
                    }
                });
            }
        });
    }

    private void updateSelection() {
        if(xmppService.isLoggedIn()) {
            boolean dndBool = XMPPSettings.XMPP_DO_NOT_DISTURB.getValue();
            available.setSelected(!dndBool);
            dnd.setSelected(dndBool);
        } else {
            //do not show selections when logged out
            available.setSelected(false);
            dnd.setSelected(false);
        }
    }

    @Inject
    void register(FriendActions actions, ListenerSupport<XMPPConnectionEvent> event) {
        event.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch (event.getType()) {
                case CONNECTED:
                case CONNECTING:
                    available.setEnabled(true);
                    dnd.setEnabled(true);
                    updateSelection();
                    break;
                case CONNECT_FAILED:
                case DISCONNECTED:
                    available.setEnabled(false);
                    dnd.setEnabled(false);
                    updateSelection();
                    break;
                }
            }
        });
    }

    public JMenuItem getAvailableMenuItem() {
        return available;
    }

    public JMenuItem getDnDMenuItem() {
        return dnd;
    }
}
