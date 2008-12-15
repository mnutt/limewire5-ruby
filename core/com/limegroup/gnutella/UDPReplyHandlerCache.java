package com.limegroup.gnutella;

import java.net.InetSocketAddress;

import com.limegroup.gnutella.filters.SpamFilter;

public interface UDPReplyHandlerCache {

    public ReplyHandler getUDPReplyHandler(InetSocketAddress addr);

    public void setPersonalFilter(SpamFilter personalFilter);

    public void clear();
    
    public SpamFilter getPersonalFilter();
}