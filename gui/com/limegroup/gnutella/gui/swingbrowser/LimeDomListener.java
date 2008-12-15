package com.limegroup.gnutella.gui.swingbrowser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ManagedThread;
import org.mozilla.browser.MozillaExecutor;
import org.mozilla.browser.MozillaRuntimeException;
import org.mozilla.dom.NodeFactory;
import org.mozilla.interfaces.nsIDOMEvent;
import org.mozilla.interfaces.nsIDOMEventListener;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.Mozilla;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.limegroup.gnutella.gui.GUIMediator;

/**
 * Launches target="_blank" links in native browsers and swallows their
 * DomEvents to prevent default behavior.
 */
public class LimeDomListener implements nsIDOMEventListener {

    private static final Log LOG = LogFactory.getLog(LimeDomListener.class);

    public void handleEvent(nsIDOMEvent event) {
        assert MozillaExecutor.isMozillaThread();
        try {
            final TargetedUrl targetedUrl = getTargetedUrl(event);
            if (targetedUrl != null && "_blank".equals(targetedUrl.getTarget())) {
                // kill the event
                event.preventDefault();
                String url = targetedUrl.getUrl();
                if (url != null) {
                    openUrl(url);
                }
            }
        } catch (MozillaRuntimeException e) {
            // This should not occur
            LOG.error("MozillaRuntimeException", e);
        }
    }

    private void openUrl(final String url) {
        // Open url in new thread to keep Mozilla thread responsive
        new ManagedThread(new Runnable() {
            public void run() {
                if (LOG.isDebugEnabled()) {
                    LOG.info("open " + url);
                }
                GUIMediator.openURL(url);
            }
        }).start();
    }

    /**
     * 
     * @return TargetedUrl if the event contains a URL, null if there is no URL.
     */
    private TargetedUrl getTargetedUrl(nsIDOMEvent event) {

        TargetedUrl targetedUrl = null;

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
    private TargetedUrl getTargetedUrl(Node node) {
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
                    return new TargetedUrl(target, url);
                }
            }
        }
        return null;
    }

    private static class TargetedUrl {
        private String target;

        private String url;

        public TargetedUrl(String target, String url) {
            this.target = target;
            this.url = url;
        }

        public String getTarget() {
            return target;
        }

        public String getUrl() {
            return url;
        }
    }

    public nsISupports queryInterface(String uuid) {
        return Mozilla.queryInterface(this, uuid);
    }

}