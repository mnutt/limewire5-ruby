package com.limegroup.gnutella.connection;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManagerImpl;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.UploadServices;

/**
 * Tests the class that checks whether or not the user has a live internet
 * connection.
 */
public class ConnectionCheckerTest extends BaseTestCase {

    private Mockery context;

    private ConnectionServices connectionServices;

    private UploadServices uploadServices;

    private DownloadServices downloadServices;

    private SocketsManager socketsManager;

    private UDPConnectionChecker udpConnectionChecker;

    private ConnectionCheckerListener connectionCheckerListener;

    public ConnectionCheckerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ConnectionCheckerTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();

        connectionServices = context.mock(ConnectionServices.class);
        uploadServices = context.mock(UploadServices.class);
        downloadServices = context.mock(DownloadServices.class);
        socketsManager = new SocketsManagerImpl();
        udpConnectionChecker = context.mock(UDPConnectionChecker.class);
        connectionCheckerListener = context.mock(ConnectionCheckerListener.class);
    }

    public void testForLiveConnection() throws Exception {
        // setup mocks
        AtomicInteger numWorkarounds = new AtomicInteger();
        String[] hosts = { "www.limewire.org" };

        context.checking(new Expectations() {
            {
                never(connectionCheckerListener).noInternetConnection();
                one(connectionCheckerListener).connected();
            }
        });

        // run test
        ConnectionChecker checker = new ConnectionChecker(numWorkarounds, hosts,
                connectionServices, uploadServices, downloadServices, socketsManager,
                udpConnectionChecker);
        checker.run(connectionCheckerListener);

        assertTrue(checker.hasConnected());

        context.assertIsSatisfied();
    }

    /**
     * Now, we "pretend" we're disconnected by just trying to connect to hosts
     * that don't exist, which is effectively the same as not being connected.
     */
    public void testNonExistingHosts() throws Exception {
        // setup mocks
        AtomicInteger numWorkarounds = new AtomicInteger();
        String[] hosts = { "http://www.dummyhostsjoafds.com", "http://www.dummyhostsjoafdser.com",
                "http://www.dumfadfostsjoafds.com", "http://www.dummyhostsjafds.com",
                "http://www.dummyhostjoafdser.com" };

        context.checking(new Expectations() {
            {
                one(connectionCheckerListener).noInternetConnection();
                never(connectionCheckerListener).connected();
            }
        });

        // run test
        ConnectionChecker checker = new ConnectionChecker(numWorkarounds, hosts,
                connectionServices, uploadServices, downloadServices, socketsManager,
                udpConnectionChecker);
        checker.run(connectionCheckerListener);

        assertFalse(checker.hasConnected());

        context.assertIsSatisfied();
    }

    public void testNonExistingHostsSP2Workaround() throws Exception {
        // setup mocks
        AtomicInteger numWorkarounds = new AtomicInteger();
        String[] hosts = { "http://www.dummyhostsjoafds.com", "http://www.dummyhostsjoafdser.com",
                "http://www.dumfadfostsjoafds.com" };

        context.checking(new Expectations() {
            {
                one(connectionCheckerListener).noInternetConnection();
                never(connectionCheckerListener).connected();
                one(downloadServices).hasActiveDownloads();
                will(returnValue(false));
                one(uploadServices).hasActiveUploads();
                will(returnValue(false));
                one(udpConnectionChecker).udpIsDead();
                will(returnValue(false));
            }
        });

        // run test
        MyConnectionChecker checker = new MyConnectionChecker(numWorkarounds, hosts,
                connectionServices, uploadServices, downloadServices, socketsManager,
                udpConnectionChecker);
        checker.shouldTrySP2Workaround = true;
        checker.run(connectionCheckerListener);

        assertFalse(checker.hasConnected());
        assertTrue(checker.triedSP2Workaround);

        context.assertIsSatisfied();
    }

    private class MyConnectionChecker extends ConnectionChecker {

        private boolean shouldTrySP2Workaround;
        private boolean triedSP2Workaround;

        public MyConnectionChecker(AtomicInteger numWorkarounds, String[] hosts,
                ConnectionServices connectionServices, UploadServices uploadServices,
                DownloadServices downloadServices, SocketsManager socketsManager,
                UDPConnectionChecker udpConnectionChecker) {
            super(numWorkarounds, hosts, connectionServices, uploadServices, downloadServices,
                    socketsManager, udpConnectionChecker);
        }

        @Override
        boolean shouldTrySP2Workaround() {
            return shouldTrySP2Workaround;
        }

        @Override
        void trySP2Workaround() {
            triedSP2Workaround = true;
        }

    }

}
