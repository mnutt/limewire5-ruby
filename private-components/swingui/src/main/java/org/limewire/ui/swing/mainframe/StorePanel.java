package org.limewire.ui.swing.mainframe;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.limewire.core.api.Application;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.mozilla.browser.MozillaAutomation;
import org.mozilla.browser.MozillaInitialization;
import org.mozilla.browser.MozillaPanel.VisibilityMode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StorePanel extends JPanel {
    public static final String NAME = "LimeWire Store";

    private final Browser browser;
    private final Application application;

    @Inject
    public StorePanel(Application application) {
        this.application = application;
        browser = new Browser(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN, VisibilityMode.DEFAULT);

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(browser, gbc);
    }
    
    public void loadDefaultUrl() {
        load("http://store.limewire.com/");
    }

    public void load(String url) {
        url = application.getUniqueUrl(url);
        if(!MozillaInitialization.isInitialized()) {
            NativeLaunchUtils.openURL(url);
        } else {
            // Reset the page to blank before continuing -- blocking is OK because this is fast.
            MozillaAutomation.blockingLoad(browser, "about:blank");
            browser.load(url + "&isClient=true");
        }
    }
}
