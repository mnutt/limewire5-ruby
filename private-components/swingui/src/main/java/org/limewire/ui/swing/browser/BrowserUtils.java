package org.limewire.ui.swing.browser;

import org.limewire.concurrent.ManagedThread;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsIDOMEventTarget;
import org.mozilla.interfaces.nsIDOMWindow2;
import org.mozilla.interfaces.nsIWebBrowserChrome;
import org.w3c.dom.Node;

public class BrowserUtils {

    private static final LimeDomListener DOM_ADAPTER = new LimeDomListener();
    
    static {
        addTargetedUrlAction("_blank", new TargetedUrlAction() {
            @Override
            public void targettedUrlClicked(final TargetedUrl targetedUrl) {
                // Open url in new thread to keep Mozilla thread responsive
                new ManagedThread(new Runnable() {
                    public void run() {
                        NativeLaunchUtils.openURL(targetedUrl.getUrl());
                    }
                }).start();
            }
        });
    }
    
    /** Adds an action to be performed when the given target is clicked. */
    public static void addTargetedUrlAction(String target, TargetedUrlAction action) {
        DOM_ADAPTER.addTargetedUrlAction(target, action);
    }

    /**
     * 
     * @return true if node is a text input, password input or textarea
     */
    static boolean isTextControl(Node node) {

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
    static void addDomListener(final nsIWebBrowserChrome chrome) {         
        nsIDOMEventTarget eventTarget = XPCOMUtils.qi(
                chrome.getWebBrowser().getContentDOMWindow(), nsIDOMWindow2.class)
                .getWindowRoot();
        // TODO: some way to listen for javascript?
        eventTarget.addEventListener("click", DOM_ADAPTER, true);
    }
    
    
    static void removeDomListener(final nsIWebBrowserChrome chrome){
        nsIDOMEventTarget eventTarget = XPCOMUtils.qi(
                chrome.getWebBrowser().getContentDOMWindow(), nsIDOMWindow2.class)
                .getWindowRoot();
        eventTarget.removeEventListener("click", DOM_ADAPTER, true);
    }

}
