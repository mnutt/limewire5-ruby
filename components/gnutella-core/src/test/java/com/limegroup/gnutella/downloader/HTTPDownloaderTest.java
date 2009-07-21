package com.limegroup.gnutella.downloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import junit.framework.Test;

import org.jmock.Mockery;
import org.limewire.collection.BitNumbers;
import org.limewire.collection.Function;
import org.limewire.collection.MultiIterable;
import org.limewire.collection.Range;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.inject.Providers;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.net.address.StrictIpPortSet;
import org.limewire.nio.NIOSocket;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.StringUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.gnutella.BandwidthManager;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointCache;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SelfEndpoint;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.http.SimpleReadHeaderState;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.statistics.TcpBandwidthStatistics;
import com.limegroup.gnutella.stubs.IOStateObserverStub;
import com.limegroup.gnutella.stubs.ReadBufferChannel;
import com.limegroup.gnutella.tigertree.ThexReaderFactory;
import com.limegroup.gnutella.util.MockUtils;

public class HTTPDownloaderTest extends org.limewire.gnutella.tests.LimeTestCase {

    private HTTPDownloaderFactory httpDownloaderFactory;

    private NetworkManager networkManager;

    private AlternateLocationFactory alternateLocationFactory;

    private DownloadManager downloadManager;

    private CreationTimeCache creationTimeCache;

    private BandwidthManager bandwidthManager;

    private PushEndpointCache pushEndpointCache;

    private VerifyingFileFactory verifyingFileFactory;

    private Mockery context;

    private PushEndpointFactory pushEndpointFactory;
    
    private RemoteFileDescFactory remoteFileDescFactory;
    
    private TcpBandwidthStatistics tcpBandwidthStatistics;
    
    private NetworkInstanceUtils networkInstanceUtils;

