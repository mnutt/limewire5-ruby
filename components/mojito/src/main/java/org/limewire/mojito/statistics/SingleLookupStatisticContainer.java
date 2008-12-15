/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package org.limewire.mojito.statistics;

import java.io.IOException;
import java.io.Writer;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;


abstract class SingleLookupStatisticContainer extends StatisticContainer {
    
    protected final KUID lookupKey;
    private final GlobalLookupStatisticContainer globalLookupStats;
    
    protected SingleLookupStatisticContainer(Context context, KUID lookupKey) {
        this.lookupKey = lookupKey;
        this.globalLookupStats = context.getGlobalLookupStats();
        globalLookupStats.addSingleLookupStatistic(this);
    }
    
    public void setHops(int hops, boolean findValue) {
        if(findValue) {
            globalLookupStats.GLOBAL_FIND_VALUE_LOOKUP_HOPS.addData(hops);
            globalLookupStats.GLOBAL_FIND_VALUE_LOOKUP_HOPS.storeCurrentStat();
        }
        globalLookupStats.GLOBAL_LOOKUP_HOPS.addData(hops);
        globalLookupStats.GLOBAL_LOOKUP_HOPS.storeCurrentStat();
    }
    
    public void setTime(int time, boolean findValue) {
        if(findValue) {
            globalLookupStats.GLOBAL_FIND_VALUE_LOOKUP_TIME.addData(time);
            globalLookupStats.GLOBAL_FIND_VALUE_LOOKUP_TIME.storeCurrentStat();
        }
        globalLookupStats.GLOBAL_LOOKUP_TIME.addData(time);
        globalLookupStats.GLOBAL_LOOKUP_TIME.storeCurrentStat();
    }
    
    public void addRequest() {
        globalLookupStats.GLOBAL_LOOKUP_REQUESTS.incrementStat();
    }

    public void addReply() {
        globalLookupStats.GLOBAL_LOOKUP_REPLIES.incrementStat();
    }
    
    public void addTimeout() {
        globalLookupStats.GLOBAL_LOOKUP_TIMEOUTS.incrementStat();
    }
    
    @Override
    public void writeStats(Writer writer) throws IOException {
        writer.write("Lookup: "+lookupKey+"\n");
        super.writeStats(writer);
    }
    
}
