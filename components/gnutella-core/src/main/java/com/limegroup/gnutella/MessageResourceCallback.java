package com.limegroup.gnutella;

/** Front-End delegate for the Back-End to obtain internationalized Strings.
 */
public interface MessageResourceCallback {

    // strings for the html page
    // -------------------------
    public String getHTMLPageTitle();
    public String getHTMLPageListingHeader();
    public String getHTMLPageMagnetHeader();
    // -------------------------
    

}
