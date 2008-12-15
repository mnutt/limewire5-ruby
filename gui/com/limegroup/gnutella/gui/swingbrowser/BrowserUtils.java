package com.limegroup.gnutella.gui.swingbrowser;

import org.mozilla.browser.MozillaExecutor;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsIDOMEventTarget;
import org.mozilla.interfaces.nsIDOMWindow2;
import org.mozilla.interfaces.nsIWebBrowserChrome;
import org.w3c.dom.Node;

public class BrowserUtils {
    

    private static final  LimeDomListener DOM_ADAPTER = new LimeDomListener();
    

    /**
     * 
     * @return true if node is a text input, password input or textarea
     */
    public static boolean isTextControl(Node node) {

        boolean isText = false;

        if ("input".equalsIgnoreCase(node.getNodeName())) {
            Node type = node.getAttributes().getNamedItem("type");
            // null, text or password are text controls. Filters out checkbox,
            // etc
            isText = type == null || "text".equalsIgnoreCase(type.getNodeValue())
                    || "password".equalsIgnoreCase(type.getNodeValue());
        } else {
            isText = "textarea".equalsIgnoreCase(node.getNodeName());
        }

        return isText;
    }

    /**
     * Adds LimeDomListener to chromeAdapter
     */
    public static void addDomListener(final nsIWebBrowserChrome chrome) {
        MozillaExecutor.mozAsyncExec(new Runnable() {
            public void run() {               
                nsIDOMEventTarget eventTarget = XPCOMUtils.qi(
                        chrome.getWebBrowser().getContentDOMWindow(), nsIDOMWindow2.class)
                        .getWindowRoot();
                // TODO: some way to listen for javascript?
                eventTarget.addEventListener("click", DOM_ADAPTER, true);
            }

        });
    }
    
    
    public static void removeDomListener(final nsIWebBrowserChrome chrome){
        MozillaExecutor.mozAsyncExec(new Runnable() {
            public void run() {
                nsIDOMEventTarget eventTarget = XPCOMUtils.qi(
                        chrome.getWebBrowser().getContentDOMWindow(), nsIDOMWindow2.class)
                        .getWindowRoot();
                eventTarget.removeEventListener("click", DOM_ADAPTER, true);
            }

        });
    }

}