    public HTTPDownloaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HTTPDownloaderTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }

    private Injector setupInjector(Module... modules) {
		Injector injector = LimeTestUtils.createInjector(modules);
		remoteFileDescFactory = injector.getInstance(RemoteFileDescFactory.class);
		networkManager = injector.getInstance(NetworkManager.class);
		alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
		downloadManager = injector.getInstance(DownloadManager.class);
		creationTimeCache = injector.getInstance(CreationTimeCache.class);
		bandwidthManager = injector.getInstance(BandwidthManager.class);
		pushEndpointCache = injector.getInstance(PushEndpointCache.class);
		alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
		verifyingFileFactory = injector.getInstance(VerifyingFileFactory.class);
		pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
		tcpBandwidthStatistics = injector.getInstance(TcpBandwidthStatistics.class);
		networkInstanceUtils = injector.getInstance(NetworkInstanceUtils.class);
		
        
		httpDownloaderFactory = new SocketlessHTTPDownloaderFactory(networkManager,
                alternateLocationFactory, downloadManager, creationTimeCache, bandwidthManager,
                Providers.of(pushEndpointCache), pushEndpointFactory, remoteFileDescFactory,
                injector.getInstance(ThexReaderFactory.class), tcpBandwidthStatistics,
                networkInstanceUtils);

        return injector;
    }

    /**
     * Tests if X-FW-Node-Info header is written. Must not have accepted an incoming
     * connection for this to happen.
     */
    public void testFWNodeInfoHeaderIsWritten() throws Exception {
        final NetworkManagerStub networkManagerStub = new NetworkManagerStub();
        networkManagerStub.setCanDoFWT(true);
        final ConnectionManager connectionManager = MockUtils.createConnectionManagerWithPushProxies(context);
        Injector injector = setupInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConnectionManager.class).toInstance(connectionManager);
                bind(NetworkManager.class).toInstance(networkManagerStub);
            }
        });
        SelfEndpoint selfEndPpoint = injector.getInstance(SelfEndpoint.class);

        // precondition
        assertFalse(networkManager.acceptedIncomingConnection());
        assertTrue(networkManager.canDoFWT());

        Map<String, String> headers = getWrittenHeaders(new Function<HTTPDownloader, Void>() {
            public Void apply(HTTPDownloader dl) {
                return null;
            }
        });

        assertEquals(selfEndPpoint.httpStringValue(), headers.get(HTTPHeaderName.FW_NODE_INFO
                .httpStringValue()));
        assertTrue(headers.get(HTTPHeaderName.FEATURES.httpStringValue()).contains(HTTPConstants.BROWSE_PROTOCOL));
        // should be not set since node is firewalled
        assertNull(headers.get(HTTPHeaderName.NODE.httpStringValue()));
        context.assertIsSatisfied();
    }

    /**
     * Tests that FWT-Node header is not written if client is not firewalled.
     */
    public void testFWTNodeHeaderIsNotWritten() throws Exception {
        final NetworkManagerStub networkManagerStub = new NetworkManagerStub();
        setupInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManagerStub);
            }
        });
        networkManagerStub.setAcceptedIncomingConnection(true);
        networkManagerStub.setAddress(new byte[] { (byte) 129, 34, 4, 5 });

        assertFalse(networkInstanceUtils.isPrivateAddress(networkManagerStub.getAddress()));

        Map<String, String> headers = getWrittenHeaders(new Function<HTTPDownloader, Void>() {
            public Void apply(HTTPDownloader dl) {
                return null;
            }
        });

        assertNull(headers.get(HTTPHeaderName.FW_NODE_INFO.httpStringValue()));
        // should be there, since not firewalled and address not private
        assertNotNull(headers.get(HTTPHeaderName.NODE.httpStringValue()));
    }

    public void testWrittenAltHeadersWithTLS() throws Exception {
        setupInjector();
        final Set<IpPort> sT, fT, sN, fN;
        sT = new IpPortSet();
        fT = new IpPortSet();
        sN = new IpPortSet();
        fN = new IpPortSet();

        Map<String, String> headers = getWrittenHeaders(new Function<HTTPDownloader, Void>() {
            public Void apply(HTTPDownloader dl) {
                try {
                    // Add some alternate locations (succesful & not, TLS &
                    // not).
                    // Do it randomly, but force atleast 1 TLS in both failed &
                    // success
                    // (Go from 20 -> 40 so we all have the same # of digits.)
                    for (int i = 20; i < 40; i++) {
                        boolean tls = false;
                        boolean failed = false;
                        AlternateLocation loc;
                        if (i == 25 || i == 30 || Math.random() < 0.5) {
                            loc = alternateLocationFactory.create("1.2.3." + i, UrnHelper.URNS[0],
                                    true);
                            tls = true;
                        } else {
                            loc = alternateLocationFactory.create("1.2.3." + i, UrnHelper.URNS[0],
                                    false);
                        }

                        if (i != 30 && (i == 25 || Math.random() < 0.5)) {
                            failed = true;
                            dl.addFailedAltLoc(loc);
                        } else {
                            dl.addSuccessfulAltLoc(loc);
                        }

                        IpPort host = ((DirectAltLoc) loc).getHost();
                        if (tls && failed)
                            fT.add(host);
                        else if (tls)
                            sT.add(host);
                        else if (failed)
                            fN.add(host);
                        else
                            sN.add(host);
                    }
                } catch (IOException iox) {
                    throw new RuntimeException(iox);
                }

                return null;
            }
        });

        // Verify NAlts has all the correct IPs.
        String nalts = headers.get("X-NAlt");
        assertFalse("shouldn't have tls indexes!", nalts.contains("tls"));
        for (IpPort ipp : new MultiIterable<IpPort>(fT, fN)) {
            assertTrue("couldn't find: " + ipp + ", in: " + nalts, nalts.contains(ipp
                    .getInetAddress().getHostAddress()));
            nalts = nalts.replace(ipp.getInetAddress().getHostAddress(), "");
        }
        // Remove all commas too...
        String removedCommas = nalts.replace(",", "").trim();
        assertEquals("wrong nalts leftover! " + nalts, "", removedCommas);

        // Verify Alts has all the correct IPs
        String originalAlts = headers.get("X-Alt");
        assertTrue("no tls indexes!", originalAlts.startsWith("tls="));
        // Remove the TLS= index for right now..
        String alts = originalAlts.substring(originalAlts.indexOf(",") + 1);
        // verify all the IPs exist
        for (IpPort ipp : new MultiIterable<IpPort>(sT, sN)) {
            assertTrue("couldn't find: " + ipp + ", in: " + originalAlts, alts.contains(ipp
                    .getInetAddress().getHostAddress()));
            alts = alts.replace(ipp.getInetAddress().getHostAddress(), "");
        }
        // Remove all commas too...
        removedCommas = alts.replace(",", "").trim();
        assertEquals("wrong alts leftover! " + alts, "", removedCommas);

        // Verify the tls= index is correct...
        alts = originalAlts.substring(originalAlts.indexOf(",") + 1);
        StringTokenizer tokenizer = new StringTokenizer(alts, ",");
        List<IpPort> ips = new ArrayList<IpPort>();
        while (tokenizer.hasMoreTokens()) {
            String ip = tokenizer.nextToken();
            ips.add(new IpPortImpl(ip, 6346));
        }
        BitNumbers bn = new BitNumbers(ips.size());
        for (int i = 0; i < ips.size(); i++) {
            if (sT.contains(ips.get(i)))
                bn.set(i);
        }
        assertTrue(originalAlts.startsWith("tls=" + bn.toHexString()));
    }

    public void testReadXAltsWithTLS() throws Exception {
        setupInjector();
        String str;
        HTTPDownloader dl;
        Collection<RemoteFileDesc> receivedLocations;

        str = "HTTP/1.1 200 OK\r\n"
                + "X-Alt: tls=AB8,1.2.3.4:5,4.3.2.1,2.3.4.5:6,5.4.3.2:1,3.4.5.6:7,6.5.4.3:2,4.5.6.7:8,7.6.5.4:3,5.6.7.8:9,8.7.6.5:4\r\n";
        dl = newHTTPDownloaderWithHeader(str);
        assertEquals(0, dl.getLocationsReceived().size());

        readHeaders(dl);

        receivedLocations = dl.getLocationsReceived();
        assertEquals(10, receivedLocations.size());
        dl.stop();

        Set<Connectable> tlsExpected = new StrictIpPortSet<Connectable>(new ConnectableImpl("1.2.3.4:5", true),
                new ConnectableImpl("2.3.4.5:6", true), new ConnectableImpl("3.4.5.6:7", true), new ConnectableImpl("4.5.6.7:8", true),
                new ConnectableImpl("7.6.5.4:3", true), new ConnectableImpl("5.6.7.8:9", true));
        Set<Connectable> normalExpected = new StrictIpPortSet<Connectable>(new ConnectableImpl("4.3.2.1:6346", false), new ConnectableImpl(
                "5.4.3.2:1", false), new ConnectableImpl("6.5.4.3:2", false), new ConnectableImpl("8.7.6.5:4", false));

        Set<Address> allLocs = new HashSet<Address>(toAddresses(receivedLocations));
        allLocs.retainAll(tlsExpected);
        assertEquals(allLocs, tlsExpected);
        for (Address address : allLocs)
            assertTrue(((Connectable)address).isTLSCapable());

        allLocs = new HashSet<Address>(toAddresses(receivedLocations));
        allLocs.retainAll(normalExpected);
        assertEquals(normalExpected, allLocs);
        for (Address address : allLocs)
            assertFalse(((Connectable)address).isTLSCapable());

        allLocs = new HashSet<Address>(toAddresses(receivedLocations));
        allLocs.removeAll(tlsExpected);
        allLocs.removeAll(normalExpected);
        assertTrue(allLocs.isEmpty());
    }
    
    public void testRetryAfter() throws Exception {
        setupInjector();
        String headers = "HTTP/1.1 200 OK\r\nRetry-After: 120\r\n";
        HTTPDownloader dl = newHTTPDownloaderWithHeader(headers);
        readHeaders(dl);
        dl.stop();
        assertWaitTime(120, dl.getContext());
    }

    public void testMinRetryTime() throws Exception {
        setupInjector();
        String headers = "HTTP/1.1 200 OK\r\nRetry-After: 50\r\n";
        HTTPDownloader dl = newHTTPDownloaderWithHeader(headers);
        readHeaders(dl);
        dl.stop();
        assertWaitTime(60, dl.getContext());
    }

    public void testMaxRetryTime() throws Exception {
        setupInjector();
        String headers = "HTTP/1.1 200 OK\r\nRetry-After: 3610\r\n";
        HTTPDownloader dl = newHTTPDownloaderWithHeader(headers);
        readHeaders(dl);
        dl.stop();
        assertWaitTime(3600, dl.getContext());
    }

    private void assertWaitTime(int seconds, RemoteFileDescContext context)
    throws Exception {
        // RemoteFileDescContext.getWaitTime() adds a second, but we'll also
        // accept one second less because some time may have elapsed since
        // parsing the header
        int wait = context.getWaitTime(System.currentTimeMillis());
        assertGreaterThan(seconds - 1, wait);
        assertLessThan(seconds + 2, wait);
    }
    
    private static Collection<? extends Address> toAddresses(Collection<? extends RemoteFileDesc> rfds) {
        List<Address> list = new ArrayList<Address>();
        for (RemoteFileDesc rfd : rfds) {
            list.add(rfd.getAddress());
        }
        return list;
    }

    public void testParseContentRange() throws Throwable {
        setupInjector();
        int length = 1000;
        RemoteFileDesc rfd = remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl("1.2.3.4", 1, false), 1, "file", length, new byte[16], 1,
                2, false, null, URN.NO_URN_SET, false, "LIME", -1);
        File f = new File("sam");
        VerifyingFile vf = verifyingFileFactory.createVerifyingFile(length);
        vf.open(f);
        HTTPDownloader dl = httpDownloaderFactory.create(null, new RemoteFileDescContext(rfd), vf, false);

        PrivilegedAccessor.setValue(dl, "_amountToRead", new Long(rfd.getSize()));

        assertEquals(Range.createRange(1, 9), parseContentRange(dl, "Content-range: bytes 1-9/10"));

        assertEquals(Range.createRange(1, 9), parseContentRange(dl, "Content-range:bytes=1-9/10"));

        // should this work? the server says the size is 10, we think it's
        // 1000. throw IllegalArgumentException or ProblemReadingHeader?
        assertEquals(Range.createRange(0, 999), parseContentRange(dl, "Content-range:bytes */10"));

        assertEquals(Range.createRange(0, 999), parseContentRange(dl, "Content-range:bytes */*"));

        assertEquals(Range.createRange(1, 9), parseContentRange(dl, "Content-range:bytes 1-9/*"));

        // expect exception for invalid header requests
        try {
            assertEquals(Range.createRange(0, 9), parseContentRange(dl, "Content-range:bytes 1-10/10"));
        } catch (IOException ie) {
            assertInstanceof(ProblemReadingHeaderException.class, ie);
        }

        assertEquals(Range.createRange(0, 0), parseContentRange(dl, "Content-range:bytes 0-0/1"));
        
        try {
            parseContentRange(dl, "Content-range:bytes -1-9/10");
            fail("Should have thrown exception");
        } catch (ProblemReadingHeaderException ignored) {
        }
        
        Range iv = null;
        try {
            iv = parseContentRange(dl, "Content-range:bytes 1 10 10");
            fail("Parsed invalid content range.  Got: " + iv);
        } catch (ProblemReadingHeaderException ignored) {
        }

        // low is less than high
        try {
            iv = parseContentRange(dl, "Content-range:bytes 10-9/*");
            fail("Parsed invalid content range.  Got: " + iv);
        } catch (ProblemReadingHeaderException ignored) {
        }

        // negative values.
        try {
            iv = parseContentRange(dl, "Content-range: bytes -10--5/*");
            fail("Parsed invalid content range.  Got: " + iv);
        } catch (ProblemReadingHeaderException ignored) {
        }

        // negative high.
        try {
            iv = parseContentRange(dl, "Content-range:bytes 0--10/*");
            fail("Parsed invalid content range.  Got: " + iv);
        } catch (ProblemReadingHeaderException ignored) {
        }
    }

    public void testLegacy() throws Throwable {
        setupInjector();
        // readHeaders tests
        String str;
        HTTPDownloader down;

        str = "HTTP/1.1 200 OK\r\n";
        down = newHTTPDownloaderWithHeader(str);
        readHeaders(down);
        down.stop();

        str = "HTTP/1.1 301 Moved Permanently\r\n";
        down = newHTTPDownloaderWithHeader(str);
        try {
            readHeaders(down);
            down.stop();
            fail("exception should have been thrown");
        } catch (IOException e) {
        }

        str = "HTTP/1.1 300 Multiple Choices\r\n";
        down = newHTTPDownloaderWithHeader(str);
        try {
            readHeaders(down);
            down.stop();
            fail("exception should have been thrown");
        } catch (IOException e) {
        }

        str = "HTTP/1.1 404 File Not Found \r\n";
        down = newHTTPDownloaderWithHeader(str);
        try {
            readHeaders(down);
            down.stop();
            fail("exception should have been thrown");
        } catch (FileNotFoundException e) {
        }

        str = "HTTP/1.1 410 Not Sharing \r\n";
        down = newHTTPDownloaderWithHeader(str);
        try {
            readHeaders(down);
            down.stop();
            fail("exception should have been thrown");
        } catch (NotSharingException e) {
        }

        str = "HTTP/1.1 412 \r\n";
        down = newHTTPDownloaderWithHeader(str);
        try {
            readHeaders(down);
            down.stop();
            fail("exception should have been thrown");
        } catch (IOException e) {
        }

        str = "HTTP/1.1 503 \r\n";
        down = newHTTPDownloaderWithHeader(str);
        try {
            readHeaders(down);
            down.stop();
            fail("exception should have been thrown");
        } catch (TryAgainLaterException e) {
        }

        str = "HTTP/1.1 210 \r\n";
        down = newHTTPDownloaderWithHeader(str);
        readHeaders(down);
        down.stop();

        str = "HTTP/1.1 204 Partial Content\r\n";
        down = newHTTPDownloaderWithHeader(str);
        readHeaders(down);
        down.stop();

        str = "HTTP/1.1 200 OK\r\nUser-Agent: LimeWire\r\n";
        down = newHTTPDownloaderWithHeader(str);
        readHeaders(down);
        down.stop();

        str = "200 OK\r\n";
        down = newHTTPDownloaderWithHeader(str);
        try {
            readHeaders(down);
            down.stop();
            fail("exception should have been thrown.");
        } catch (NoHTTPOKException e) {
        }
    }

    private static Range parseContentRange(HTTPDownloader dl, String s) throws Throwable {
        try {
            return (Range) PrivilegedAccessor.invokeMethod(dl, "parseContentRange",
                    s);
        } catch (Exception e) {
            if (e.getCause() != null)
                throw e.getCause();
            else
                throw e;
        }
    }

    private Map<String, String> getWrittenHeaders(Function<HTTPDownloader, Void> func)
            throws Exception {
        ServerSocket server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress(0));

        RemoteFileDesc rfd = remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl("127.0.0.1", server.getLocalPort(), false), 1, "file", 1000, new byte[16], 1,
                1, false, null, UrnHelper.URN_SETS[0], false, "TEST", -1);

        VerifyingFile vf = verifyingFileFactory.createVerifyingFile(1000);

        Socket socket = new NIOSocket("127.0.0.1", server.getLocalPort());
        Socket accept = server.accept();

        HTTPDownloader dl = httpDownloaderFactory.create(socket, new RemoteFileDescContext(rfd), vf, false);
        func.apply(dl);

        dl.initializeTCP();
        IOStateObserverStub observer = new IOStateObserverStub();
        dl.connectHTTP(0, 500, true, observer);
        observer.waitForFinish();

        InputStream in = accept.getInputStream();
        byte[] read = new byte[5000];
        int amtRead = in.read(read);
        assertGreaterThan(0, amtRead);

        String headers = StringUtils.getASCIIString(read, 0, amtRead);
        return parseHeaders(headers, "GET /uri-res/N2R?" + UrnHelper.URNS[0].httpStringValue()
                + " HTTP/1.1");
    }

    private Map<String, String> parseHeaders(String headers, String firstLine) throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader(headers));
        String line = reader.readLine();
        Map<String, String> map = new HashMap<String, String>();
        assertEquals("GET /uri-res/N2R?" + UrnHelper.URNS[0].httpStringValue() + " HTTP/1.1", line);
        while ((line = reader.readLine()) != null && line.length() != 0) {
            int colon = line.indexOf(":");
            assertNotEquals("couldn't find colon, line is: " + line, -1, colon);
            map.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
        }
        return map;
    }

    private HTTPDownloader newHTTPDownloaderWithHeader(String s) throws Exception {
        s += "\r\n";
        SimpleReadHeaderState reader = new SimpleReadHeaderState(null, 100, 2048);
        reader.process(new ReadBufferChannel(StringUtils.toAsciiBytes(s)), ByteBuffer.allocate(1024));
        RemoteFileDesc rfd = remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl("127.0.0.1", 1, false), 1, "file", 1000, new byte[16], 1, 1, false,
                null, UrnHelper.URN_SETS[0], false, "TEST", -1);
        HTTPDownloader d = httpDownloaderFactory.create(null, new RemoteFileDescContext(rfd), null, false);
        PrivilegedAccessor.setValue(d, "_headerReader", reader);
        return d;
    }

    private static void readHeaders(HTTPDownloader d) throws Exception {
        d.parseHeaders();
    }
}
