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

package org.limewire.mojito.settings;

import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;

/**
 * Settings for the bootstrapping process.
 */
public class BootstrapSettings extends MojitoProps {
    
    private BootstrapSettings() {}
    
    /**
     * The maximum number of bootstrap failures before bootstrapping 
     * is given up.
     */
    public static final IntSetting MAX_BOOTSTRAP_FAILURES
        = FACTORY.createIntSetting("MAX_BOOTSTRAP_FAILURES", 40);

    
    /**
     * The maximum amount of time the bootstrapping process can take
     * before it's interrupted.
     */
    public static final LongSetting BOOTSTRAP_TIMEOUT
        = FACTORY.createRemoteLongSetting("BOOTSTRAP_TIMEOUT", 
                8L*60L*1000L, "Mojito.BootstrapTimeout", 60L*1000L, 30L*60L*1000L);
    
    /**
     * The IS_BOOTSTRAPPED_RATIO is used to determinate if a Node's RouteTable
     * is good enough to say it's bootstrapped.
     */
    public static final FloatSetting IS_BOOTSTRAPPED_RATIO
        = FACTORY.createRemoteFloatSetting("IS_BOOTSTRAPPED_RATIO", 
                0.5f, "Mojito.IsBootstrappedRatio", 0f, 1.0f);
    
    /**
     * Number of bootstrap workers to spawn.
     */
    public static final IntSetting BOOTSTRAP_WORKERS =
        FACTORY.createRemoteIntSetting("BOOSTRAP_WORKERS", 1, 
                "Mojito.BooststrapWorkers", 1, 5);
    
    /**
     * Enabled or disables the second part of the bootstrapping process that
     * does a full Bucket refresh to fill up the RouteTable.
     */
    //public static final BooleanSetting REFRESH_ALL_BUCKETS
    //    = FACTORY.createRemoteBooleanSetting("REFRESH_ALL_BUCKETS", 
    //            true, "Mojito.RefreshAllBuckets");
    
    /**
     * Setting for how many Buckets should be refreshed at most
     * during bootstrapping. Ideally we want to refresh them all
     * but it takes too long and leads to bootstrap failures if
     * there's a big number of Buckets.
     */
    //public static final IntSetting MAX_BUCKETS_TO_REFRESH
    //    = FACTORY.createRemoteIntSetting("MAX_BUCKETS_TO_REFRESH", 
    //            10, "Mojito.MaxBucketsToRefresh", 0, Integer.MAX_VALUE);
    
    /**
     * Returns the lock timeout for the BootstrapProcess.
     */
    public static long getWaitOnLock(boolean hasInitialNode) {
        long waitOnLock = 0L;
        
        // 1) Ping Nodes to find initial bootstrap Node
        if (!hasInitialNode) {
            waitOnLock += PingSettings.getWaitOnLock();
        }
        
        // 2) Do a lookup for your own Node ID
        waitOnLock += LookupSettings.getWaitOnLock(true);
        
        // 3) Refresh all Buckets
        waitOnLock += ContextSettings.getWaitOnLock(
                BootstrapSettings.BOOTSTRAP_TIMEOUT.getValue());
        
        return waitOnLock;
    }
}
