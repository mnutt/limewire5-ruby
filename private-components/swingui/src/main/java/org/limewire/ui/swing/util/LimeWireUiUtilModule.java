package org.limewire.ui.swing.util;


import com.google.inject.AbstractModule;

public class LimeWireUiUtilModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(NonBlockFileIconController.class).to(BasicFileIconController.class);
        bind(PropertiableHeadings.class).to(PropertiableHeadingsImpl.class);
        bind(SaveLocationExceptionHandler.class).to(SaveLocationExceptionHandlerImpl.class);
        bind(MagnetHandler.class);
    }
}