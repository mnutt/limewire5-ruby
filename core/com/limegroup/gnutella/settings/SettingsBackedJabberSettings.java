package com.limegroup.gnutella.settings;

import org.limewire.core.settings.XMPPSettings;
import org.limewire.xmpp.api.client.JabberSettings;

import com.google.inject.Singleton;

@Singleton
public class SettingsBackedJabberSettings implements JabberSettings {

    @Override
    public boolean isDoNotDisturbSet() {
        return XMPPSettings.XMPP_DO_NOT_DISTURB.getValue();
    }

}
