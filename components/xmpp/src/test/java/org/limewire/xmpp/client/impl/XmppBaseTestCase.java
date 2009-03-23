package org.limewire.xmpp.client.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.limewire.common.LimeWireCommonModule;
import org.limewire.http.auth.LimeWireHttpAuthModule;
import org.limewire.inject.AbstractModule;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.LimeWireNetTestModule;
import org.limewire.net.address.AddressEvent;
import org.limewire.util.BaseTestCase;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;

/**
 * Base class for all tests xmpp 
 */
public abstract class XmppBaseTestCase extends BaseTestCase {

    protected static final String SERVICE = "gmail.com";
    protected static final int SLEEP = 5000; // Milliseconds

    private ServiceRegistry registry;
    protected XMPPServiceImpl service;
    protected Injector injector;

    protected AddressEventTestBroadcaster addressEventBroadcaster;

    public XmppBaseTestCase(String name) {
        super(name);
    }


    protected void setUp() throws Exception {
        super.setUp();
        injector = createInjector(getModules());
        registry = injector.getInstance(ServiceRegistry.class);
        registry.initialize();
        registry.start();
        service = injector.getInstance(XMPPServiceImpl.class);
        service.setMultipleConnectionsAllowed(true);
    }

    protected Injector createInjector(Module... modules) {
        return Guice.createInjector(Stage.PRODUCTION, modules);
    }

    private Module[] getModules() {
        List<Module> modules = new ArrayList<Module>();
        modules.add(new LimeWireCommonModule());
        modules.add(new LimeWireHttpAuthModule());
        modules.addAll(getServiceModules());
        return modules.toArray(new Module[modules.size()]);
    }

    protected List<Module> getServiceModules() {
        Module xmppModule = new LimeWireXMPPTestModule();
        addressEventBroadcaster = new AddressEventTestBroadcaster();
        Module m = new AbstractModule() {
            protected void configure() {
                bind(new TypeLiteral<ListenerSupport<AddressEvent>>(){}).toInstance(addressEventBroadcaster);
                bind(XMPPConnectionListenerMock.class);
            }
        };
        return Arrays.asList(xmppModule, m, new LimeWireNetTestModule());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        service.stop();
        registry.stop();
        Thread.sleep(SLEEP);
    }
}
