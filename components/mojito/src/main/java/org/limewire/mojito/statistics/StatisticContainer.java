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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.limewire.mojito.KUID;
import org.limewire.statistic.Statistic;


class StatisticContainer {
    
    public StatisticContainer() {};
    
    public StatisticContainer(KUID nodeId) {
        DHTStatsManager.getInstance(nodeId).addStatisticContainer(this);
    }
    
    public void writeStats(Writer writer) throws IOException {
        
        List<Field> fields = new ArrayList<Field>();

        Class superclass = getClass().getSuperclass();
        Class declaringClass = getClass().getDeclaringClass();
        
        if (superclass != null) {
            fields.addAll(Arrays.asList(superclass.getFields()));
        }
        
        if (declaringClass != null) {
            fields.addAll(Arrays.asList(declaringClass.getFields()));
        }
        
        fields.addAll(Arrays.asList(getClass().getFields()));
        
        for (Field field : fields) {
            try {
                if(Modifier.isTransient(field.getModifiers())){
                    continue;
                }
                
                Object value = field.get(this);
                if (!(value instanceof Statistic)) {
                    continue;
                }
                
                Statistic stat = (Statistic) value;
                writer.write(field.getName() + "\t");
                stat.storeStats(writer);
                writer.write("\n");
                
            } catch(IllegalAccessException e) {
                continue;
            }
        }
    }
}
