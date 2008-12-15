package org.limewire.core.impl.search.sponsored;

import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.core.api.search.sponsored.SponsoredResultTarget;

public class MockSponsoredResult implements SponsoredResult {

   

    private String text;

    private SponsoredResultTarget target;

    private String title;

    private String navUrl;

    private String visibleUrl;

    /**
     * @param title title of the ad
     * @param text main text of the ad
     * @param visibleUrl url shown in the ad
     * @param navUrl the url navigated to when clicked
     * @param target LinkTarget.INTERNAL to open within LimeWire or
     *        LinkTarget.EXTERNAL to open in a native browser
     */
    public MockSponsoredResult(String title, String text, String visibleUrl, String navUrl, SponsoredResultTarget target) {
        this.title = title;
        this.text = text;
        this.visibleUrl = visibleUrl;
        this.navUrl = navUrl;
        this.target = target;
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
