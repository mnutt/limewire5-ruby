package com.limegroup.gnutella.messagehandlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.collection.IntSet;
import org.limewire.inspection.Inspectable;
import org.limewire.io.GUID;
import org.limewire.security.SecurityToken;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;

/**
 * A session of OOB result exchange between the local host and a remote host.
 */
class OOBSession implements Inspectable {
    // inspection-related fields
    private final List<Long> responseTimestamps = new ArrayList<Long>(10);
    private final List<Integer> responseCounts = new ArrayList<Integer>(10);
    private final List<Integer> addedResponses = new ArrayList<Integer>(10);
    private final long start;
	// end inspection-related fields
    
    private final SecurityToken token;
    private final IntSet urnHashCodes;
    
    private IntSet responseHashCodes;
    
    private final int requestedResponseCount;
    private final GUID guid;
    
    OOBSession(SecurityToken token, int requestedResponseCount, GUID guid) {
        this.token = token;
        this.requestedResponseCount = requestedResponseCount;
        this.urnHashCodes = new IntSet(requestedResponseCount);
        this.guid = guid;
        start = System.currentTimeMillis();
	}
    
    GUID getGUID() {
        return guid;
    }
	
    /**
     * Counts the responses uniquely. 
     */
    int countAddedResponses(Response[] responses) {
        int added = 0;
        for (Response response : responses) {
            Set<URN> urns = response.getUrns();
            if (!urns.isEmpty()) {
                added += urnHashCodes.add(urns.iterator().next().hashCode()) ? 1 : 0;
            }
            else {
                // create lazily since responses should have urns
                if (responseHashCodes == null) {
                    responseHashCodes = new IntSet();
                }
                added += responseHashCodes.add(response.hashCode()) ? 1 : 0;
            }
        }
        
        // inspection-related code
        responseTimestamps.add(System.currentTimeMillis());
        responseCounts.add(responses.length);
        addedResponses.add(added);
        // end inspection-related code
        
        return added;
    }
    
    /**
     * Returns the number of results that are still expected to come in.
     */
    final int getRemainingResultsCount() {
        return requestedResponseCount - urnHashCodes.size() - (responseHashCodes != null ? responseHashCodes.size() : 0); 
    }
    
	@Override
    public boolean equals(Object o) {
		if (! (o instanceof OOBSession))
			return false;
		OOBSession other = (OOBSession) o;
		return Arrays.equals(token.getBytes(), other.token.getBytes());
	}
    
	@Override
    public Object inspect() {
        Map<String,Object> ret = new HashMap<String,Object>();
        ret.put("start",start);
        ret.put("urnh",urnHashCodes.size());
        if (responseHashCodes != null)
            ret.put("rhh",responseHashCodes.size());
        ret.put("rrc",requestedResponseCount);
        ret.put("timestamps",responseTimestamps);
        ret.put("rcounts",responseCounts);
        ret.put("added",addedResponses);
        return ret;
    }
}