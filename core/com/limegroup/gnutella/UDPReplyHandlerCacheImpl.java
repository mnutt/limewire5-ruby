package com.limegroup.gnutella;

import java.net.InetSocketAddress;

import org.limewire.collection.FixedsizeForgetfulHashMap;
import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.inject.Providers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.filters.SpamFilterFactory;

@Singleton
public class UDPReplyHandlerCacheImpl implements UDPReplyHandlerCache {
    
    private final UDPReplyHandlerFactory udpReplyHandlerFactory;
    private volatile Provider<SpamFilter> personalFilter;
    
    /** A mapping of UDPReplyHandlers, to prevent creation of them over-and-over. */
    private final FixedsizeForgetfulHashMap<InetSocketAddress, UDPReplyHandler> udpReplyHandlerCache =
        new FixedsizeForgetfulHashMap<InetSocketAddress, UDPReplyHandler>(500);
    
    @Inject
    public UDPReplyHandlerCacheImpl(UDPReplyHandlerFactory udpReplyHandlerFactory, final SpamFilterFactory spamFilterFactory) {
        this.udpReplyHandlerFactory = udpReplyHandlerFactory;
        personalFilter = new AbstractLazySingletonProvider<SpamFilter>() {
            @Override
            protected SpamFilter createObject() {
                return spamFilterFactory.createPersonalFilter();
            }
        };
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.UDPReplyHnadlerCache#getUDPReplyHandler(java.net.InetSocketAddress)
     */
    public synchronized UDPReplyHandler getUDPReplyHandler(InetSocketAddress addr) {
        UDPReplyHandler handler = udpReplyHandlerCache.get(addr);
        if (handler == null) {
            handler = udpReplyHandlerFactory.createUDPReplyHandler(addr, personalFilter.get());
            udpReplyHandlerCache.put(addr, handler);
        }
        return handler;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.UDPReplyHnadlerCache#setPersonalFilter(com.limegroup.gnutella.filters.SpamFilter)
     */
    public synchronized void setPersonalFilter(SpamFilter personalFilter) {
        if (personalFilter == null) { 
            throw new NullPointerException("personalFiter must not be null");
        }
        this.personalFilter = Providers.of(personalFilter);
        clear();
    }
    
    public SpamFilter getPersonalFilter() {
        return personalFilter.get();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.UDPReplyHnadlerCache#clear()
     */
    public synchronized void clear() {
        udpReplyHandlerCache.clear();
    }
}
