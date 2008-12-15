package org.limewire.ui.swing.library.nav;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiLibraryNavModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(NavPanelFactory.class).toProvider(FactoryProvider.newFactory(NavPanelFactory.class, NavPanel.class));
        bind(LibraryNavigator.class).to(LibraryNavigatorImpl.class);
    }
}
