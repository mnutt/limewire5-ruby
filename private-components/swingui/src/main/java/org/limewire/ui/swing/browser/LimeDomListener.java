package org.limewire.ui.swing.browser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.browser.MozillaExecutor;
import org.mozilla.browser.MozillaRuntimeException;
import org.mozilla.dom.NodeFactory;
import org.mozilla.interfaces.nsIDOMEvent;
import org.mozilla.interfaces.nsIDOMEventListener;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.Mozilla;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Launches target="_blank" links in native browsers and swallows their
 * DomEvents to prevent default behavior.
 */
class LimeDomListener implements nsIDOMEventListener {

    private static final Log LOG = LogFactory.getLog(LimeDomListener.class);
   
    private final Map<String, TargetedUrlAction> urlActions = new ConcurrentHashMap<String, TargetedUrlAction>();
    
    void addTargetedUrlAction(String target, TargetedUrlAction action) {
        urlActions.put(target, action);
    }

    public void handleEvent(nsIDOMEvent event) {
        assert MozillaExecutor.isMozillaThread();
        try {
            TargetedUrlAction.TargetedUrl targetedUrl = getTargetedUrl(event);
            if(targetedUrl != null && targetedUrl.getTarget() != null) {
                TargetedUrlAction action = urlActions.get(targetedUrl.getTarget());
                if(action != null) {
                    event.preventDefault();
                    if(targetedUrl.getUrl() != null) {
                        action.targettedUrlClicked(targetedUrl);
                    }
                }
            }
        } catch (MozillaRuntimeException e) {
            // This should not occur
            LOG.error("MozillaRuntimeException", e);
        }
    }

    /**
     * 
     * @return TargetedUrl if the event contains a URL, null if there is no URL.
     */
    private TargetedUrlAction.TargetedUrl getTargetedUrl(nsIDOMEvent event) {
        TargetedUrlAction.TargetedUrl targetedUrl = null;
        Node node = NodeFactory.getNodeInstance(event.getTarget());
        if (!"html".equalsIgnoreCase(node.getNodeName())) {
            targetedUrl = getTargetedUrl(node);
            if (targetedUrl == null) {
                // also check parent node
                targetedUrl = getTargetedUrl(node.getParentNode());
            }
        }
        return targetedUrl;
    }

    /**
     * 
     * @return a TargetedUrl if the nodes attributes contain href, null if not.
     */
    private TargetedUrlAction.TargetedUrl getTargetedUrl(Node node) {
        if (node != null) {
            NamedNodeMap map = node.getAttributes();
            if (map != null) {
                Node hrefNode = map.getNamedItem("href");
                if (hrefNode != null) {
                    String target = null;
                    String url = hrefNode.getNodeValue();
                    Node targetNode = map.getNamedItem("target");
                    if (targetNode != null) {
                        target = targetNode.getNodeValue();
                    }
                    return new TargetedUrlAction.TargetedUrl(target, url);
                }
            }
        }
        return null;
    }

    

    public nsISupports queryInterface(String uuid) {
        return Mozilla.queryInterface(this, uuid);
    }
    
    

}