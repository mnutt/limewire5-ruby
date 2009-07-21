package org.limewire.ui.swing.friends.login;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jdesktop.application.Resource;
import org.limewire.concurrent.FutureEvent;
import org.limewire.core.api.Application;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.friend.api.Network;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.browser.LimeDomListener;
import org.limewire.ui.swing.browser.UriAction;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.friends.settings.FriendAccountConfiguration;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.mozilla.browser.MozillaAutomation;
import org.mozilla.browser.MozillaPanel.VisibilityMode;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.browser.impl.ChromeAdapter;
import org.mozilla.interfaces.nsICookie;
import org.mozilla.interfaces.nsICookieManager;
import org.mozilla.interfaces.nsIDOMEvent;
import org.mozilla.interfaces.nsIDOMEventListener;
import org.mozilla.interfaces.nsIDOMEventTarget;
import org.mozilla.interfaces.nsIDOMWindow2;
import org.mozilla.interfaces.nsISimpleEnumerator;
import org.mozilla.interfaces.nsISupports;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import net.miginfocom.swing.MigLayout;

public class FacebookLoginAction extends AbstractAction {

    private static final Log LOG = LogFactory.getLog(FacebookLoginAction.class);

    @Resource
    private Font goBackFont;
    @Resource private Color goBackBackground;
    
    private final FriendAccountConfiguration config;
    private final FriendConnectionFactory friendConnectionFactory;
    private final LoginPopupPanel loginPanel;
    private final Application application;

    private ListenerSupport<FriendConnectionEvent> listenerSupport;
    private EventListener<FriendConnectionEvent> listener;

    @Inject
    public FacebookLoginAction(@Assisted FriendAccountConfiguration config,
            FriendConnectionFactory friendConnectionFactory, LoginPopupPanel loginPanel,
            Application application) {
        
        super(config.getLabel(), config.getLargeIcon());
        
        GuiUtils.assignResources(this);
        
        this.config = config;
        this.friendConnectionFactory = friendConnectionFactory;
        this.loginPanel = loginPanel;
        this.application = application;
    }
    
    @Inject 
    public void register(ListenerSupport<FriendConnectionEvent> listenerSupport) {
        this.listenerSupport = listenerSupport;
        
        listener = new EventListener<FriendConnectionEvent>() {
            @Override
            public void handleEvent(FriendConnectionEvent event) {
                if(event.getType() == FriendConnectionEvent.Type.DISCONNECTED &&
                        event.getSource().getConfiguration().getType() == Network.Type.FACEBOOK) {
                    nsICookieManager cookieService = XPCOMUtils.getServiceProxy("@mozilla.org/cookiemanager;1",
                            nsICookieManager.class);
                    nsISimpleEnumerator enumerator = cookieService.getEnumerator();
                    while(enumerator.hasMoreElements()) {                        
                        nsICookie cookie = XPCOMUtils.proxy(enumerator.getNext(), nsICookie.class);
                        if(cookie.getHost().equals(".facebook.com")) {
                            cookieService.remove(cookie.getHost(), cookie.getName(), cookie.getPath(), false);    
                        }
                    }
                }
            }
        };
        
        listenerSupport.addListener(listener);
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
                            nsICookieManager cookieService = XPCOMUtils.getServiceProxy("@mozilla.org/cookiemanager;1",
                            nsICookieManager.class);
                            nsISimpleEnumerator enumerator = cookieService.getEnumerator();
                            List<Cookie> cookiesCopy = new ArrayList<Cookie>();
                            Cookie login_x = null;
                            while(enumerator.hasMoreElements()) {                        
                                nsICookie cookie = XPCOMUtils.proxy(enumerator.getNext(), nsICookie.class);
                                if(cookie.getHost() != null && cookie.getHost().endsWith(".facebook.com")) {
                                    LOG.debugf("adding cookie {0} = {1} for host {2}", cookie.getName(), cookie.getValue(), cookie.getHost());
                                    BasicClientCookie copy = new BasicClientCookie(cookie.getName(), cookie.getValue());
                                    copy.setDomain(cookie.getHost());
                                    double expiry = cookie.getExpires();
                                    if(expiry != 0 && expiry != 1) {
                                        long expiryMillis = (long) expiry * 1000;
                                        copy.setExpiryDate(new Date(expiryMillis));
                                    }
                                    copy.setPath(cookie.getPath());
                                    copy.setSecure(cookie.getIsSecure());
                                    // TODO copy.setVersion();
                                    cookiesCopy.add(copy);
                                    if(copy.getName().equals("login_x")) {
                                        login_x = copy;
                                    }
                                } else {
                                    LOG.debugf("dropping cookie {0} = {1} for host {2}", cookie.getName(), cookie.getValue(), cookie.getHost());
                                }
                            }
                            
                            config.setAttribute("cookie", cookiesCopy);
                            setUsername(config, login_x);
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
                        }
                    }
                    @Override
                    public nsISupports queryInterface(String uuid) {
                        return null;
                    }
                }, true);
            }
        };
        
        JPanel facebookLoginPanel = new DisposablePanel();
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
    
    private void setUsername(FriendAccountConfiguration config, Cookie login_x) {
        if(login_x != null) {
            try {
                String value = URLDecoder.decode(login_x.getValue(), "UTF-8");
                config.setUsername(extractEmail(value));
            } catch (UnsupportedEncodingException e) {
                LOG.debugf(e, "failed to decode {0}", login_x.getValue());
            }
        }
    }

    private String extractEmail(String s) {
        int emailNameIndex = s.indexOf("\"email\"");
        s = s.substring(emailNameIndex + "\"email\"".length());
        int emailStart = s.indexOf('"');
        String email = s.substring(emailStart + 1);
        int emailEndIndex = email.indexOf('"');
        email = email.substring(0, emailEndIndex);
        return email;
    }
    
    // TODO: For some reason this class is an action not a panel.  In order for the listeners to be 
    //  cleaned up properly the panel the action creates must be disposable.
    // This does not make sense... This needs to be cleaned up.
    private class DisposablePanel extends JPanel implements Disposable {
        @Override
        public void dispose() {
            listenerSupport.removeListener(listener);
        }   
    }
}