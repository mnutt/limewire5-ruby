package org.limewire.ui.swing.browser;

import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import org.mozilla.interfaces.nsIWebBrowserChrome;

/**
 * Removes LimeDomListener from popup browser windows when they close.
 *
 */
class MozillaClosingListener extends WindowAdapter {

    private nsIWebBrowserChrome chrome;

    public MozillaClosingListener(nsIWebBrowserChrome chrome) {
        this.chrome = chrome;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        BrowserUtils.removeDomListener(chrome);
    }

}
