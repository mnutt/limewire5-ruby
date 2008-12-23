package org.limewire.ui.swing.properties;

/**
 * Controls addition of IP addresses to the block list to prevent
 * IPs from being sources for download.
 */
public interface FilterList {
    void addIPToFilter(String ipAddress);
}
