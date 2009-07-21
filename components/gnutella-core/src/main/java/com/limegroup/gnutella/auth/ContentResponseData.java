package com.limegroup.gnutella.auth;

import java.io.Serializable;

import org.limewire.util.StringUtils;

import com.limegroup.gnutella.messages.vendor.ContentResponse;

/** A response for content. */
public class ContentResponseData implements Serializable {
    private static final long serialVersionUID = -2625113217839178878L;
    
    private long created;
    private boolean ok;
    
    /** Constructs a new ContentResponseData with data from the given ContentResponse. */
    public ContentResponseData(ContentResponse msg) {
        this(System.currentTimeMillis(), msg.getOK());
    }
    
    /** Hook for tests to create Responses at different times. */
    private ContentResponseData(long now, boolean ok) {
        this.created = now;
        this.ok = ok;
    }
    
    /** Returns if this is OK. */
    public boolean isOK() {
        return ok;
    }
    
    /** Returns the time this ContentResponseData was created. */
    public long getCreationTime() {
        return created;
    }
    
    @Override
    public String toString() {
        return StringUtils.toStringBlacklist(this, serialVersionUID);
    }
}
