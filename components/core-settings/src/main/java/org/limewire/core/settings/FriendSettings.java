package org.limewire.core.settings;

import org.limewire.setting.IntSetting;

/**
 * Settings for Instant Messenger.
 */
public class FriendSettings extends LimeProps {

    private FriendSettings(){}
    
    /**
     * Setting for the number of times a user has logged into the chat
     */
    public static final IntSetting NUM_LOGINS =
        FACTORY.createIntSetting("NUM_LOGINS", 0);
}
