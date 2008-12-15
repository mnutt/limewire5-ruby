package com.limegroup.gnutella.version;

import java.util.List;

import org.limewire.util.Version;

/**
 * An abstraction for the update XML.
 * Contains the ID & timestamp of the message, as well as the list
 * of UpdateData information for individual messages.
 */
public interface UpdateCollection {
    
    /**
     * Gets the id of this UpdateCollection.
     */
    public int getId();

    /**
     * Gets the timestamp.
     */
    public long getTimestamp();

    /**
     * Gets the UpdateData objects.
     */
    public List<UpdateData> getUpdateData();

    /**
     * Gets all updates that have information so we can download them.
     */
    public List<DownloadInformation> getUpdatesWithDownloadInformation();

    /**
     * Gets the UpdateData that is relevant to us.
     * Returns null if there is no relevant update.
     */
    public UpdateData getUpdateDataFor(Version currentV, String lang, boolean currentPro,
            int currentStyle, Version currentJava);    
}