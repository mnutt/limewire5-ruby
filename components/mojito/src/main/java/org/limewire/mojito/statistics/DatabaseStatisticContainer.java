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

import org.limewire.mojito.KUID;
import org.limewire.statistic.Statistic;



public class DatabaseStatisticContainer extends StatisticContainer {

    public DatabaseStatisticContainer(KUID nodeId) {
        super(nodeId);
    }

    @Override
    public void writeStats(Writer writer) throws IOException {
        writer.write("Database Stats:\n");
        super.writeStats(writer);
    }
    
    /**
     * <tt>Statistic</tt> for the number of store forwards
     */
    public Statistic STORE_FORWARD_COUNT = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the number of values removed du to store forward
     */
    public Statistic STORE_FORWARD_REMOVALS = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for values that expire
     */
    public Statistic EXPIRED_VALUES = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for stored values
     */
    public Statistic STORED_VALUES = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for retrieved values
     */
    public Statistic RETRIEVED_VALUES = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for removed values
     */
    public Statistic REMOVED_VALUES = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for republished values
     */
    public Statistic REPUBLISHED_VALUES = new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for whether or not we're close to a Key
     */
    public Statistic NOT_MEMBER_OF_CLOSEST_SET = new SimpleStatistic();
}
