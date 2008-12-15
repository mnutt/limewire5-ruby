package org.limewire.core.impl.search.sponsored;

import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.core.api.search.sponsored.SponsoredResultTarget;
import org.limewire.util.Objects;

public class CoreSponsoredResult implements SponsoredResult {

    private final String text;
    private final SponsoredResultTarget target;
    private final String title;
    private final String navUrl;
    private final String visibleUrl;

    /**
     * @param title title of the ad
     * @param text main text of the ad
     * @param visibleUrl url shown in the ad
     * @param navUrl the url navigated to when clicked
     * @param target LinkTarget.INTERNAL to open within LimeWire or
     *        LinkTarget.EXTERNAL to open in a native browser
     */
    public CoreSponsoredResult(String title, String text, String visibleUrl, String navUrl, SponsoredResultTarget target) {
        this.title = Objects.nonNull(title, "title").replace("|", "\n");
        this.text = Objects.nonNull(text, "text").replace("|", "\n");
        this.visibleUrl = Objects.nonNull(visibleUrl, "visibleUrl");
        this.navUrl = Objects.nonNull(navUrl, "navUrl");
        this.target = Objects.nonNull(target, "target");
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.search.sponsored.ISponsoredResult#getTitle()
     */
    public String getTitle() {
        return title;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.search.sponsored.ISponsoredResult#getVisibleUrl()
     */
    public String getVisibleUrl() {
        return visibleUrl;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.search.sponsored.ISponsoredResult#getUrl()
     */
    public String getUrl(){
        return navUrl;
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.search.sponsored.ISponsoredResult#getTarget()
     */
    public SponsoredResultTarget getTarget() {
        return target;
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.search.sponsored.ISponsoredResult#getText()
     */
    public String getText() {
        return text;
    }
}
