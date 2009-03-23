package org.limewire.ui.swing.home;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Application;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.browser.BrowserUtils;
import org.limewire.ui.swing.browser.UriAction;
import org.limewire.ui.swing.components.HTMLPane;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;
import org.mozilla.browser.MozillaAutomation;
import org.mozilla.browser.MozillaInitialization;
import org.mozilla.browser.MozillaPanel.VisibilityMode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/** The main home page.*/
@Singleton
public class HomePanel extends JXPanel {

    public static final String NAME = "Home";
    
    private boolean firstRequest = true;
    
    private final Application application;
    private final Browser browser;
    private final HTMLPane fallbackBrowser;

    @Inject
    public HomePanel(Application application, final Navigator navigator) {
        this.application = application;
        
        setPreferredSize(new Dimension(500, 500));
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        
        
        
        if(MozillaInitialization.isInitialized()) {            
            // Hide the page when the browser goes away.
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentHidden(ComponentEvent e) {
                    browser.load("about:blank");
                }
            });
            
            BrowserUtils.addTargetedUrlAction("_lwHome", new UriAction() {
                @Override
                public boolean uriClicked(final TargetedUri targetedUrl) {
                    SwingUtils.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            navigator.getNavItem(NavCategory.LIMEWIRE, NAME).select();
                            load(targetedUrl.getUri());
                        }
                    });
                    return true;
                }
            });
            
            browser = new Browser(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN, VisibilityMode.DEFAULT);
            fallbackBrowser = null;
            add(browser, gbc);
            loadDefaultUrl();
        } else {
            browser = null;
            fallbackBrowser = new HTMLPane();
            fallbackBrowser.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        NativeLaunchUtils.openURL(e.getURL().toExternalForm());
                    }
                }
            });
            loadDefaultUrl();
            JScrollPane scroller = new JScrollPane(fallbackBrowser,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scroller.setBorder(BorderFactory.createEmptyBorder());
            add(scroller, gbc);
        }
    }
    
    public void loadDefaultUrl() {
        load("http://client-data.limewire.com/client_startup/home/");
    }

    public void load(String url) {
        url = application.getUniqueUrl(url);
        if(MozillaInitialization.isInitialized()) {
            if(firstRequest) {
                if(browser.isLastRequestSuccessful()) {
                    firstRequest = false;
                } else {
                    url += "&firstRequest=true";
                }
            }
            // Reset the page to blank before continuing -- blocking is OK because this is fast.
            MozillaAutomation.blockingLoad(browser, "about:blank");
            browser.load(url);
        } else {
            String offlinePage = "<html><body>This is the offline home page.</body></html>";
            url += "&html32=true";
            if(firstRequest) {
                if(fallbackBrowser.isLastRequestSuccessful()) {
                    firstRequest = false;
                } else {
                    url += "&firstRequest=true";
                }
            }
            fallbackBrowser.setPageAsynchronous(url, offlinePage);
        }
    }    
}
