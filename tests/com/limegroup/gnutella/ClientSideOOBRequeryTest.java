package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.downloader.TestFile;
import com.limegroup.gnutella.downloader.TestUploader;
import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactory;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.NetworkManagerStub;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class ClientSideOOBRequeryTest extends ClientSideTestCase {
    
    private static final int UPLOADER_PORT = 10000;
    
    private MyCallback callback;

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket[] UDP_ACCESS;

    private NetworkManagerStub networkManagerStub;

    private FileManager fileManager;

    private MessagesSupportedVendorMessage messagesSupportedVendorMessage;

    private SearchServices searchServices;

    private ResponseFactory responseFactory;

    private QueryReplyFactory queryReplyFactory;

    private ReplyNumberVendorMessageFactory replyNumberVendorMessageFactory;

    private QueryRequestFactory queryRequestFactory;

    private DownloadServices downloadServices;

    private MessageRouter messageRouter;

    private AlternateLocationFactory alternateLocationFactory;

    private MessageFactory messageFactory;

    private PingReplyFactory pingReplyFactory;

    private OnDemandUnicaster onDemandUnicaster;
    
    private MACCalculatorRepositoryManager macManager;
    
    private RemoteFileDescFactory remoteFileDescFactory;

    private PushEndpointFactory pushEndpointFactory;

    public ClientSideOOBRequeryTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideOOBRequeryTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    public void setSettings() {
        TIMEOUT = 4000;
        ConnectionSettings.DO_NOT_BOOTSTRAP.setValue(true);
    }   
    
    @Override
    public void setUp() throws Exception {
        networkManagerStub = new NetworkManagerStub();
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, MyCallback.class, new LimeTestUtils.NetworkManagerStubModule(networkManagerStub));
        super.setUp(injector);
        
        fileManager = injector.getInstance(FileManager.class);
        messagesSupportedVendorMessage = injector.getInstance(MessagesSupportedVendorMessage.class);
        searchServices = injector.getInstance(SearchServices.class);
        responseFactory = injector.getInstance(ResponseFactory.class);
        queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        replyNumberVendorMessageFactory = injector.getInstance(ReplyNumberVendorMessageFactory.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        downloadServices = injector.getInstance(DownloadServices.class);
        messageRouter = injector.getInstance(MessageRouter.class);
        alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
        messageFactory = injector.getInstance(MessageFactory.class);
        pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        onDemandUnicaster = injector.getInstance(OnDemandUnicaster.class);
        callback = (MyCallback) injector.getInstance(ActivityCallback.class);
        macManager = injector.getInstance(MACCalculatorRepositoryManager.class);
        remoteFileDescFactory = injector.getInstance(RemoteFileDescFactory.class);
        pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
        
        networkManagerStub.setAcceptedIncomingConnection(true);
        networkManagerStub.setCanReceiveSolicited(true);
        networkManagerStub.setCanReceiveUnsolicited(true);
        networkManagerStub.setOOBCapable(true);
        networkManagerStub.setPort(SERVER_PORT);
        
        File file = TestUtils.getResourceFile("com/limegroup/gnutella/metadata/metadata.mp3");
        assertNotNull(fileManager.getGnutellaFileList().add(file).get(1, TimeUnit.SECONDS));
                
        UDP_ACCESS = new DatagramSocket[10];
        for (int i = 0; i < UDP_ACCESS.length; i++)
            UDP_ACCESS[i] = new DatagramSocket();

        for (int i = 0; i < testUP.length; i++) {
            assertTrue("should be open", testUP[i].isOpen());
            assertTrue("should be up -> leaf",
                testUP[i].getConnectionCapabilities().isSupernodeClientConnection());
            BlockingConnectionUtils.drain(testUP[i], 100);
            // OOB client side needs server side leaf guidance
            testUP[i].send(messagesSupportedVendorMessage);
            testUP[i].flush();
        }

        Thread.sleep(250);

        // we should now be guess capable and tcp incoming capable....
        // test on acceptor since network manager is stubbed out
//        assertTrue(injector.getInstance(Acceptor.class).acceptedIncoming());
    }
    
    public void testNoDownloadQueryDonePurge() throws Exception {

        
        // set smaller clear times so we can test in a timely fashion

        
        BlockingConnectionUtils.keepAllAlive(testUP, pingReplyFactory);
        // clear up any messages before we begin the test.
        drainAll();

        Message m = null;

        byte[] guid = searchServices.newQueryGUID();
        searchServices.query(guid, "whatever");
        // i need to pretend that the UI is showing the user the query still
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // ok, the leaf is sending OOB queries - good stuff, now we should send
        // a lot of results back and make sure it buffers the bypassed OOB ones
        for (int i = 0; i < testUP.length; i++) {
            Response[] res = new Response[200];
            for (int j = 0; j < res.length; j++)
                res[j] = responseFactory.createResponse(10+j+i, 10+j+i, "whatever "+ j + i, UrnHelper.SHA1);
            m = queryReplyFactory.createQueryReply(qr.getGUID(), (byte) 1, 6355,
                    myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                    true, true, false, false, null);
            testUP[i].send(m);
            testUP[i].flush();
        }
        
        // wait for processing
        Thread.sleep(2000);

        for (int i = 0; i < UDP_ACCESS.length; i++) {
            ReplyNumberVendorMessage vm = 
                replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(qr.getGUID()), i+1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            DatagramPacket pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUP[0].getInetAddress(), SERVER_PORT);
            UDP_ACCESS[i].send(pack);
        }

        // wait for processing
        Thread.sleep(1000);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            assertByPassedResultsCacheHasSize(qr.getGUID(), UDP_ACCESS.length);
        }

        {
            // now we should make sure MessageRouter clears the map
            searchServices.stopQuery(new GUID(qr.getGUID()));
            assertByPassedResultsCacheHasSize(qr.getGUID(), 0);
        }
        callback.clearGUID();
    }


    public void testDownloadDoneQueryDonePurge() throws Exception {

        BlockingConnectionUtils.keepAllAlive(testUP, pingReplyFactory);
        // clear up any messages before we begin the test.
        drainAll();

        // luckily there is hacky little way to go through the download paces -
        // download from yourself :) .

        Message m = null;

        byte[] guid = searchServices.newQueryGUID();
        searchServices.query(guid, "berkeley");
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // just return ONE real result and the rest junk
        {
            // get a correct response object
            QueryRequest qrTemp = queryRequestFactory.createQuery("berkeley");
            testUP[0].send(qrTemp);
            testUP[0].flush();
        }

        Response resp = null;
        QueryReply reply = null;
        reply = BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                           QueryReply.class);
        assertNotNull(reply);
        resp = (reply.getResultsAsList()).get(0);
        assertNotNull(resp);
        Response[] res = new Response[] { resp };

        // this isn't really needed but just for completeness send it back to 
        // the test Leaf
        m = queryReplyFactory.createQueryReply(guid, (byte) 1, SERVER_PORT,
                myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                true, true, false, false, null);
        testUP[0].send(m);
        testUP[0].flush();

        // send back a lot of results via TCP so you konw the UDP one will be
        // bypassed
        for (int i = 0; i < testUP.length; i++) {
            res = new Response[75];
            for (int j = 0; j < res.length; j++)
                res[j] = responseFactory.createResponse(10+j+i, 10+j+i, "berkeley "+ j + i, UrnHelper.SHA1);
            m = queryReplyFactory.createQueryReply(guid, (byte) 1,
                    testUP[0].getPort(), myIP(), 0, res, GUID.makeGuid(), new byte[0],
                    false, false, true, true, false, false, null);
            testUP[i].send(m);
            testUP[i].flush();
        }

        // allow for processing
        Thread.sleep(3000);

        {
            // now we should make sure MessageRouter has not bypassed anything
            // yet
            assertByPassedResultsCacheHasSize(qr.getGUID(), 0);
        }
        
        // send back a UDP response and make sure it was saved in bypassed...
        {
            ReplyNumberVendorMessage vm = 
                replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(guid), 1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            DatagramPacket pack = new DatagramPacket(baos.toByteArray(), 
                                                     baos.toByteArray().length,
                                                     testUP[0].getInetAddress(),
                                                     SERVER_PORT);
            UDP_ACCESS[0].send(pack);
        }

        // allow for processing
        Thread.sleep(500);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            assertByPassedResultsCacheHasSize(guid, 1);
        }
        
        // now do the download, wait for it to finish, and then bypassed results
        // should be empty again
        RemoteFileDesc rfd = resp.toRemoteFileDesc(reply, null, remoteFileDescFactory, pushEndpointFactory);
        assertFalse("file should not be saved yet", 
            new File( _savedDir, "berkeley.txt").exists());
        
        downloadServices.download(new RemoteFileDesc[] { rfd }, false, new GUID(guid));
        callback.clearGUID();
        
        // sleep to make sure the download starts 
        Thread.sleep(10000);
        
        assertTrue("file should saved", 
            new File( _savedDir, "berkeley.txt").exists());

        {
            // now we should make sure MessageRouter clears the map
            assertByPassedResultsCacheHasSize(qr.getGUID(), 0);
        }

    }

    private void assertByPassedResultsCacheHasSize(byte[] guid, int size) {
        Set<GUESSEndpoint> endpoints = messageRouter.getQueryLocs(new GUID(guid));
        assertEquals(size, endpoints.size());
    }

    public void testQueryAliveNoPurge() throws Exception {

        BlockingConnectionUtils.keepAllAlive(testUP, pingReplyFactory);
        // clear up any messages before we begin the test.
        drainAll();

        // luckily there is hacky little way to go through the download paces -
        // download from yourself :) .

        Message m = null;

        byte[] guid = searchServices.newQueryGUID();
        searchServices.query(guid, "berkeley");
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // just return ONE real result and the rest junk
        Response resp = null;
        QueryReply reply = null;
        {
            // get a correct response object
            QueryRequest qrTemp = queryRequestFactory.createQuery("berkeley");
            testUP[0].send(qrTemp);
            testUP[0].flush();

            reply = BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                               QueryReply.class);
            assertNotNull(reply);
            resp = (reply.getResultsAsList()).get(0);

        }
        assertNotNull(reply);
        assertNotNull(resp);
        Response[] res = new Response[] { resp };

        // this isn't really needed but just for completeness send it back to 
        // the test Leaf
        m = queryReplyFactory.createQueryReply(guid, (byte) 1, SERVER_PORT,
                myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                true, true, false, false, null);
        testUP[0].send(m);
        testUP[0].flush();

        // send back a lot of results via TCP so you konw the UDP one will be
        // bypassed
        for (int i = 0; i < testUP.length; i++) {
            res = new Response[75];
            for (int j = 0; j < res.length; j++)
                res[j] = responseFactory.createResponse(10+j+i, 10+j+i, "berkeley "+ j + i, UrnHelper.SHA1);
            m = queryReplyFactory.createQueryReply(guid, (byte) 1,
                    testUP[0].getPort(), myIP(), 0, res, GUID.makeGuid(), new byte[0],
                    false, false, true, true, false, false, null);
            testUP[i].send(m);
            testUP[i].flush();
        }

        // allow for processing
        Thread.sleep(3000);

        {
            // now we should make sure MessageRouter has not bypassed anything
            // yet
            assertByPassedResultsCacheHasSize(qr.getGUID(), 0);
        }
        
        // send back a UDP response and make sure it was saved in bypassed...
        {
            ReplyNumberVendorMessage vm = 
                replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(guid), 1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            DatagramPacket pack = new DatagramPacket(baos.toByteArray(), 
                                                     baos.toByteArray().length,
                                                     testUP[0].getInetAddress(),
                                                     SERVER_PORT);
            UDP_ACCESS[0].send(pack);
        }

        // allow for processing
        Thread.sleep(500);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            assertByPassedResultsCacheHasSize(guid, 1);
        }
        
        // now do the download, wait for it to finish, and then bypassed results
        // should not be empty since the query is still alive
        RemoteFileDesc rfd = resp.toRemoteFileDesc(reply, null, remoteFileDescFactory, pushEndpointFactory);
        
        assertFalse("file should not be saved yet", 
            new File( _savedDir, "berkeley.txt").exists());
        
        Downloader d = downloadServices.download(new RemoteFileDesc[] { rfd }, false, new GUID(guid));
        // sleep to make sure the download starts
        int sleeps = 0;
        while(sleeps++ < 30 && !d.isCompleted())
            Thread.sleep(1000);
        
        assertTrue("download took too long",d.isCompleted());
        
        assertTrue("file should saved", 
            new File( _savedDir, "berkeley.txt").exists());

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            assertByPassedResultsCacheHasSize(guid, 1);
        }
        
        searchServices.stopQuery(new GUID(guid));

        {
            // now we should make sure MessageRouter clears the map
            assertByPassedResultsCacheHasSize(qr.getGUID(),0);
        }

    }


    public void testDownloadProgressQueryDoneNoPurge() 
        throws Exception {

        BlockingConnectionUtils.keepAllAlive(testUP, pingReplyFactory);
        // clear up any messages before we begin the test.
        drainAll();

        // luckily there is hacky little way to go through the download paces -
        // download from yourself :) .

        Message m = null;

        byte[] guid = searchServices.newQueryGUID();
        searchServices.query(guid, "metadata");
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // just return ONE real result and the rest junk
        Response resp = null;
        QueryReply reply = null;
        {
            // get a correct response object
            QueryRequest qrTemp = queryRequestFactory.createQuery("metadata");
            testUP[0].send(qrTemp);
            testUP[0].flush();

            reply = BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                               QueryReply.class);
            assertNotNull(reply);
            resp = (reply.getResultsAsList()).get(0);

        }
        assertNotNull(reply);
        assertNotNull(resp);
        Response[] res = new Response[] { resp };

        // this isn't really needed but just for completeness send it back to 
        // the test Leaf
        m = queryReplyFactory.createQueryReply(guid, (byte) 1, SERVER_PORT,
                myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                true, true, false, false, null);
        testUP[0].send(m);
        testUP[0].flush();

        // send back a lot of results via TCP so you konw the UDP one will be
        // bypassed
        for (int i = 0; i < testUP.length; i++) {
            res = new Response[75];
            for (int j = 0; j < res.length; j++)
                res[j] = responseFactory.createResponse(10+j+i, 10+j+i, "metadata "+ j + i, UrnHelper.SHA1);
            m = queryReplyFactory.createQueryReply(guid, (byte) 1,
                    testUP[0].getPort(), myIP(), 0, res, GUID.makeGuid(), new byte[0],
                    false, false, true, true, false, false, null);
            testUP[i].send(m);
            testUP[i].flush();
        }

        // allow for processing
        Thread.sleep(3000);

        {
            // now we should make sure MessageRouter has not bypassed anything
            // yet
            assertByPassedResultsCacheHasSize(qr.getGUID(), 0);
        }
        
        // send back a UDP response and make sure it was saved in bypassed...
        {
            ReplyNumberVendorMessage vm = 
                replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(guid), 1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            DatagramPacket pack = new DatagramPacket(baos.toByteArray(), 
                                                     baos.toByteArray().length,
                                                     testUP[0].getInetAddress(),
                                                     SERVER_PORT);
            UDP_ACCESS[0].send(pack);
        }

        // allow for processing
        Thread.sleep(500);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            assertByPassedResultsCacheHasSize(guid, 1);
        }
        
        // now do the download, wait for it to finish, and then bypassed results
        // should be empty again
        RemoteFileDesc rfd = resp.toRemoteFileDesc(reply, null, remoteFileDescFactory, pushEndpointFactory);
        
        assertFalse("file should not be saved yet", 
            new File( _savedDir, "metadata.mp3").exists());
        
        downloadServices.download(new RemoteFileDesc[] { rfd }, false, new GUID(guid));
        UploadSettings.UPLOAD_SPEED.setValue(5);

        searchServices.stopQuery(new GUID(guid));
        callback.clearGUID();

        // download still in progress, don't purge
        assertByPassedResultsCacheHasSize(guid, 1);

        UploadSettings.UPLOAD_SPEED.setValue(100);

        // sleep to make sure the download starts 
        Thread.sleep(20000);
        
        assertTrue("file should saved", 
            new File( _savedDir, "metadata.mp3").exists());

        // now we should make sure MessageRouter clears the cache
        assertByPassedResultsCacheHasSize(qr.getGUID(), 0);
    }


    public void testBusyDownloadLocatesSources() throws Exception {

        BlockingConnectionUtils.keepAllAlive(testUP, pingReplyFactory);
        // clear up any messages before we begin the test.
        drainAll();

        DatagramPacket pack = null;

        Message m = null;

        byte[] guid = searchServices.newQueryGUID();
        searchServices.query(guid, "whatever");
        // i need to pretend that the UI is showing the user the query still
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // ok, the leaf is sending OOB queries - good stuff, now we should send
        // a lot of results back and make sure it buffers the bypassed OOB ones
        for (int i = 0; i < testUP.length; i++) {
            Response[] res = new Response[200];
            for (int j = 0; j < res.length; j++)
                res[j] = responseFactory.createResponse(10+j+i, 10+j+i, "whatever "+ j + i, UrnHelper.SHA1);
            m = queryReplyFactory.createQueryReply(qr.getGUID(), (byte) 1, 6355,
                    myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                    true, true, false, false, null);
            testUP[i].send(m);
            testUP[i].flush();
        }

        // create a test uploader and send back that response
        TestUploader uploader = new TestUploader(alternateLocationFactory, networkManagerStub);
        uploader.start("whatever", UPLOADER_PORT, false);
        uploader.setBusy(true);
        RemoteFileDesc rfd = makeRFD("GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ");

        // wait for processing
        Thread.sleep(1500);

        for (int i = 0; i < UDP_ACCESS.length; i++) {
            ReplyNumberVendorMessage vm = 
                replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(qr.getGUID()), i+1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUP[0].getInetAddress(), SERVER_PORT);
            UDP_ACCESS[i].send(pack);
        }

        // wait for processing
        Thread.sleep(500);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            assertByPassedResultsCacheHasSize(qr.getGUID(), UDP_ACCESS.length);
        }
        
        Downloader downloader = 
            downloadServices.download(new RemoteFileDesc[] { rfd }, false, 
                new GUID(guid));
        
        final int MAX_TRIES = 60;
        for (int i = 0; i <= MAX_TRIES; i++) {
            Thread.sleep(1000);
            if (downloader.getState() == DownloadStatus.ITERATIVE_GUESSING)
                break;
            if (i == MAX_TRIES) fail("didn't GUESS!!");
        }

        // we should start getting guess queries on all UDP ports, actually
        // querykey requests
        for (int i = 0; i < UDP_ACCESS.length; i++) {
            boolean gotPing = false;
            while (!gotPing) {
                try {
                    byte[] datagramBytes = new byte[1000];
                    pack = new DatagramPacket(datagramBytes, 1000);
                    UDP_ACCESS[i].setSoTimeout(10000); // may need to wait
                    UDP_ACCESS[i].receive(pack);
                    InputStream in = new ByteArrayInputStream(pack.getData());
                    m = messageFactory.read(in, Network.TCP);
                    m.hop();
                    if (m instanceof PingRequest)
                        gotPing = ((PingRequest) m).isQueryKeyRequest();
                }
                catch (InterruptedIOException iioe) {
                    fail("was successful for " + i, iioe);
                }
            }
        }

        //Thread.sleep((UDP_ACCESS.length * 1000) - 
                     //(System.currentTimeMillis() - currTime));
        int guessWaitTime = 5000;
        Thread.sleep(guessWaitTime+2000);
        assertEquals(DownloadStatus.BUSY, downloader.getState());

        callback.clearGUID();
        downloader.stop(false);

        Thread.sleep(1000);

        {
            // now we should make sure MessageRouter clears the map
            assertByPassedResultsCacheHasSize(qr.getGUID(), 0);
        }

        uploader.stopThread();
    }


    public void testDownloadFinishes() throws Exception {

        BlockingConnectionUtils.keepAllAlive(testUP, pingReplyFactory);
        // clear up any messages before we begin the test.
        drainAll();

        DatagramPacket pack = null;

        Message m = null;

        byte[] guid = searchServices.newQueryGUID();
        searchServices.query(guid, "whatever");
        // i need to pretend that the UI is showing the user the query still
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // ok, the leaf is sending OOB queries - good stuff, now we should send
        // a lot of results back and make sure it buffers the bypassed OOB ones
        for (int i = 0; i < testUP.length; i++) {
            Response[] res = new Response[200];
            for (int j = 0; j < res.length; j++)
                res[j] = responseFactory.createResponse(10+j+i, 10+j+i, "whatever "+ j + i, UrnHelper.SHA1);
            m = queryReplyFactory.createQueryReply(qr.getGUID(), (byte) 1, 6355,
                    myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                    true, true, false, false, null);
            testUP[i].send(m);
            testUP[i].flush();
        }

        // create a test uploader and send back that response
        TestUploader uploader = new TestUploader(alternateLocationFactory, networkManagerStub);
        uploader.start("whatever", UPLOADER_PORT, false);
        uploader.setBusy(true);
        URN urn = TestFile.hash();
        RemoteFileDesc rfd = makeRFD(urn);

        // wait for processing
        Thread.sleep(1500);

        // just do it for 1 UDP guy
        {
            ReplyNumberVendorMessage vm = 
                replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(qr.getGUID()), 1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUP[0].getInetAddress(), SERVER_PORT);
            UDP_ACCESS[0].send(pack);
        }

        // wait for processing
        Thread.sleep(500);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            assertByPassedResultsCacheHasSize(qr.getGUID(), 1);
        }
        
        long currTime = System.currentTimeMillis();
        Downloader downloader = 
            downloadServices.download(new RemoteFileDesc[] { rfd }, false, 
                new GUID(guid));
        
        final int MAX_TRIES = 60;
        for (int i = 0; i <= MAX_TRIES; i++) {
            Thread.sleep(500);
            if (downloader.getState() == DownloadStatus.ITERATIVE_GUESSING)
                break;
            if (i == MAX_TRIES) fail("didn't GUESS!!");
        }

        // we should get a query key request
        {
            boolean gotPing = false;
            while (!gotPing) {
                byte[] datagramBytes = new byte[1000];
                pack = new DatagramPacket(datagramBytes, 1000);
                UDP_ACCESS[0].setSoTimeout(10000); // may need to wait
                UDP_ACCESS[0].receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                m = messageFactory.read(in, Network.TCP);
                m.hop();
                if (m instanceof PingRequest)
                    gotPing = ((PingRequest) m).isQueryKeyRequest();
            }
        }

        // send back a query key
        AddressSecurityToken qk = new AddressSecurityToken(InetAddress.getLocalHost(), SERVER_PORT, macManager);
        {
            byte[] ip = new byte[] {(byte)127, (byte) 0, (byte) 0, (byte) 1};
            PingReply pr = 
                pingReplyFactory.createQueryKeyReply(GUID.makeGuid(), (byte) 1,
                                              UDP_ACCESS[0].getLocalPort(),
                                              ip, 10, 10, false, qk);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pr.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUP[0].getInetAddress(),
                                      SERVER_PORT);
            UDP_ACCESS[0].send(pack);
        }

        Thread.sleep(500);

        // ensure that it gets into the OnDemandUnicaster
        {
            // now we should make sure MessageRouter retains the key
            Map _queryKeys = (Map) PrivilegedAccessor.getValue(onDemandUnicaster, "_queryKeys");
            assertNotNull(_queryKeys);
            assertEquals(1, _queryKeys.size());
        }

        byte[] urnQueryGUID = null;
        { // confirm a URN query
            boolean gotQuery = false;
            while (!gotQuery) {
                byte[] datagramBytes = new byte[1000];
                pack = new DatagramPacket(datagramBytes, 1000);
                UDP_ACCESS[0].setSoTimeout(10000); // may need to wait
                UDP_ACCESS[0].receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                m = messageFactory.read(in, Network.TCP);
                if (m instanceof QueryRequest) {
                    QueryRequest qReq = (QueryRequest)m;
                    Set queryURNs = qReq.getQueryUrns();
                    gotQuery = queryURNs.contains(urn);
                    if (gotQuery) {
                        gotQuery = qReq.getQueryKey().isFor(InetAddress.getLocalHost(), SERVER_PORT);
                        if (gotQuery)
                            urnQueryGUID = qReq.getGUID();
                    }
                }
            }
        }
        
        assertNotNull(urnQueryGUID);

        long timeoutVal = 8000 - (System.currentTimeMillis() - currTime);
        Thread.sleep(timeoutVal > 0 ? timeoutVal : 0);
        assertEquals(DownloadStatus.BUSY, downloader.getState());
        // purge front end of query
        callback.clearGUID();

        // create a new Uploader to service the download
        TestUploader uploader2 = new TestUploader(alternateLocationFactory, networkManagerStub);
        uploader2.start("whatever", UPLOADER_PORT+1, false);
        uploader2.setRate(100);

        { // send back a query request, the TestUploader should service upload
            rfd = makeRFD(urn, UPLOADER_PORT + 1);
            Response[] res = new Response[] { responseFactory.createResponse(10, 10, "whatever", urn) };
            m = queryReplyFactory.createQueryReply(urnQueryGUID, (byte) 1,
                    UPLOADER_PORT+1, myIP(), 0, res, GUID.makeGuid(),
                    new byte[0], false, false, true, true, false, false, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUP[0].getInetAddress(),
                                      SERVER_PORT);
            UDP_ACCESS[0].send(pack);
        }

        // after a while, the download should finish, the bypassed results
        // should be discarded
        Thread.sleep(10000);
        assertEquals(DownloadStatus.COMPLETE, downloader.getState());

        {
            // now we should make sure MessageRouter clears the map
            assertByPassedResultsCacheHasSize(qr.getGUID(), 0);
        }
        uploader.stopThread();
    }


    public void testUsesCachedQueryKeys() throws Exception {

        BlockingConnectionUtils.keepAllAlive(testUP, pingReplyFactory);
        // clear up any messages before we begin the test.
        drainAll();

        DatagramPacket pack = null;

        Message m = null;

        byte[] guid = searchServices.newQueryGUID();
        searchServices.query(guid, "whatever");
        // i need to pretend that the UI is showing the user the query still
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // ok, the leaf is sending OOB queries - good stuff, now we should send
        // a lot of results back and make sure it buffers the bypassed OOB ones
        for (int i = 0; i < testUP.length; i++) {
            Response[] res = new Response[200];
            for (int j = 0; j < res.length; j++)
                res[j] = responseFactory.createResponse(10+j+i, 10+j+i, "whatever "+ j + i, UrnHelper.SHA1);
            m = queryReplyFactory.createQueryReply(qr.getGUID(), (byte) 1, 6355,
                    myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                    true, true, false, false, null);
            testUP[i].send(m);
            testUP[i].flush();
        }

        // create a test uploader and send back that response
        TestUploader uploader = new TestUploader(alternateLocationFactory, networkManagerStub);
        uploader.start("whatever", UPLOADER_PORT, false);
        uploader.setBusy(true);
        URN urn = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ");
        RemoteFileDesc rfd = makeRFD(urn);

        // wait for processing
        Thread.sleep(1500);

        // send back ReplyNumberVMs that should be bypassed
        for (int i = 0; i < UDP_ACCESS.length; i++) {
            ReplyNumberVendorMessage vm = 
                replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(qr.getGUID()), i+1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUP[0].getInetAddress(), SERVER_PORT);
            UDP_ACCESS[i].send(pack);
        }

        // wait for processing
        Thread.sleep(500);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            assertByPassedResultsCacheHasSize(qr.getGUID(), UDP_ACCESS.length);
        }
        
        // Prepopulate Query Keys
        AddressSecurityToken qk = new AddressSecurityToken(InetAddress.getLocalHost(),
                                           SERVER_PORT, macManager);
        for (int i = 0; i < UDP_ACCESS.length; i++) {
            byte[] ip = new byte[] {(byte)127, (byte) 0, (byte) 0, (byte) 1};
            PingReply pr = 
                pingReplyFactory.createQueryKeyReply(GUID.makeGuid(), (byte) 1,
                                              UDP_ACCESS[i].getLocalPort(),
                                              ip, 10, 10, false, qk);
            pr.hop();
            onDemandUnicaster.handleQueryKeyPong(pr);

        }

        // confirm download will try to GUESS
        long currTime = System.currentTimeMillis();
        Downloader downloader = 
            downloadServices.download(new RemoteFileDesc[] { rfd }, false, 
                new GUID(guid));
        
        final int MAX_TRIES = 60;
        for (int i = 0; i <= MAX_TRIES; i++) {
            Thread.sleep(500);
            if (downloader.getState() == DownloadStatus.ITERATIVE_GUESSING)
                break;
            if (i == MAX_TRIES) fail("didn't GUESS!!");
        }

        // we should start getting guess queries on all UDP ports
        for (int i = 0; i < UDP_ACCESS.length; i++) {
            boolean gotQuery = false;
            while (!gotQuery) {
                try {
                    byte[] datagramBytes = new byte[1000];
                    pack = new DatagramPacket(datagramBytes, 1000);
                    UDP_ACCESS[i].setSoTimeout(10000); // may need to wait
                    UDP_ACCESS[i].receive(pack);
                    InputStream in = new ByteArrayInputStream(pack.getData());
                    m = messageFactory.read(in, Network.TCP);
                    if (m instanceof QueryRequest) {
                        QueryRequest qReq = (QueryRequest)m;
                        Set queryURNs = qReq.getQueryUrns();
                        gotQuery = queryURNs.contains(urn);
                        if (gotQuery)
                            gotQuery = qReq.getQueryKey().isFor(InetAddress.getLocalHost(), SERVER_PORT);
                    }
                }
                catch (InterruptedIOException iioe) {
                    assertTrue("was successful for " + i,
                               false);
                }
            }
        }

        Thread.sleep((UDP_ACCESS.length * 1500) - 
                     (System.currentTimeMillis() - currTime));

        assertEquals(DownloadStatus.BUSY, downloader.getState());

        callback.clearGUID();
        downloader.stop(false);

        Thread.sleep(1000);

        {
            // now we should make sure MessageRouter clears the map
            assertByPassedResultsCacheHasSize(qr.getGUID(), 0);
        }

        uploader.stopThread();
    }


    public void testMultipleDownloadsNoPurge() throws Exception {

        BlockingConnectionUtils.keepAllAlive(testUP, pingReplyFactory);
        // clear up any messages before we begin the test.
        drainAll();

        DatagramPacket pack = null;

        Message m = null;

        byte[] guid = searchServices.newQueryGUID();
        searchServices.query(guid, "whatever");
        // i need to pretend that the UI is showing the user the query still
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // ok, the leaf is sending OOB queries - good stuff, now we should send
        // a lot of results back and make sure it buffers the bypassed OOB ones
        for (int i = 0; i < testUP.length; i++) {
            Response[] res = new Response[200];
            for (int j = 0; j < res.length; j++)
                res[j] = responseFactory.createResponse(10+j+i, 10+j+i, "whatever "+ j + i, UrnHelper.SHA1);
            m = queryReplyFactory.createQueryReply(qr.getGUID(), (byte) 1, 6355,
                    myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                    true, true, false, false, null);
            testUP[i].send(m);
            testUP[i].flush();
        }

        // create a test uploader and send back that response
        TestUploader uploader = new TestUploader(alternateLocationFactory, networkManagerStub);
        uploader.start("whatever", UPLOADER_PORT, false);
        uploader.setBusy(true);
        RemoteFileDesc rfd = makeRFD("GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ");
        
        TestUploader uploader2 = new TestUploader(alternateLocationFactory, networkManagerStub);
        uploader2.start("whatever", UPLOADER_PORT*2, false);
        uploader2.setBusy(true);
        RemoteFileDesc rfd2 = makeRFD("GLIQY64M7FSXBSQEZY37FIM5QQSASUSH");

        // wait for processing
        Thread.sleep(1500);

        { // bypass 1 result only
            ReplyNumberVendorMessage vm = 
                replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(qr.getGUID()), 1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUP[0].getInetAddress(), SERVER_PORT);
            UDP_ACCESS[0].send(pack);
        }

        // wait for processing
        Thread.sleep(500);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            assertByPassedResultsCacheHasSize(qr.getGUID(), 1);
        }
        
        Downloader downloader = 
            downloadServices.download(new RemoteFileDesc[] { rfd }, false, 
                new GUID(guid));
        
        //  Don't try using the same default file 
        Downloader downloader2 = 
            downloadServices.download(new RemoteFileDesc[] { rfd2 }, 
                new GUID(guid), false, null, "anotherFile" );
        

        // let downloaders do stuff  
        final int MAX_TRIES = 60;
        boolean oneGood = false, twoGood = false;
        for (int i = 0; i <= MAX_TRIES; i++) {
            Thread.sleep(500);
            if (downloader.getState() == DownloadStatus.BUSY)
                oneGood = true;
            if (downloader2.getState() == DownloadStatus.BUSY)
                twoGood = true;
            if (oneGood && twoGood) break;
            if (i == MAX_TRIES) fail("didn't GUESS!!");
        }

        callback.clearGUID();  // isQueryAlive == false 
        downloader.stop(false);

        Thread.sleep(500);

        {
            // we should still have bypassed results since downloader2 alive
            assertByPassedResultsCacheHasSize(qr.getGUID(), 1);
        }

        downloader2.stop(false);
        Thread.sleep(1000);

        {
            // now we should make sure MessageRouter clears the map
            assertByPassedResultsCacheHasSize(qr.getGUID(), 0);
        }
        uploader.stopThread();
        uploader2.stopThread();
    }

    // RUN THIS TEST LAST!!
    // TODO move this test case to OnDemandUnicasterTest, sounds like a pure unit test
    // proabably doesn't make sense anymore since it doesn't have any data from the 
    // previous tests to clear
    public void testUnicasterClearingCode() throws Exception {

        BlockingConnectionUtils.keepAllAlive(testUP, pingReplyFactory);
        // clear up any messages before we begin the test.
        drainAll();

        { // clear all the unicaster data structures
            Long[] longs = new Long[] { new Long(0), new Long(1) };
            Class[] classTypes = new Class[] { Long.TYPE, Long.TYPE };
            // now confirm that clearing code works
            Object ret = PrivilegedAccessor.invokeMethod(onDemandUnicaster,
                                                         "clearDataStructures", 
                                                         longs, classTypes);
            assertTrue(ret instanceof Boolean);
            assertTrue(((Boolean)ret).booleanValue());
        }


        DatagramPacket pack = null;

        Message m = null;

        byte[] guid = searchServices.newQueryGUID();
        searchServices.query(guid, "whatever");
        // i need to pretend that the UI is showing the user the query still
        callback.setGUID(new GUID(guid));
        
        QueryRequest qr = 
            BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                         QueryRequest.class);
        assertNotNull(qr);
        assertTrue(qr.desiresOutOfBandReplies());

        // ok, the leaf is sending OOB queries - good stuff, now we should send
        // a lot of results back and make sure it buffers the bypassed OOB ones
        for (int i = 0; i < testUP.length; i++) {
            Response[] res = new Response[200];
            for (int j = 0; j < res.length; j++)
                res[j] = responseFactory.createResponse(10+j+i, 10+j+i, "whatever "+ j + i, UrnHelper.SHA1);
            m = queryReplyFactory.createQueryReply(qr.getGUID(), (byte) 1, 6355,
                    myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                    true, true, false, false, null);
            testUP[i].send(m);
            testUP[i].flush();
        }

        // create a test uploader and send back that response
        TestUploader uploader = new TestUploader(alternateLocationFactory, networkManagerStub);
        uploader.start("whatever", UPLOADER_PORT, false);
        uploader.setBusy(true);
        RemoteFileDesc rfd = makeRFD("GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ");

        // wait for processing
        Thread.sleep(1500);

        for (int i = 0; i < UDP_ACCESS.length; i++) {
            ReplyNumberVendorMessage vm = 
                replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(qr.getGUID()), i+1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            vm.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUP[0].getInetAddress(), SERVER_PORT);
            UDP_ACCESS[i].send(pack);
        }

        // wait for processing
        Thread.sleep(500);

        {
            // all the UDP ReplyNumberVMs should have been bypassed
            assertByPassedResultsCacheHasSize(qr.getGUID(), UDP_ACCESS.length);
        }
        
        Downloader downloader = 
            downloadServices.download(new RemoteFileDesc[] { rfd }, false, 
                new GUID(guid));
        
        final int MAX_TRIES = 60;
        for (int i = 0; i <= MAX_TRIES; i++) {
            Thread.sleep(500);
            if (downloader.getState() == DownloadStatus.ITERATIVE_GUESSING)
                break;
            if (i == MAX_TRIES) fail("didn't GUESS!!");
        }

        // we should start getting guess queries on all UDP ports, actually
        // querykey requests
        for (int i = 0; i < UDP_ACCESS.length; i++) {
            boolean gotPing = false;
            while (!gotPing) {
                try {
                    byte[] datagramBytes = new byte[1000];
                    pack = new DatagramPacket(datagramBytes, 1000);
                    UDP_ACCESS[i].setSoTimeout(10000); // may need to wait
                    UDP_ACCESS[i].receive(pack);
                    InputStream in = new ByteArrayInputStream(pack.getData());
                    m = messageFactory.read(in, Network.TCP);
                    m.hop();
                    if (m instanceof PingRequest)
                        gotPing = ((PingRequest) m).isQueryKeyRequest();
                }
                catch (InterruptedIOException iioe) {
                    assertTrue("was successful for " + i,
                               false);
                }
            }
        }

        // Prepopulate Query Keys
        AddressSecurityToken qk = new AddressSecurityToken(InetAddress.getLocalHost(),
                                           SERVER_PORT, macManager);
        for (int i = 0; i < (UDP_ACCESS.length/2); i++) {
            byte[] ip = new byte[] {(byte)127, (byte) 0, (byte) 0, (byte) 1};
            PingReply pr = 
                pingReplyFactory.createQueryKeyReply(GUID.makeGuid(), (byte) 1,
                                              UDP_ACCESS[i].getLocalPort(),
                                              ip, 10, 10, false, qk);
            pr.hop();
            onDemandUnicaster.handleQueryKeyPong(pr);

        }

        // ensure that it gets into the OnDemandUnicaster
        {
            // now we should make sure MessageRouter retains the key
            Map _queryKeys = 
            (Map) PrivilegedAccessor.getValue(onDemandUnicaster,
                                              "_queryKeys");
            assertNotNull(_queryKeys);
            assertEquals((UDP_ACCESS.length/2), _queryKeys.size());

            // now make sure some URNs are still buffered
            Map _bufferedURNs = 
            (Map) PrivilegedAccessor.getValue(onDemandUnicaster,
                                              "_bufferedURNs");
            assertNotNull(_bufferedURNs);
            assertEquals((UDP_ACCESS.length/2), _bufferedURNs.size());

        }

        // now until those guys get expired
        Thread.sleep(60 * 1000);

        {
            Long[] longs = new Long[] { new Long(0), new Long(1) };
            Class[] classTypes = new Class[] { Long.TYPE, Long.TYPE };
            // now confirm that clearing code works
            Object ret = PrivilegedAccessor.invokeMethod(onDemandUnicaster,
                                                         "clearDataStructures", 
                                                         longs, classTypes);
            assertTrue(ret instanceof Boolean);
            assertTrue(((Boolean)ret).booleanValue());
        }

        // ensure that clearing worked
        {
            // now we should make sure MessageRouter retains the key
            Map _queryKeys = 
            (Map) PrivilegedAccessor.getValue(onDemandUnicaster,
                                              "_queryKeys");
            assertNotNull(_queryKeys);
            assertEquals(0, _queryKeys.size());

            // now make sure some URNs are still buffered
            Map _bufferedURNs = 
            (Map) PrivilegedAccessor.getValue(onDemandUnicaster,
                                              "_bufferedURNs");
            assertNotNull(_bufferedURNs);
            assertEquals(0, _bufferedURNs.size());

        }

        
        assertEquals(DownloadStatus.BUSY, downloader.getState());

        callback.clearGUID();
        downloader.stop(false);

        Thread.sleep(1000);

        {
            // now we should make sure MessageRouter clears the map
            assertByPassedResultsCacheHasSize(qr.getGUID(), 0);
        }
    }


    //////////////////////////////////////////////////////////////////
    
    
    
    private RemoteFileDesc makeRFD(URN urn, int port) throws Exception {
        Set<URN> urns = new HashSet<URN>();
        urns.add(urn);
        return injector.getInstance(RemoteFileDescFactory.class).createRemoteFileDesc(new ConnectableImpl("127.0.0.1", port, false), 1, "whatever", 10, GUID.makeGuid(), 1,
                false, 3, false, null, urns, false, "LIME", -1);
    }
    
    private RemoteFileDesc makeRFD(String sha1) throws Exception {
        URN urn = URN.createSHA1Urn("urn:sha1:" + sha1);
        return makeRFD(urn);
    }
    
    private RemoteFileDesc makeRFD(URN urn) throws Exception {
        return makeRFD(urn, UPLOADER_PORT);
    }

    @Override
    public int getNumberOfPeers() {
        return 3;
    }
    
    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, 0, 1 };
    }

    @Singleton
    public static class MyCallback extends ActivityCallbackStub {
        public GUID aliveGUID = null;

        public void setGUID(GUID guid) { aliveGUID = guid; }
        public void clearGUID() { aliveGUID = null; }

        @Override
        public boolean isQueryAlive(GUID guid) {
            if (aliveGUID != null)
                return (aliveGUID.equals(guid));
            return false;
        }
    }


}
