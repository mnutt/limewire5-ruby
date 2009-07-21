package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;

/**
 * Settings for friends.
 */
public class FriendSettings extends LimeProps {

    private FriendSettings() {}

    /**
     * This setting tracks whether or not the user should be in do not disturb mode. 
     * It should be remembered across sessions.
     */
    public static final BooleanSetting DO_NOT_DISTURB =
        (BooleanSetting)FACTORY.createBooleanSetting("XMPP_DO_NOT_DISTURB", false).setPrivate(true);
}
