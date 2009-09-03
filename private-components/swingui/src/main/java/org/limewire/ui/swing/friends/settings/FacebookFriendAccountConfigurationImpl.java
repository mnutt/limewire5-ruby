package org.limewire.ui.swing.friends.settings;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.Icon;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.limewire.io.UnresolvedIpPort;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.core.settings.FacebookSettings;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsICookie;
import org.mozilla.interfaces.nsICookieManager;
import org.mozilla.interfaces.nsISimpleEnumerator;

public class FacebookFriendAccountConfigurationImpl extends FriendAccountConfigurationImpl implements FacebookFriendAccountConfiguration {

    private static Log LOG = LogFactory.getLog(FacebookFriendAccountConfigurationImpl.class);
    private final FriendAccountConfigurationManager configurationManager;

    public FacebookFriendAccountConfigurationImpl(boolean requireDomain, String serviceName, String label, Icon icon, Icon largeIcon, String resource, List<UnresolvedIpPort> defaultServers, Type type, FriendAccountConfigurationManager configurationManager) {
        super(requireDomain, serviceName, label, icon, largeIcon, resource, defaultServers, type);
        this.configurationManager = configurationManager;
        loadCookies();
        for(Object key : FacebookSettings.ATTRIBUTES.get().keySet()) {
            setAttribute((String) key, FacebookSettings.ATTRIBUTES.get().get(key));
        }
    }

    @Override
    public void loadCookies() {
        nsICookieManager cookieService = XPCOMUtils.getServiceProxy("@mozilla.org/cookiemanager;1",
        nsICookieManager.class);
        nsISimpleEnumerator enumerator = cookieService.getEnumerator();
        List<Cookie> cookiesCopy = new ArrayList<Cookie>();
        Cookie login = null;
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
                if(copy.getName().equals("lxe")) {
                    login = copy;
                }
            } else {
                LOG.debugf("dropping cookie {0} = {1} for host {2}", cookie.getName(), cookie.getValue(), cookie.getHost());
            }
        }
        
        setAttribute("cookie", cookiesCopy);
        setUsername(login);
    }

    @Override
    public void clearCookies() {
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

    @Override
    public void setAutoLogin(boolean autoLogin) {
        configurationManager.setAutoLoginConfig(autoLogin ? this : null);
        if(autoLogin) {
            FacebookSettings.ATTRIBUTES.get().put("session_key", getAttribute("session_key"));
            FacebookSettings.ATTRIBUTES.get().put("secret", getAttribute("secret"));
            FacebookSettings.ATTRIBUTES.get().put("uid", getAttribute("uid"));
        } else {
            FacebookSettings.ATTRIBUTES.get().remove("session_key");
            FacebookSettings.ATTRIBUTES.get().remove("secret");
            FacebookSettings.ATTRIBUTES.get().remove("uid");
        }
        FacebookSettings.ATTRIBUTES.set(FacebookSettings.ATTRIBUTES.get());
    }

    @Override
    public boolean isAutologin() {
        return configurationManager.getAutoLoginConfig() == this;
    }

    private void setUsername(Cookie login_x) {
        if(login_x != null) {
            try {
                String value = URLDecoder.decode(login_x.getValue(), "UTF-8");
                setUsername(value);
            } catch (UnsupportedEncodingException e) {
                LOG.debugf(e, "failed to decode {0}", login_x.getValue());
            }
        }
    }

    @Override
    public boolean storePassword() {
        return false;
    }
}
