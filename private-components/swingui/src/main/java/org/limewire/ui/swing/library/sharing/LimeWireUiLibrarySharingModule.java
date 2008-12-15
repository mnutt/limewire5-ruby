package org.limewire.ui.swing.library.sharing;

import com.google.inject.AbstractModule;

public class LimeWireUiLibrarySharingModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(ShareWidgetFactory.class).to(ShareWidgetFactoryImpl.class);
    }
}
