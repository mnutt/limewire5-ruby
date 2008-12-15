package com.limegroup.gnutella.statistics;

import java.util.HashMap;
import java.util.Map;

import org.limewire.collection.NumericBuffer;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectionPoint;
import org.limewire.util.ByteUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.LifecycleManager;

/**
 * Keeps track and reports some statistics about local queries.
 */
@Singleton
public class QueryStats {
    
    private NumericBuffer<Long> times = new NumericBuffer<Long>(200);
    
    private final LifecycleManager lifecycleManager;
    
    @Inject
    public QueryStats(LifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }
    
    public synchronized void recordQuery() {
        times.add(System.currentTimeMillis());
    }
    
    public synchronized long getLastQueryTime() {
        if (times.isEmpty())
            return 0;
        return times.first();
    }

    @InspectionPoint("user query stats v3")
    @SuppressWarnings("unused")
    private final Inspectable queryStats = new Inspectable() {
        public Object inspect() {
            synchronized(QueryStats.this) {
                Map<String, Object> ret = new HashMap<String, Object>();
                /*
                 * version 1 used to send 10 search times in milliseconds
                 * version 2 used to try to minimize return size
                 */
                ret.put("ver",3);

                byte[] b = new byte[times.getSize() * 4];
                for (int i = 0; i < times.getSize(); i++) {
                    int time = Float.floatToIntBits((times.get(i)-lifecycleManager.getStartFinishedTime()) / 1000f);
                    ByteUtils.int2beb(time, b, i*4);
                }

                ret.put("buf",b);
                return ret;
            }
        }
    };
    
}
