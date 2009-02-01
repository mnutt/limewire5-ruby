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
 * A filter that blocks anomalous queries that meet the following criteria:
 * 1) The first four bytes of the GUID match a large fraction of recently
 *    seen queries
 * 2) The query does not ask for OOB replies, so the first four bytes of
 *    the GUID are not being used to encode the OOB reply address
 * 3) There are no flags set in the minimum speed field
 */
@Singleton
public class AnomalousQueryFilter implements SpamFilter {
    
    private static final Log LOG =
        LogFactory.getLog(AnomalousQueryFilter.class);

    private static final int PREFIXES_TO_COUNT = 100;
    private static final double MAX_FRACTION_PER_PREFIX = 0.25;
    
    private int total = 0;
    private final LinkedHashMap<Integer, Integer> prefixCounts =
        new LinkedHashMap<Integer, Integer>(PREFIXES_TO_COUNT, 0.75f, true) {
            @Override
            public boolean removeEldestEntry(Map.Entry<Integer, Integer> e) {
                if(size() > PREFIXES_TO_COUNT) {
                    total -= e.getValue();
                    return true;
                }
                return false;
            }
        };
    
    @Override
    public boolean allow(Message m) {
        if(m instanceof QueryRequest) {
            QueryRequest q = (QueryRequest)m;
            // The prefix is the first four bytes of the GUID
            Integer prefix = ByteUtils.leb2int(q.getGUID(), 0);
            // Count how often we've seen each prefix recently
            Integer count = prefixCounts.get(prefix);
            if(count == null)
                count = 0;
            count++;
            total++;
            prefixCounts.put(prefix, count);
            // Drop the query if we've seen enough queries to make a judgement
            // and the query matches all the criteria 
            if(total >= PREFIXES_TO_COUNT &&
                    count > total * MAX_FRACTION_PER_PREFIX &&
                    !q.desiresOutOfBandReplies() && q.getMinSpeed() == 0) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Dropping anomalous query " + q + ", counted " +
                            count + "/" + total);
                return false;
            }
        }
        return true;
    }
}