package com.limegroup.gnutella.gui;

import com.limegroup.gnutella.MessageResourceCallback;

/** Simply delegates calls to GUIMediator.getStringResource().
 */
public class MessageResourceHandler implements MessageResourceCallback {

    public String getHTMLPageTitle() {
        return I18n.tr("Download Page");
    }

    public String getHTMLPageListingHeader() {
        return I18n.tr("File Listing for");
    }

    public String getHTMLPageMagnetHeader() {
        return I18n.tr("Magnet Links for Fast Downloads (if you have LimeWire installed)");
    }

}
