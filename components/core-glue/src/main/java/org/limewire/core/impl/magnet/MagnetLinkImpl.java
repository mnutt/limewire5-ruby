package org.limewire.core.impl.magnet;

import org.limewire.core.api.magnet.MagnetLink;

import com.limegroup.gnutella.browser.MagnetOptions;

public class MagnetLinkImpl implements MagnetLink {
    private final MagnetOptions magnetOptions;
    public MagnetLinkImpl(MagnetOptions magnetOptions) {
        this.magnetOptions = magnetOptions;
    }

    public boolean isDownloadable() {
        return magnetOptions.isDownloadable();
    }

    public boolean isKeywordTopicOnly() {
        return magnetOptions.isKeywordTopicOnly();
    }
    
    public MagnetOptions getMagnetOptions() {
        return magnetOptions;
    }
    
    public String getQueryString() {
        return magnetOptions.getQueryString();
    }
}
