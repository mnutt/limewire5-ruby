package org.limewire.bittorrent;

public interface TorrentAlert {

    /**
     * Category number of a SAVE_RESUME_DATE_ALERT
     */
    public final static int SAVE_RESUME_DATA_ALERT = 8;

    /**
     * Returns the category for this alert.
     */
    public int getCategory();

    /**
     * Returns the sha1 associated with this alert. will return null or empty
     * string if the alert is not associated with a torrent. but is a more
     * general alert.
     */
    public String getSha1();

    /**
     * Returns a message describing what this alert relates to.
     */
    public String getMessage();

}