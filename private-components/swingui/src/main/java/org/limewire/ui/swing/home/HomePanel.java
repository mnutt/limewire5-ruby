package org.limewire.ui.swing.home;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Application;
import org.limewire.inject.LazySingleton;
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

/** The main home page.*/
@LazySingleton
public class HomePanel extends JXPanel {
    
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
                            navigator.getNavItem(NavCategory.LIMEWIRE, HomeMediator.NAME).select();
                            load(targetedUrl.getUri());
                        }
                    });
                    return true;
                }
            });
            
            browser = new Browser(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN, VisibilityMode.DEFAULT);
            fallbackBrowser = null;
            add(browser, gbc);
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
            JScrollPane scroller = new JScrollPane(fallbackBrowser,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scroller.setBorder(BorderFactory.createEmptyBorder());
            add(scroller, gbc);
        }
    }
    
    public void loadDefaultUrl() {
        load("http://mnutt.github.com/limewire-remote/loading.html");
    }

    public void load(String url) {
        url = application.addClientInfoToUrl(url);
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
       
            URL bgImage = HomePanel.class.getResource("/org/limewire/ui/swing/mainframe/resources/icons/static_pages/body_bg.png");
            URL topImage = HomePanel.class.getResource("/org/limewire/ui/swing/mainframe/resources/icons/static_pages/header_logo.png");
                    
            String offlinePage = "<html><head><style type=\"text/css\">* {margin: 0;  padding: 0;} body {background: #EAEAEA url(\""+ bgImage.toExternalForm() + "\") repeat-x left top; font-family: Arial, sans-serif;}table#layout tr td#header {  background: url(\"" + topImage.toExternalForm() + "\") no-repeat center top;}table#layout tr td h2 {  font-size: 16px;  margin: 0 0 8px 0;  color: #807E7E;}table#layout tr td p {  font-size: 11px;  color: #931F22;}</style></head><body><center>  <table id=\"layout\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"400\" style=\"margin: 46px 0 0 0\">    <tr valign=\"top\">      <td id=\"header\" height=\"127\" align=\"center\"></td>    </tr>    <tr valign=\"top\">      <td align=\"center\">        <h2>You are offline</h2>        <p>Please check your internet connection.</p>      </td>    </tr>  </table></center></body></html>";
        
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
