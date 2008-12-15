package com.limegroup.gnutella.spam;

import com.google.inject.Provider;
import com.limegroup.gnutella.filters.IPFilter;
import org.limewire.io.IP;

/**
 * A token representing an IP address
 */
public class AddressToken extends Token {
    
    /**
     * A node is unlikely to return spam and non-spam results in the same
     * session. However, the user may mark unwanted results as spam even if
     * they don't come from a professional spammer, and the same address may
     * be shared by spammers and non-spammers due to NATs or dynamic IPs. We
     * can't use the port number to distinguish between NATed nodes because
     * it's too easy for a spammer to use multiple ports. Therefore we should
     * be a little bit cautious about using an address to identify spammers.  
     */
    private static final float ADDRESS_WEIGHT = 0.2f;
    
    private transient Provider<IPFilter> ipFilter;

    private final String address;
    private boolean ratingInitialized;
    
    void setIPFilter(Provider<IPFilter> ipFilter) {
        this.ipFilter = ipFilter;
    }
    
	public AddressToken(String address, Provider<IPFilter> ipFilter) {
		this.address = address;
        this.ipFilter = ipFilter;
        ratingInitialized = false;
	}
	
    /**
     * Rating initialization is costly, and many tokens are created 
     * simply to be replaced by existing tokens in the RatingTable,
     * so we initialize the rating lazily.
     */
    private void initializeRating() {
        if (ratingInitialized) {
            return;
        }
        
        if(ipFilter == null)
            throw new IllegalStateException("must initialize IPFilter after deserializing.");
        
        int logDistance = ipFilter.get().logMinDistanceTo(new IP(address));
        rating = 1 - (logDistance / 32f); // Between 0 and 1
        ratingInitialized = true;
    }
    
    @Override
	protected float getRating() {
        if(!ratingInitialized)
            initializeRating();
		return super.getRating();
	}
    
    @Override
	protected float getWeight() {
        return ADDRESS_WEIGHT;
    }
    
    @Override
    public int hashCode() {
        return address.hashCode();
    }
    
    @Override public boolean equals(Object o) {
        if(o == null)
            return false;
        if(!(o instanceof AddressToken))
            return false;
        return address.equals(((AddressToken)o).address);
    }
    
	@Override
    public String toString() {
		return "address " + address;
	}
}
