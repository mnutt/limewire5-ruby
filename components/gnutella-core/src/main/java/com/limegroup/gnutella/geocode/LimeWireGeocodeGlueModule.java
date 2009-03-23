package com.limegroup.gnutella.geocode;

import org.limewire.geocode.LimewireGeocodeModule;

import com.google.inject.AbstractModule;

public class LimeWireGeocodeGlueModule extends AbstractModule {
    
    @Override
    protected void configure() {
        install(new LimewireGeocodeModule(GeocoderImpl.class));
        bind(CachedGeoLocation.class).to(CachedGeoLocationImpl.class);
    }
    

}
