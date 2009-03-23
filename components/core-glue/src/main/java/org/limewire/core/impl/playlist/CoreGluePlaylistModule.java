package org.limewire.core.impl.playlist;

import org.limewire.core.api.playlist.PlaylistManager;

import com.google.inject.AbstractModule;

/**
 * Guice module to configure the Playlist API for the live core. 
 */
public class CoreGluePlaylistModule extends AbstractModule {

    /**
     * Configures the Playlist API for the live core. 
     */
    @Override
    protected void configure() {
        bind(PlaylistManager.class).to(PlaylistManagerImpl.class);
    }

}
