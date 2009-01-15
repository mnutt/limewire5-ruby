package org.limewire.ui.swing.library.nav;

import javax.swing.SwingUtilities;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.settings.SwingUiSettings;

/**
 * Overrides checkVisibility from NavList to setVisibility based on the
 * XMPPSettings.XMPP_SHOW_OFFLINE setting.
 */
class OfflineNavList extends NavList {

    public OfflineNavList(String name, BooleanSetting collapsedSetting) {
        super(name, collapsedSetting);
        SwingUiSettings.XMPP_SHOW_OFFLINE.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        checkVisibility(SwingUiSettings.XMPP_SHOW_OFFLINE.getValue());                        
                    }
                });
            }
        });
    }

    /**
     * Overriding checkVisibility from NavList. canDisplay paramter is ignored.
     * Instead the value from the MPPSettings.XMPP_SHOW_OFFLINE setting is used.
     */
    @Override
    protected void checkVisibility(boolean canDisplay) {
        super.checkVisibility(SwingUiSettings.XMPP_SHOW_OFFLINE.getValue());
    }

}
