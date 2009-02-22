package org.limewire.xmpp.client.impl;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.limewire.util.BaseTestCase;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.net.LimeWireNetModule;
import org.limewire.net.ProxySettings;
import org.limewire.net.EmptyProxySettings;
import org.limewire.net.SocketBindingSettings;
import org.limewire.net.EmptySocketBindingSettings;
import org.limewire.net.address.AddressEvent;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.common.LimeWireCommonModule;
import org.limewire.http.auth.LimeWireHttpAuthModule;
import org.limewire.inject.AbstractModule;
import org.limewire.listener.ListenerSupport;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Guice;
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


    class LimeWireNetTestModule extends LimeWireNetModule {
        @Override
        protected void configure() {
            super.configure();
            bind(ProxySettings.class).to(EmptyProxySettings.class);
            bind(SocketBindingSettings.class).to(EmptySocketBindingSettings.class);
            bind(NetworkInstanceUtils.class).to(SimpleNetworkInstanceUtils.class);
        }
    }
}
