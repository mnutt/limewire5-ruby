package org.limewire.ui.swing.friends.login;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jdesktop.application.Resource;
import org.limewire.concurrent.FutureEvent;
import org.limewire.core.api.Application;
import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.browser.LimeDomListener;
import org.limewire.ui.swing.browser.UriAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.friends.settings.FacebookFriendAccountConfiguration;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.mozilla.browser.MozillaAutomation;
import org.mozilla.browser.MozillaPanel.VisibilityMode;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.browser.impl.ChromeAdapter;
import org.mozilla.interfaces.nsIDOMEvent;
import org.mozilla.interfaces.nsIDOMEventListener;
import org.mozilla.interfaces.nsIDOMEventTarget;
import org.mozilla.interfaces.nsIDOMWindow2;
import org.mozilla.interfaces.nsISupports;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import net.miginfocom.swing.MigLayout;

public class FacebookLoginAction extends AbstractAction {

    @Resource
    private Font goBackFont;
    @Resource private Color goBackBackground;
    
    private final FacebookFriendAccountConfiguration config;
    private final FriendConnectionFactory friendConnectionFactory;
    private final LoginPopupPanel loginPanel;
    private final Application application;

    @Inject
    public FacebookLoginAction(@Assisted FacebookFriendAccountConfiguration config,
            FriendConnectionFactory friendConnectionFactory, LoginPopupPanel loginPanel,
            Application application) {
        
        super(config.getLabel(), config.getLargeIcon());
        putValue(ServiceSelectionLoginPanel.CONFIG, config);
        GuiUtils.assignResources(this);
        
        this.config = config;
        this.friendConnectionFactory = friendConnectionFactory;
        this.loginPanel = loginPanel;
        this.application = application;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final Browser browser = new Browser(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN, VisibilityMode.DEFAULT) {
            @Override
            public void onAttachBrowser(ChromeAdapter chromeAdapter,
                    ChromeAdapter parentChromeAdapter) {
                super.onAttachBrowser(chromeAdapter, parentChromeAdapter);
                nsIDOMEventTarget eventTarget = XPCOMUtils.qi(chromeAdapter.getWebBrowser().getContentDOMWindow(),
                        nsIDOMWindow2.class).getWindowRoot();
                
                LimeDomListener limeDomListener = new LimeDomListener();
                limeDomListener.addTargetedUrlAction("", new UriAction() {
                    @Override
                    public boolean uriClicked(TargetedUri targetedUri) {
                        NativeLaunchUtils.openURL(targetedUri.getUri());
                        return true;
                    }
                });
                eventTarget.addEventListener("click", limeDomListener, true);
                
                eventTarget.addEventListener("load", new nsIDOMEventListener() {
                    @Override
                    public void handleEvent(nsIDOMEvent event) {
                        String url = getUrl();
                        if (url.contains("desktopapp.php")) {
                            config.loadCookies();
                            friendConnectionFactory.login(config);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    loginPanel.finished();
                                }
                            });
                        } else if (url.contains("login")) {
                            String script = "(function() {" +
                            "    function addHiddenInput(name, value) {" +
                            "    var input = document.createElement('input'); input.type='hidden'; input.name=name; input.value=value; document.forms[0].appendChild(input);" +
                            "    }" +
                            "    addHiddenInput('persistent', '1');" +
                            "    addHiddenInput('visibility', 'true');" +
                            "})();";
                            jsexec(script);
                            if(config.isAutologin()) {
                                script = "(function() {" +
                                "    function checkStayLoggedIn(offline_checkbox) {" +
                                "       offline_checkbox.checked = 1;" +
                                "    }" +
                                "    checkStayLoggedIn(document.getElementById('offline_access'));" +
                                "})();";
                                jsexec(script);    
                            }
                        }
                    }
                    @Override
                    public nsISupports queryInterface(String uuid) {
                        return null;
                    }
                }, true);
            }
        };
        
        JPanel facebookLoginPanel = new JPanel();
        facebookLoginPanel.setLayout(new BorderLayout());
        facebookLoginPanel.add(browser, BorderLayout.CENTER);
        
        HyperlinkButton goBackLink = new HyperlinkButton(new AbstractAction(I18n.tr("Choose another account")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginPanel.restart();
            }
        });
        goBackLink.setFont(goBackFont);
        
        JPanel goBackPanel = new JPanel(new MigLayout("insets 0 0 2 0, gap 0, fill"));
        goBackPanel.setBackground(goBackBackground);
        goBackPanel.add(goBackLink, "align center");
        goBackPanel.setBorder(BorderFactory.createMatteBorder(1,0,0,0, new Color(0xa4a4a4)));
        
        facebookLoginPanel.add(goBackPanel, BorderLayout.SOUTH);
        
        loginPanel.setLoginComponent(facebookLoginPanel);
        MozillaAutomation.blockingLoad(browser, "about:blank");
        // show a loading panel (but not immediately -- the blocking load finishing may hide it)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                browser.showLoadingPanel();
            }
        });
        friendConnectionFactory.requestLoginUrl(config).addFutureListener(new EventListener<FutureEvent<String>>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FutureEvent<String> event) {
                switch (event.getType()) {
                case SUCCESS:
                    browser.load(event.getResult());
                    break;
                case EXCEPTION:
                    browser.load(application.addClientInfoToUrl("http://client-data.limewire.com/fberror/"));
                    break;
                default:
                    throw new IllegalStateException(event.getType().toString());
                }
            }
        });
    }
}