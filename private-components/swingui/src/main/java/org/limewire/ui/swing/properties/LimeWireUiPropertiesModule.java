package org.limewire.ui.swing.properties;

import com.google.inject.AbstractModule;


public class LimeWireUiPropertiesModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FilterList.class).to(FilterListImpl.class);
    }
}
