package org.limewire.ui.swing.player;

import com.google.inject.AbstractModule;

public class LimeWireUiPlayerModule extends AbstractModule {

    @Override
    protected void configure() {
      //TODO:  this is not the best way to handle player access but it gets it working for now.
        requestStaticInjection(PlayerUtils.class);
    }

}
