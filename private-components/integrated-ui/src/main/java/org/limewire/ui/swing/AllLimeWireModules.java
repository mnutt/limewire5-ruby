package org.limewire.ui.swing;

import com.google.inject.AbstractModule;

/**
 * This exists only as an easy modules that installs all LW modules.
 * It should not be used in normal program flow.
 */
public class AllLimeWireModules extends AbstractModule {
    @Override
    protected void configure() {
        install(new LimeWireModule());
        install(new LimeWireSwingUiModule());
    }

}
