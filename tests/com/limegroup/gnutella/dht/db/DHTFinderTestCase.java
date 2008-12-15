package com.limegroup.gnutella.dht.db;

import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.util.MojitoUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTTestCase;
import com.limegroup.gnutella.dht.DHTTestUtils;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.MockUtils;

public abstract class DHTFinderTestCase extends DHTTestCase {

    
    protected Mockery context;
    protected DHTManager dhtManager;
    protected List<MojitoDHT> dhts;
    protected MojitoDHT mojitoDHT;
    protected NetworkManagerStub networkManager;
    protected Injector injector;
    protected AltLocValueFactory altLocValueFactory;
    protected PushProxiesValueFactory pushProxiesValueFactory;
    protected PushEndpointFactory pushEndpointFactory;
    protected AlternateLocationFactory alternateLocationFactory;

    public DHTFinderTestCase(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        DHTTestUtils.setSettings(PORT);
             
        context = new Mockery();
        dhtManager = context.mock(DHTManager.class);
        
        networkManager = new NetworkManagerStub();

        // to have non-empty push proxies to send
        final ConnectionManager connectionManager = MockUtils.createConnectionManagerWithPushProxies(context);
        
        injector = LimeTestUtils.createInjector(LocalSocketAddressProviderStub.STUB_MODULE, new AbstractModule() {
            @Override
            protected void configure() {
                bind(DHTManager.class).toInstance(dhtManager);
                bind(NetworkManager.class).toInstance(networkManager);
                bind(ConnectionManager.class).toInstance(connectionManager);
            }
        });
        DHTTestUtils.setLocalIsPrivate(injector, false);
        
        altLocValueFactory = injector.getInstance(AltLocValueFactory.class);
        alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
        pushProxiesValueFactory = injector.getInstance(PushProxiesValueFactory.class);
        pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
        
        
        dhts = MojitoUtils.createBootStrappedDHTs(1);
        
        mojitoDHT = dhts.get(0);
        context.checking(new Expectations() {{
            allowing(dhtManager).get(with(any(EntityKey.class)));
            will(new CustomAction("Mojito Get") {
                public Object invoke(Invocation invocation) throws Throwable {
                    return mojitoDHT.get((EntityKey)invocation.getParameter(0));
                }                
            });
        }});
        assertTrue(mojitoDHT.isBootstrapped());

        // register necessary factories
        mojitoDHT.getDHTValueFactoryManager().addValueFactory(AbstractAltLocValue.ALT_LOC, altLocValueFactory);
        mojitoDHT.getDHTValueFactoryManager().addValueFactory(AbstractPushProxiesValue.PUSH_PROXIES, pushProxiesValueFactory);
    }
    
    @Override
    protected void tearDown() throws Exception {
        for (MojitoDHT dht : dhts) {
            dht.close();
        }
    }

}
