package org.limewire.core.api.search.sponsored;


public interface SponsoredResult {

    String getTitle();

    String getVisibleUrl();

    String getUrl();

    SponsoredResultTarget getTarget();

    String getText();

}