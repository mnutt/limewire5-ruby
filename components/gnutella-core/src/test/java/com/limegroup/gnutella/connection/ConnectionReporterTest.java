package com.limegroup.gnutella.connection;

import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

import org.apache.http.client.methods.HttpGet;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.connection.ConnectionLifecycleEventType;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.inject.AbstractModule;
import org.limewire.io.GUID;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.Clock;

import com.google.inject.Injector;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.util.LimeWireUtils;

public class ConnectionReporterTest extends TestCase {
    public ConnectionReporterTest(String name) {
        super(name);
    }
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }
    
    public void testReporting() {
        Mockery mockery = new Mockery();
        final Clock clock = mockery.mock(Clock.class);
        final ApplicationServices applicationServices = mockery.mock(ApplicationServices.class);
        final HttpExecutor httpExecutor = mockery.mock(HttpExecutor.class);
        final GUID guid = new GUID();
        final AtomicReference<HttpGet> atomicReference = new AtomicReference<HttpGet>();
        mockery.checking(new Expectations() {{
            allowing(applicationServices).getMyGUID();
            will(returnValue(guid.bytes()));
            one(httpExecutor).execute(with(any(HttpGet.class)));            
            will(new AssignParameterAction<HttpGet>(atomicReference, 0));
//            will(returnValue(new Shutdownable() {
//                @Override
//                public void shutdown() {
//                    
//                }
//            }));
            allowing(clock).now();
            will(onConsecutiveCalls(returnValue(1000l),
                    returnValue(5000l), returnValue(7000l)));
        }});
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(HttpExecutor.class).toInstance(httpExecutor);
                bind(ApplicationServices.class).toInstance(applicationServices);
                bind(Clock.class).toInstance(clock);
            }
        });
        ConnectionReporter connectionReporter = injector.getInstance(ConnectionReporter.class);
        assertNull(atomicReference.get());
        connectionReporter.handleConnectionLifecycleEvent(new ConnectionLifecycleEvent(new Object(), ConnectionLifecycleEventType.CONNECTING));
        assertNull(atomicReference.get());
        connectionReporter.handleConnectionLifecycleEvent(new ConnectionLifecycleEvent(new Object(), ConnectionLifecycleEventType.CONNECTION_INITIALIZED));
        assertEquals(LimeWireUtils.addLWInfoToUrl(ConnectionReporter.REPORTING_URL, guid.bytes()) +
                            "&connect_time=4000", atomicReference.get().getURI().toASCIIString());
        // mockery expectation exception if additional http requests
        // are made upon subsequent ConnectionLifecycleEvents
        for(ConnectionLifecycleEventType eventType : ConnectionLifecycleEventType.values()) {
            connectionReporter.handleConnectionLifecycleEvent(new ConnectionLifecycleEvent(new Object(), eventType));            
        }
        mockery.assertIsSatisfied();
    }
}
