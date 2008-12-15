package com.limegroup.gnutella.filters;

import java.util.Set;
import java.util.TreeSet;

import org.limewire.io.GUID;

import com.google.inject.Singleton;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;

/**
 * Filter for query replies based on the GUID
 * of the reply, and other details.
 */
@Singleton
public final class MutableGUIDFilter implements SpamFilter {
    
    MutableGUIDFilter(KeywordFilter filter) { 
        FILTER = filter;
    }
    
    MutableGUIDFilter() { 
        this(new XMLDocFilter());
        FILTER.disallowAdult(); 
    }
    /**
     * The Set of GUIDs to compare.
     *
     * LOCKING: Never modify -- instead synchronize & replace.
     */
    private Set<byte[]> _guids = new TreeSet<byte[]>(new GUID.GUIDByteComparator());
    
    /**
     * The underlying filter.
     */
    private final KeywordFilter FILTER;
    
    /**
     * Adds a guid to be scanned for keyword filters.
     */
    public synchronized void addGUID(byte[] guid) {
        Set<byte[]> guids = new TreeSet<byte[]>(new GUID.GUIDByteComparator());
        guids.addAll(_guids);
        guids.add(guid);
        _guids = guids;
    }
    
    /**
     * Removes a guid from the list of those scanned.
     */
    public void removeGUID(byte[] guid) {
        if(_guids.size() == 0) {
            return;
        } else {
            synchronized(this) {
                if(_guids.size() > 0) {
                    Set<byte[]> guids = new TreeSet<byte[]>(new GUID.GUIDByteComparator());
                    guids.addAll(_guids);
                    guids.remove(guid);
                    _guids = guids;
                }
            }
        }
    }
    
    /**
     * Determines if this QueryReply is allowed.
     */
    public boolean allow(QueryReply qr) {
        if(_guids.contains(qr.getGUID())) {
            return FILTER.allow(qr);
        } else {
            return true;
        }
    }
    
    /**
     * Determines if this message is allowed.
     */
    public boolean allow(Message m) {
        if(m instanceof QueryReply)
            return allow((QueryReply)m);
        else
            return true;
    }
}