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
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.RouteTable.RouteTableEvent;
import org.limewire.mojito.routing.RouteTable.RouteTableListener;
import org.limewire.mojito.routing.RouteTable.RouteTableEvent.EventType;
import org.limewire.statistic.Statistic;


public class RoutingStatisticContainer extends StatisticContainer {

    /**
     * <tt>Statistic</tt> for the total number of contacts added to the main
     * routing table
     */
    public Statistic NODE_COUNT = new SimpleStatistic();

    /**
     * <tt>Statistic</tt> for the total number of LIVE contacts added to the
     * main routing table
     */
    public Statistic LIVE_NODE_COUNT = new NodeCountStatistic();

    /**
     * <tt>Statistic</tt> for the total number of UNKNOWN contacts added to
     * the main routing table
     */
    public Statistic UNKNOWN_NODE_COUNT = new NodeCountStatistic();

    /**
     * <tt>Statistic</tt> for the number of contacts added to the main routing
     * table
     */
    public Statistic BUCKET_COUNT = new SimpleStatistic();

    /**
     * <tt>Statistic</tt> for the number of bucket refreshes
     */
    public Statistic BUCKET_REFRESH_COUNT = new SimpleStatistic();

    /**
     * <tt>Statistic</tt> for the number of replacement contacts added
     */
    public Statistic REPLACEMENT_COUNT = new SimpleStatistic();

    /**
     * <tt>Statistic</tt> for the number of dead contacts
     */
    public Statistic DEAD_NODE_COUNT = new SimpleStatistic();

    /**
     * <tt>Statistic</tt> for contacts trying to spoof node ids
     */
    public Statistic SPOOF_COUNT = new SimpleStatistic();
    
    public RoutingStatisticContainer(KUID nodeId) {
        super(nodeId);
    }
    
    @Override
    public void writeStats(Writer writer) throws IOException {
        writer.write("Routing Stats:\n");
        super.writeStats(writer);
    }

    protected class NodeCountStatistic extends SimpleStatistic{

        @Override
        public void incrementStat() {
            super.incrementStat();
            NODE_COUNT.incrementStat();
        }
    }
    
    public static class Listener implements RouteTableListener {

        private RoutingStatisticContainer routingStats;
        
        public void handleRouteTableEvent(RouteTableEvent event) {
            if (routingStats == null) {
                RouteTable routeTable = event.getRouteTable();
                routingStats = new RoutingStatisticContainer(routeTable.getLocalNode().getNodeID());
            }
            
            if (event.getEventType().equals(EventType.ADD_ACTIVE_CONTACT)) {
                if (event.getContact().isAlive()) {
                    routingStats.LIVE_NODE_COUNT.incrementStat();
                } else {
                    routingStats.UNKNOWN_NODE_COUNT.incrementStat();
                }
            } else if (event.getEventType().equals(EventType.ADD_CACHED_CONTACT)) {
                routingStats.REPLACEMENT_COUNT.incrementStat();
            } else if (event.getEventType().equals(EventType.REMOVE_CONTACT)) {
                if (event.getContact().isDead()) {
                    routingStats.DEAD_NODE_COUNT.incrementStat();
                }
            } else if (event.getEventType().equals(EventType.REPLACE_CONTACT)) {
                routingStats.LIVE_NODE_COUNT.incrementStat();
                
                if (event.getContact().isDead()) {
                    routingStats.DEAD_NODE_COUNT.incrementStat();
                }
            } else if (event.getEventType().equals(EventType.SPLIT_BUCKET)) {
                // Increment only by one 'cause splitting a Bucket
                // creates only one new Bucket
                routingStats.BUCKET_COUNT.incrementStat();
            }
        }
    }
}
