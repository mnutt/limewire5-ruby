package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;

/**
 * BitTorrent settings.
 */
public class BittorrentSettings extends LimeProps {

    private BittorrentSettings() {
        // empty constructor
    }

    public static final BooleanSetting LIBTORRENT_ENABLED = FACTORY.createBooleanSetting(
            "LIBTORRENT_ENABLED", true);

    // TODO integrate with DownloadSettings.DOWNLOAD_SPEED
    public static final IntSetting LIBTORRENT_DOWNLOAD_SPEED = FACTORY.createIntSetting(
            "LIBTORRENT_DOWNLOAD_SPEED", 100);

    // TODO integrate with DownloadSettings.UPLOAD_SPEED
    public static final IntSetting LIBTORRENT_UPLOAD_SPEED = FACTORY.createIntSetting(
            "LIBTORRENT_UPLOAD_SPEED", 100);
}
