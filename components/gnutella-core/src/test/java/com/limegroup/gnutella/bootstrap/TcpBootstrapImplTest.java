package com.limegroup.gnutella.bootstrap;

import java.net.URI;
import java.util.ArrayList;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.Endpoint;

public class TcpBootstrapImplTest extends LimeTestCase {

    final static String addr1 = "1.2.3.4:5678";
    final static String addr2 = "9.10.11.12:1314";
    final static int serverPort = 50505;
    final static URI serverUri = URI.create("http://localhost:" + serverPort);
    final static String emptyResponse = "\r\n\r\n";
    final static String twoHostResponse = addr1 + "\r\n" + addr2 + "\r\n\r\n";

    Mockery context;
    ConnectionServices connectionServices;
    TcpBootstrapImpl tcpBootstrap;
    Bootstrapper.Listener listener;
    TestBootstrapServer server;
    ArrayList<Endpoint> endpoints;

    public TcpBootstrapImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(TcpBootstrapImplTest.class);
    }

    @Override
    public void setUp() throws Exception {
        context = new Mockery();
        connectionServices = context.mock(ConnectionServices.class);
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConnectionServices.class).toInstance(connectionServices);
            }
        });
        tcpBootstrap = injector.getInstance(TcpBootstrapImpl.class);
        listener = context.mock(Bootstrapper.Listener.class);
        server = new TestBootstrapServer(serverPort);
        endpoints = new ArrayList<Endpoint>();
        endpoints.add(new Endpoint(addr1, true));
        endpoints.add(new Endpoint(addr2, true));
    }

    @Override
    public void tearDown() throws Exception {
        server.shutdown();
    }

    public void testGetsNoEndpoints() throws Exception {
        context.checking(new Expectations() {{
            allowing(connectionServices).isConnected();
            will(returnValue(false));
        }});
        server.setResponseData(emptyResponse);
        assertTrue(tcpBootstrap.add(serverUri));
        assertTrue(tcpBootstrap.fetchHosts(listener));
        Thread.sleep(500);
        context.assertIsSatisfied();        
    }

    public void testGetsSomeEndpoints() throws Exception {
        context.checking(new Expectations() {{
            allowing(connectionServices).isConnected();
            will(returnValue(false));
            one(listener).handleHosts(endpoints);
            will(returnValue(2));
        }});
        server.setResponseData(twoHostResponse);
        assertTrue(tcpBootstrap.add(serverUri));
        assertTrue(tcpBootstrap.fetchHosts(listener));
        Thread.sleep(500);
        context.assertIsSatisfied();
    }

    public void testRemembersAttemptedServers() throws Exception {
        context.checking(new Expectations() {{
            allowing(connectionServices).isConnected();
            will(returnValue(false));
            one(listener).handleHosts(endpoints);
            will(returnValue(2));
        }});
        server.setResponseData(twoHostResponse);
        assertTrue(tcpBootstrap.add(serverUri));
        assertTrue(tcpBootstrap.fetchHosts(listener));
        Thread.sleep(500);
        // No more servers to try - fetchHosts() should return false
        assertFalse(tcpBootstrap.fetchHosts(listener));
        context.assertIsSatisfied();
    }
    
    public void testRemembersFailedServers() throws Exception {
        context.checking(new Expectations() {{
            allowing(connectionServices).isConnected();
            will(returnValue(false));
        }});
        URI wrongUri = new URI("http://localhost" + (serverPort + 1));
        assertTrue(tcpBootstrap.add(wrongUri));
        assertTrue(tcpBootstrap.fetchHosts(listener));
        Thread.sleep(500);
        // No more servers to try - fetchHosts() should return false
        assertFalse(tcpBootstrap.fetchHosts(listener));        
        context.assertIsSatisfied();
    }
    
    public void testDuplicatesNotAdded() throws Exception {
        assertTrue(tcpBootstrap.add(serverUri));
        assertFalse(tcpBootstrap.add(serverUri));
    }
}
