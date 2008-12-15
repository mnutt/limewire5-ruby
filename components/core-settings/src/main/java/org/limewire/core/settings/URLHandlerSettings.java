package org.limewire.core.settings;

import org.limewire.setting.StringSetting;

/**
 * Settings for programs LimeWire should open to view files on unix.
 */
public final class URLHandlerSettings extends LimeProps {
    
    private URLHandlerSettings() {}

	/**
     * Setting for which browser to use
	 */
	public static final StringSetting BROWSER =
        FACTORY.createStringSetting("BROWSER", "mozilla $URL$");

	/**
     * Setting for which movie player to use
	 */
	public static final StringSetting VIDEO_PLAYER =
		FACTORY.createStringSetting("VIDEO_PLAYER", "xine $URL$");

	/**
     * Setting for which image viewer to use
	 */
	public static final StringSetting IMAGE_VIEWER =
		FACTORY.createStringSetting("IMAGE_VIEWER", "mozilla $URL$");

	/**
     * Setting for which audio player to use
	 */
	public static final StringSetting AUDIO_PLAYER =
		FACTORY.createStringSetting("AUDIO_PLAYER", "xine $URL$");
	
}

