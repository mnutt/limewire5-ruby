package com.limegroup.gnutella.geocode;

import org.limewire.geocode.GeocodeInformation;

public interface CachedGeoLocation {

    /**
     * Returns geo code information lazily, possibly querying a server.
     * 
     * Should be called from a background thread that can block.
     * 
     * @return null if info could not be retrieved at the time or another
     * thread is currently retrieving it
     */
    GeocodeInformation getGeocodeInformation();

}