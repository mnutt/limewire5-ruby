package org.limewire.core.settings;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Collections;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.facebook.service.settings.ChatChannel;
import org.limewire.facebook.service.settings.FacebookAPIKey;
import org.limewire.facebook.service.settings.FacebookAppID;
import org.limewire.facebook.service.settings.FacebookAuthServerUrls;
import org.limewire.facebook.service.settings.FacebookReportBugs;
import org.limewire.facebook.service.settings.FacebookURLs;
import org.limewire.facebook.service.settings.InspectionsServerUrls;
import org.limewire.geocode.GeoLocation;
import org.limewire.geocode.GeocodeUrl;
import org.limewire.inject.AbstractModule;
import org.limewire.inject.MutableProvider;
import org.limewire.setting.StringSetting;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.internal.ImmutableMap;

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
        bind(new TypeLiteral<Map<String, Provider<String>>>(){}).annotatedWith(FacebookURLs.class).toProvider(FacebookURLsMapProvider.class);
        bind(new TypeLiteral<Map<String, StringSetting>>(){}).annotatedWith(InspectionsServerUrls.class).toProvider(InspectionsURLsMapProvider.class);   
    }

    @Singleton
    private static class FacebookURLsMapProvider extends AbstractLazySingletonProvider<Map<String, Provider<String>>> {
        
        @Inject
        public FacebookURLsMapProvider() {
        }

        @Override
        protected Map<String, Provider<String>> createObject() {
            Map<String, Provider<String>> map = new HashMap<String, Provider<String>>();
            map.put(FacebookURLs.HOME_PAGE_URL, FacebookSettings.HOME_PAGE_URL);
            map.put(FacebookURLs.PRESENCE_POPOUT_PAGE_URL, FacebookSettings.PRESENCE_POPOUT_PAGE_URL);
            map.put(FacebookURLs.CHAT_SETTINGS_URL, FacebookSettings.CHAT_SETTINGS_URL);
            map.put(FacebookURLs.RECONNECT_URL, FacebookSettings.RECONNECT_URL);
            map.put(FacebookURLs.LOGOUT_URL, FacebookSettings.LOGOUT_URL);
            map.put(FacebookURLs.SEND_CHAT_URL, FacebookSettings.SEND_CHAT_URL);
            map.put(FacebookURLs.SEND_CHAT_STATE_URL, FacebookSettings.SEND_CHAT_STATE_URL);
            map.put(FacebookURLs.UPDATE_PRESENCES_URL, FacebookSettings.UPDATE_PRESENCES_URL);
            map.put(FacebookURLs.RECEIVE_CHAT_URL, FacebookSettings.RECEIVE_CHAT_URL);
            return Collections.unmodifiableMap(map);        
        }
    }
    
    @Singleton
    private static class InspectionsURLsMapProvider extends AbstractLazySingletonProvider<Map<String, StringSetting>> {
        @Inject public InspectionsURLsMapProvider() {}

        @Override
        protected Map<String, StringSetting> createObject() {
            return ImmutableMap.of(
                InspectionsServerUrls.INSPECTION_SPEC_REQUEST_URL, InspectionsSettings.INSPECTION_SPEC_REQUEST_URL, 
                InspectionsServerUrls.INSPECTION_SPEC_SUBMIT_URL, InspectionsSettings.INSPECTION_SPEC_SUBMIT_URL);        
        }
    }
}
