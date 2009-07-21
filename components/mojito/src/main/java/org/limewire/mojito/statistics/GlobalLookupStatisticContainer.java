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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.limewire.mojito.KUID;
import org.limewire.statistic.Statistic;


public class GlobalLookupStatisticContainer extends StatisticContainer {
    
    private static final int MAX_LOOKUPS = 20;
    
    private List<StatisticContainer> singleLookups = new ArrayList<StatisticContainer>();
    

    public GlobalLookupStatisticContainer(KUID nodeId) {
        super(nodeId);
    }
    
    /**
     * <tt>Statistic</tt> for the number of lookups.
     */
    public Statistic GLOBAL_LOOKUPS = new SimpleStatistic();
    
    /**
    * <tt>Statistic</tt> for all outgoing lookup messages.
    */
   public Statistic GLOBAL_LOOKUP_REQUESTS = new SimpleStatistic();
   
   /**
    * <tt>Statistic</tt> for all incoming lookup messages.
    */
   public Statistic GLOBAL_LOOKUP_REPLIES = new SimpleStatistic();
   
   /**
    * <tt>Statistic</tt> for timeouts
    */
   public Statistic GLOBAL_LOOKUP_TIMEOUTS = new SimpleStatistic();
   
   /**
    * <tt>Statistic</tt> for the number of hops.
    */
   public Statistic GLOBAL_LOOKUP_HOPS = new SimpleStatistic();
   
   /**
    * <tt>Statistic</tt> for the time.
    */
   public Statistic GLOBAL_LOOKUP_TIME = new SimpleStatistic();
   
   /**
    * <tt>Statistic</tt> for the FIND_VALUE times.
    */
   public Statistic GLOBAL_FIND_VALUE_LOOKUP_TIME = new SimpleStatistic();
   
   /**
    * <tt>Statistic</tt> for the FIND_VALUE hops.
    */
   public Statistic GLOBAL_FIND_VALUE_LOOKUP_HOPS = new SimpleStatistic();
   
   public void addSingleLookupStatistic(SingleLookupStatisticContainer lookupStat) {
       synchronized (singleLookups) {
           GLOBAL_LOOKUPS.incrementStat();
           singleLookups.add(lookupStat);
           if (singleLookups.size() > MAX_LOOKUPS) {
               singleLookups.remove(0);
           }
       }
   }
   
   @Override
public void writeStats(Writer writer) throws IOException {
       writer.write("Global lookups: \n");
       super.writeStats(writer);
       synchronized (singleLookups) {
           for (Iterator iter = singleLookups.iterator(); iter.hasNext();) {
               StatisticContainer stat = (StatisticContainer) iter.next();
               stat.writeStats(writer);
           }
       }
   }
   
   public void writeGlobalStats(Writer writer) throws IOException {
       writer.write("Global lookups: \n");
       super.writeStats(writer);
   }

}
