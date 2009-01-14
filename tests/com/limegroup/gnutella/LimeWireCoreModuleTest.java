package com.limegroup.gnutella;

import junit.framework.TestCase;

import org.limewire.net.ConnectionDispatcher;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Names;

public class LimeWireCoreModuleTest extends TestCase {

    public void testConnectionDispatcher() {
        Injector injector = Guice.createInjector(new LimeWireCoreModule(ActivityCallbackAdapter.class));
        ConnectionDispatcher globalInstance = injector.getInstance(Key.get(ConnectionDispatcher.class, Names.named("global")));
        ConnectionDispatcher localInstance = injector.getInstance(Key.get(ConnectionDispatcher.class, Names.named("local")));
        
        assertNotSame(globalInstance, localInstance);
        
        Provider<ConnectionDispatcher> localProvider = injector.getProvider(Key.get(ConnectionDispatcher.class, Names.named("local")));
        assertSame(localInstance, localProvider.get());
        assertSame(localProvider.get(), localProvider.get());

        Provider<ConnectionDispatcher> globalProvider = injector.getProvider(Key.get(ConnectionDispatcher.class, Names.named("global")));
        assertSame(globalInstance, globalProvider.get());
        assertSame(globalProvider.get(), globalProvider.get());
    }
}
