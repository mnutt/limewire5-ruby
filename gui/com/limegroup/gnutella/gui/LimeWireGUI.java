package com.limegroup.gnutella.gui;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.limegroup.gnutella.LimeWireCore;

@Singleton
public class LimeWireGUI {
    
    private final Injector injector;
    private final LimeWireCore limewireCore;
    
    @Inject
    LimeWireGUI(Injector injector, LimeWireCore limewireCore) {
        this.injector = injector;
        this.limewireCore = limewireCore;
    }
    
    public Injector getInjector() {
        return injector;
    }
    
    public LimeWireCore getLimeWireCore() {
        return limewireCore;
    }
    
    public LocalClientInfoFactory getLocalClientInfoFactory() {
        return injector.getInstance(LocalClientInfoFactoryImpl.class);
    }
    

}
