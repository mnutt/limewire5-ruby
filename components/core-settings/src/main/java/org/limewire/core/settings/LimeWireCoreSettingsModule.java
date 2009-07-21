package org.limewire.core.settings;

import java.util.Properties;

import org.limewire.facebook.service.settings.ChatChannel;
import org.limewire.facebook.service.settings.FacebookAPIKey;
import org.limewire.facebook.service.settings.FacebookAppID;
import org.limewire.facebook.service.settings.FacebookAuthServerUrls;
import org.limewire.facebook.service.settings.FacebookReportBugs;
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
        bind(new TypeLiteral<MutableProvider<String>>(){}).annotatedWith(ChatChannel.class).toInstance(FacebookSettings.CHAT_CHANNEL);
        bind(new TypeLiteral<String[]>(){}).annotatedWith(FacebookAuthServerUrls.class).toProvider(FacebookSettings.AUTH_SERVER_URLS);
        bind(new TypeLiteral<String>(){}).annotatedWith(FacebookAPIKey.class).toProvider(FacebookSettings.API_KEY);
        bind(new TypeLiteral<String>(){}).annotatedWith(FacebookAppID.class).toProvider(FacebookSettings.APP_ID);
        bind(new TypeLiteral<Boolean>(){}).annotatedWith(FacebookReportBugs.class).toProvider(FacebookSettings.REPORT_BUGS);
    }
}
