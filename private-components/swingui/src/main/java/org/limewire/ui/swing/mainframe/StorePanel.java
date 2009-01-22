package org.limewire.ui.swing.mainframe;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;

import org.limewire.core.api.Application;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.browser.BrowserUtils;
import org.limewire.ui.swing.browser.TargetedUrlAction;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;
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
    public StorePanel(Application application, final Navigator navigator) {
        this.application = application;
        browser = new Browser(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN, VisibilityMode.DEFAULT);

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(browser, gbc);
        
        // Hide the page when the browser goes away.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                if(MozillaInitialization.isInitialized()) {
                    browser.load("about:blank");
                }
            }
        });
        
        BrowserUtils.addTargetedUrlAction("_lwStore", new TargetedUrlAction() {
            @Override
            public void targettedUrlClicked(final TargetedUrl targetedUrl) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        navigator.getNavItem(NavCategory.LIMEWIRE, NAME).select();
                        load(targetedUrl.getUrl());
                    }
                });
            }
        });        
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
