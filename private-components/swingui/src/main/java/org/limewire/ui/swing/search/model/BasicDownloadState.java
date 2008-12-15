package org.limewire.ui.swing.search.model;

/**
 * This differs from the DownloadState enum
 * which doesn't have a value equivalent to NOT_STARTED and
 * has many values that represent states between DOWNLOADING and DOWNLOADED.
 * @author R. Mark Volkmann
 */
public enum BasicDownloadState {
    NOT_STARTED,
    DOWNLOADING,
    DOWNLOADED,
    LIBRARY
}