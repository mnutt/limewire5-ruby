package com.limegroup.gnutella.gui.tables;

/** A cell that can have a link. */
public interface Linkable {
    
    /**
     * Returns true if this linkable is a real link.
     */
    boolean isLink();
    
    /**
     * Returns the URL this link should navigate to.
     */
    String getLinkUrl();

    /**
     * Returns the URL that should be displayed to the user.
     */
    String getLinkDisplayUrl();

}
