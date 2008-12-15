package com.limegroup.gnutella.gui;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.limegroup.gnutella.LimeWireCoreModule;

/** The master LimeWire module. */
public class LimeWireModule implements Module {

    public void configure(Binder binder) {
        binder.install(new LimeWireCoreModule());
        binder.install(new LimeWireGUIModule());
    }

}
