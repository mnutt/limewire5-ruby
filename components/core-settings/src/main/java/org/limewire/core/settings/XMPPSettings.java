package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;

/**
 * Settings for XMPP: a list of servers and the label and username of the
 * auto-login account, if there is one (the password can be retrieved from
 * PasswordManager).
 */
public class XMPPSettings extends LimeProps {

    private XMPPSettings() {}

    /**
     * This setting tracks whether or not the user should be in do not disturb mode. 
     * It should be remembered across xmpp sessions.
     */
    public static final BooleanSetting XMPP_DO_NOT_DISTURB =
        (BooleanSetting)FACTORY.createBooleanSetting("XMPP_DO_NOT_DISTURB", false).setPrivate(true);
}
