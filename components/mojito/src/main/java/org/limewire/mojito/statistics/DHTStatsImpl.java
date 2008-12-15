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
import java.util.List;

import org.limewire.mojito.KUID;


class DHTStatsImpl implements DHTStats {

    private KUID nodeId;
    
    private List<StatisticContainer> containers = new ArrayList<StatisticContainer>();
    
    public DHTStatsImpl(KUID nodeId) {
        this.nodeId = nodeId;
    }
    
    public synchronized void addStatisticContainer(StatisticContainer container) {
        containers.add(container);
    }

    public synchronized void dump(Writer writer, boolean writeSingLookups) throws IOException {
        writer.write(nodeId + "\n");
        for (StatisticContainer c : containers) {
            if(!writeSingLookups && (c instanceof GlobalLookupStatisticContainer)) {
                ((GlobalLookupStatisticContainer)c).writeGlobalStats(writer);
            } else {
                c.writeStats(writer);
            }
        }
        writer.write("--------------------------------------------\n");
        writer.flush();
    }
}
