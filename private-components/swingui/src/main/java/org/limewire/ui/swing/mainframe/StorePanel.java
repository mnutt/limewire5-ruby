package org.limewire.ui.swing.mainframe;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;

import org.limewire.core.api.Application;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.browser.BrowserUtils;
import org.limewire.ui.swing.browser.UriAction;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;
import org.mozilla.browser.MozillaAutomation;
import org.mozilla.browser.MozillaInitialization;
import org.mozilla.browser.MozillaPanel.VisibilityMode;

import com.google.inject.Inject;

public class StorePanel extends JPanel {
    private final Browser browser;

    private final Application application;

    /**
     * Used to ignore the first component hidden event coming through to the
     * ComponentListener. The load and hidden events are coming out of order because
     * of the usage of card layout, and loading StorePanel lazily. When adding a component
     * to CardLayout, card layout calls setVisible false on it. The main issue is that we have
     * started loading components lazily as they are selected. So we can't force that componsnts
     * are added to the card layout before we use them.
     */
    private final AtomicBoolean firstHiddenIgnored = new AtomicBoolean(false);

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
                if (firstHiddenIgnored.getAndSet(true) && MozillaInitialization.isInitialized()) {
                    browser.load("about:blank");
                }
            }
        });     
        BrowserUtils.addTargetedUrlAction("_lwStore", new UriAction() {
            @Override
            public boolean uriClicked(final TargetedUri targetedUrl) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        navigator.getNavItem(NavCategory.LIMEWIRE, StoreMediator.NAME).select();
                        load(targetedUrl.getUri());
                    }
                });
                return true;
            }
        }); 
    }
    
    public void loadDefaultUrl() {
        load("http://store.limewire.com/");
    }

    public void load(String url) {
        url = application.addClientInfoToUrl(url);
        if (!MozillaInitialization.isInitialized()) {
            NativeLaunchUtils.openURL(url);
        } else {
            // Reset the page to blank before continuing -- blocking is OK
            // because this is fast.
            MozillaAutomation.blockingLoad(browser, "about:blank");
            browser.load(url + "&isClient=true");
        }
    }
}
