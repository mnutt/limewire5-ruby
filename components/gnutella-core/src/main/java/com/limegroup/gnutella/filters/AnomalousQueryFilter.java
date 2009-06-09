package com.limegroup.gnutella.filters;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.ByteUtils;

import com.google.inject.Singleton;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * A filter that blocks anomalous queries where any four-byte slice of the GUID
 * matches a large fraction of recently seen queries.
 */
@Singleton
public class AnomalousQueryFilter implements SpamFilter {

    private static final Log LOG =
        LogFactory.getLog(AnomalousQueryFilter.class);

    // Package access for testing
    static final int GUIDS_TO_COUNT = 100;
    static final float MAX_FRACTION_PER_SLICE = 0.25f;

    private int[] sliceTotals = new int[4];
    // The keys of this map contain two integer values packed into a long:
    // the position of the slice within the GUID (0 to 3) and the slice's value
    private final LinkedHashMap<Long, Integer> sliceCounts =
        new LinkedHashMap<Long, Integer>(GUIDS_TO_COUNT * 4, 0.75f, true) {
        @Override
        // Limited-size map with LRU eviction policy
        public boolean removeEldestEntry(Map.Entry<Long, Integer> e) {
            if(size() > GUIDS_TO_COUNT * 4) {
                int position = (int)(e.getKey() >> 32);
                sliceTotals[position] -= e.getValue();
                return true;
            }
            return false;
        }
    };

    @Override
    public boolean allow(Message m) {
        if(m instanceof QueryRequest) {
            QueryRequest q = (QueryRequest)m;
            boolean shouldDrop = false;
            // Count each four-byte slice of the GUID
            byte[] guid = q.getGUID();
            for(int i = 0; i < 4; i++) {
                int slice = ByteUtils.leb2int(guid, i * 4);
                // Pack the position and value of the slice into a long
                Long key = ((long)i << 32) | (slice & 0xFFFFFFFFL);
                // Count how often we've seen each slice recently
                Integer count = sliceCounts.get(key);
                if(count == null)
                    count = 0;
                count++;
                sliceTotals[i]++;
                sliceCounts.put(key, count);
                // Drop the query if we've seen enough queries to make a
                // judgement and it matches a large fraction of recent queries 
                if(sliceTotals[i] >= GUIDS_TO_COUNT &&
                        count > sliceTotals[i] * MAX_FRACTION_PER_SLICE) {
                    // Count the other slices before returning
                    shouldDrop = true;
                }
            }
            if(shouldDrop) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Dropping anomalous query " + q);
                return false;
            }
        }
        return true;
    }
}