package org.limewire.core.impl.player;

import org.limewire.player.api.AudioPlayer;

import com.google.inject.AbstractModule;

public class MockPlayerModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(AudioPlayer.class).to(MockAudioPlayer.class);
    }

}
