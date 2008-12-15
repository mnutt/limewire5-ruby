package org.limewire.core.api.magnet;

public interface MagnetLink {

    public boolean isDownloadable();

    public boolean isKeywordTopicOnly();

    public String getQueryString();

}
