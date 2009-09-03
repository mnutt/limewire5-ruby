package org.limewire.inspection;

/**
 * The type of information contained in an inspection point.
 */
public enum DataCategory {
    /**
     * Data created by 'something the software does.'  This tends to
     * be network related information, but not always.
     */
    NETWORK,

    /**
     * Data created by 'something the user does'.  Users have the ability to
     * opt-out of having this data collected.
     * @see(org.limewire.core.settings.ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING)
     */
    USAGE
}
