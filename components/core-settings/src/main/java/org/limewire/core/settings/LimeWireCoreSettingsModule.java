package org.limewire.core.settings;

import java.util.Properties;

import org.limewire.geocode.GeoLocation;
import org.limewire.geocode.GeocodeUrl;
import org.limewire.inject.AbstractModule;
import org.limewire.inject.MutableProvider;

import com.google.inject.TypeLiteral;

public class LimeWireCoreSettingsModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(String.class).annotatedWith(GeocodeUrl.class).toProvider(GeocodeSettings.GEOCODE_URL);
        bind(new TypeLiteral<MutableProvider<Properties>>(){}).annotatedWith(GeoLocation.class).toInstance(GeocodeSettings.GEO_LOCATION);
    }
}
