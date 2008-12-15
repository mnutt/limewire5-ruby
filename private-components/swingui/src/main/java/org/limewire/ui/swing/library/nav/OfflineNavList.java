package org.limewire.ui.swing.library.nav;

import org.limewire.core.settings.XMPPSettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;

/**
 * Overrides checkVisibility from NavList to setVisibility based on the
 * XMPPSettings.XMPP_SHOW_OFFLINE setting.
 */
class OfflineNavList extends NavList {

    public OfflineNavList() {
        XMPPSettings.XMPP_SHOW_OFFLINE.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                checkVisibility(XMPPSettings.XMPP_SHOW_OFFLINE.getValue());
            }
        });
    }

    /**
     * Overriding checkVisibility from NavList. canDisplay paramter is ignored.
     * Instead the value from the MPPSettings.XMPP_SHOW_OFFLINE setting is used.
     */
    @Override
    protected void checkVisibility(boolean canDisplay) {
        super.checkVisibility(XMPPSettings.XMPP_SHOW_OFFLINE.getValue());
    }

}
