package com.limegroup.gnutella.http;

import java.net.InetSocketAddress;
import java.net.Socket;

import junit.framework.Test;

import org.apache.http.nio.NHttpConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.http.reactor.DefaultDispatchedIOReactor;
import org.limewire.net.SocketsManager;
import org.limewire.util.BaseTestCase;

import com.google.inject.Injector;
import com.limegroup.gnutella.Acceptor;

public class HttpIOReactorTest extends BaseTestCase {

    private static final int ACCEPTOR_PORT = 9999;

    private static Acceptor acceptor;


    private BasicHttpParams params;

    private SocketsManager socketsManager;

    public HttpIOReactorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HttpIOReactorTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        
        acceptor = injector.getInstance(Acceptor.class);
        acceptor.start();
        acceptor.setListeningPort(ACCEPTOR_PORT);
        
        params = new BasicHttpParams();
        params.setIntParameter(HttpConnectionParams.SO_TIMEOUT, 2222)
               .setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 1111)
               .setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024)
               .setBooleanParameter(HttpConnectionParams.STALE_CONNECTION_CHECK, false)
               .setBooleanParameter(HttpConnectionParams.TCP_NODELAY, true)
               .setParameter(HttpProtocolParams.USER_AGENT, "TEST-SERVER/1.1");
        
        socketsManager = injector.getInstance(SocketsManager.class);
    }

    @Override
    protected void tearDown() throws Exception {
        acceptor.shutdown();
    }

    // TODO: this is not really testing what i think it should:
    //       if it wants to test that Acceptor accepted something,
    //       it should add a ConnectionAcceptor into ConnectionDispatcher,
    //       connect to the Acceptor, and make sure that ConnectionAcceptor
    //       got it -- but then it's really a test of Acceptor, not HttpIOReactor.
    //       What I think this *wants* to test is that HttpIOReactor can
    //       take a pre-connected socket and do things to it.  That would involve
    //       setting up a fake server & connecting to it, then handing off the
    //       connection to the reactor.  It looks like Acceptor is doubling as
    //       the fake server here, which isn't quite right (and makes the test
    //       hard to understand).  Even still, after it hands it off, there's
    //       no real test going on to make sure the reactor did the right thing.
    public void testAcceptConnection() throws Exception {
        HttpTestServer server = new HttpTestServer(params);
        server.execute(null);
        DefaultDispatchedIOReactor reactor = server.getReactor();
        
        Socket socket = socketsManager.connect(new InetSocketAddress("localhost", ACCEPTOR_PORT), 500);
        try {
            NHttpConnection conn = reactor.acceptConnection(null, socket);
            assertNotNull(conn.getContext().getAttribute(DefaultDispatchedIOReactor.IO_SESSION_KEY));
            assertEquals(2222, socket.getSoTimeout());
        } finally {
            socket.close();
        }
    }
}
