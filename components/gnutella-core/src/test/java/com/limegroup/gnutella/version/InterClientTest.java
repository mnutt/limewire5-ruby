package com.limegroup.gnutella.version;

import java.io.ByteArrayOutputStream;
import java.io.File;

import junit.framework.Test;

import org.limewire.core.settings.UpdateSettings;
import org.limewire.gnutella.tests.ActivityCallbackStub;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GGEP;
import org.limewire.listener.EventListener;
import org.limewire.util.Base32;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.BlockingConnectionUtils;
import com.limegroup.gnutella.PeerTestCase;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.AbstractVendorMessage;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMStubHelper;
import com.limegroup.gnutella.messages.vendor.UpdateRequest;
import com.limegroup.gnutella.messages.vendor.UpdateResponse;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Tests to make sure updates are sent, requested, etc...
 */
public class InterClientTest extends PeerTestCase {
    
    private BlockingConnection PEER;
    
    private MyActivityCallback myActivityCallback;
    
    public InterClientTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(InterClientTest.class);
    }    

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
    
    private UpdateRequest dummy = new UpdateRequest();
    
    @Override
    public void setUp() throws Exception {
        myActivityCallback = new MyActivityCallback();
        Module m = new AbstractModule() {
            @Override
            public void configure() {
                bind(ActivityCallback.class).toInstance(myActivityCallback);
            }
        };
        super.setUp(LimeTestUtils.createInjector(Stage.PRODUCTION, m));
        setEmpty();
        PEER = connect(true);
        BlockingConnectionUtils.drain(PEER);
        doInitialExchange();
    }
    
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if(PEER != null)
            PEER.close();
    }
    
    private UpdateHandler getUpdateHandler() {
        return injector.getInstance(UpdateHandler.class);
    }
    
    /**
     * Simple test to make sure that if someone sends a CapabilitiesVM
     * with a greater version than we have, that we request the new version.
     * And that we don't request if someone doesn't send it.
     */
    public void testRequestIsSent() throws Exception {
        assertEquals(0, getUpdateHandler().getLatestId());
        
        PEER.send(getCVM(1));
        PEER.flush();
        
        // We should get an UpdateRequest.
        Message m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateRequest.class);
        assertNotNull(m);
        assertInstanceof(UpdateRequest.class, m);
        
        setCurrentId(10);
        assertEquals(10, getUpdateHandler().getLatestId());
        PEER.send(getCVM(5));
        PEER.flush();
        
        // we shouldn't get a message, since they said they had 5 & we know of 10.
        m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateRequest.class);
        assertNull(m);
        
        // Now if they send with 11, we'll request.
        PEER.send(getCVM(11));
        PEER.flush();
        m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateRequest.class);
        assertNotNull(m);
        assertInstanceof(UpdateRequest.class, m);
    }
    
    /**
     * Tests that a response is sent in response to a rquest.
     */
    public void testResponseIsSent() throws Exception {
        assertEquals(0, getUpdateHandler().getLatestId());
        
        // We should get no response, since we have no data to give.
        PEER.send(new UpdateRequest());
        PEER.flush();
        Message m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNull(m);
        
        // Alright, set some current bytes so we can do some testing.
        byte[] data = setCurrent(-10);
        PEER.send(new UpdateRequest());
        PEER.flush();
        m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNotNull(m);
        assertInstanceof(UpdateResponse.class, m);
        byte[] payload = payload(m);
        GGEP ggep = new GGEP(payload, 0);
        assertEquals(data, ggep.get("U"));
        assertEquals(1, ggep.getHeaders().size());
    }
    
    /**
     * Tests that valid versions are used.
     */
    public void testValidVersion() throws Exception {
        setCurrentId(-11);
        assertEquals(-11, getUpdateHandler().getLatestId());
        
        // Get the -10 file.
        byte[] b = readFile(-10);
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-10, getUpdateHandler().getLatestId());
        
        // Make sure we got a new CapabilitiesVM.
        Message m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, CapabilitiesVM.class);
        assertNotNull(m);
        // TODO: is 65526 right?
        assertEquals(65526, ((CapabilitiesVM)m).supportsUpdate());
    }
    
    /**
     * Tests that older versions are ignored.
     */
    public void testOlderVersionsIgnored() throws Exception {
        setCurrentId(-9);
        assertEquals(-9, getUpdateHandler().getLatestId());
        
        // Get the -10 file.
        byte[] b = readFile(-10);
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-9, getUpdateHandler().getLatestId());        
    }
    
    /**
     * Test that invalid signatures are ignored.
     */
    public void testInvalidSignaturesIgnored() throws Exception {
        setCurrentId(-11);
        assertEquals(-11, getUpdateHandler().getLatestId());
        
        // Get the -10 file.
        byte[] b = readFile(-10);
        b[0] = '0'; // break the sig.
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-11, getUpdateHandler().getLatestId());
    }
    
    /**
     * Tests that invalid bytes break verification.
     */
    public void testInvalidBytesIgnored() throws Exception {
        setCurrentId(-11);
        assertEquals(-11, getUpdateHandler().getLatestId());
        
        // Get the -10 file.
        byte[] b = readFile(-10);
        b[b.length-1] = '0'; // break the data.
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-11, getUpdateHandler().getLatestId());
    }
    
    /**
     * Test invalid XML ignored.
     */
    public void testInvalidXMLIgnored() throws Exception {
        setCurrentId(-11);
        assertEquals(-11, getUpdateHandler().getLatestId());
        
        // Get the -9 file.
        byte[] b = readFile(-9);
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        Thread.sleep(1000); // let it process.
        
        assertEquals(-11, getUpdateHandler().getLatestId());
    }
    
    /**
     * Tests that no updates are sent to the UI because the 
     * version is too old
     */
    public void testUpdatesNotSentToGui() throws Exception {
        setCurrentId(-11);
        assertEquals(-11, getUpdateHandler().getLatestId());
        
        // add listener, should not receive any events
        HandleUpdate update = new HandleUpdate();
        getUpdateHandler().addListener(update);
        
        // Get the -8 file.
        byte[] b = readFile(-8);
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        Thread.sleep(1000); // let it process.

        getUpdateHandler().removeListener(update);// remove listener
        
        assertEquals(-8, getUpdateHandler().getLatestId());
        assertNull("Should not recieve update event", update.event);
    }
    
    /**
     * Tests that updates are sent out when versions come in.
     */
    public void testUpdatesSentToGUI() throws Exception {       
        setCurrentId(-11);
        assertEquals(-11, getUpdateHandler().getLatestId());
        setVersion("3.0.0");

        // Set the update style to zero to ensure the message is not ignored
        UpdateSettings.UPDATE_STYLE.setValue(0);
        
        //add listener, should receive an update event
        HandleUpdate update = new HandleUpdate();
        getUpdateHandler().addListener(update);
        
        // Get the -8 file.
        byte[] b = readFile(-8);
        PEER.send(UpdateResponse.createUpdateResponse(b,dummy));
        PEER.flush();
        
        Thread.sleep(1000); // let it process.

        getUpdateHandler().removeListener(update); //remove listener
        
        assertEquals(-8, getUpdateHandler().getLatestId());
        assertEquals(update.event.getType(), com.limegroup.gnutella.version.UpdateEvent.Type.UPDATE);
    }    
    
    private class HandleUpdate implements EventListener<UpdateEvent> {
        UpdateEvent event = null;

        @Override
        public void handleEvent(UpdateEvent event) {
            this.event = event;
        }
    }
    
    public void testCompressedResponse() throws Exception {
        assertEquals(0, getUpdateHandler().getLatestId());
        
        // We should get no response, since we have no data to give.
        UpdateRequestStub request = new UpdateRequestStub(2,true,true);
        PEER.send(new UpdateRequest());
        PEER.flush();
        Message m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNull(m);
        
        // Alright, set some current bytes so we can do some testing.
        byte[] data = setCurrent(-10);
        PEER.send(request);
        PEER.flush();
        m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNotNull(m);
        assertInstanceof(UpdateResponse.class, m);
        byte [] payload = payload(m);
        GGEP g = new GGEP(payload,0,null);
        assertEquals(g.getBytes("C"),data);
        assertFalse(g.hasKey("U"));
    }
    
    public void testUncompressedGGEPResponse() throws Exception {
        assertEquals(0, getUpdateHandler().getLatestId());
        
        // We should get no response, since we have no data to give.
        PEER.send(new UpdateRequest());
        PEER.flush();
        Message m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNull(m);
        
        UpdateRequestStub request = new UpdateRequestStub(2,true,false);
        byte[] data = setCurrent(-10);
        PEER.send(request);
        PEER.flush();
        m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNotNull(m);
        assertInstanceof(UpdateResponse.class, m);
        
        byte [] payload = payload(m);
        GGEP g = new GGEP(payload,0,null);
        assertEquals(g.getBytes("U"),data);
        assertFalse(g.hasKey("C"));
    }
    
    /**
     * tests that a request with higher version w/o GGEP gets responded to properly
     */
    public void testHigherVersionNoGGEP() throws Exception {
        assertEquals(0, getUpdateHandler().getLatestId());
        
        // We should get no response, since we have no data to give.
        PEER.send(new UpdateRequest());
        PEER.flush();
        Message m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNull(m);
        
        UpdateRequestStub request = new UpdateRequestStub(UpdateRequest.VERSION+1,false,false);
        byte[] data = setCurrent(-10);
        PEER.send(request);
        PEER.flush();
        m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNotNull(m);
        byte [] payload = payload(m);
        GGEP g = new GGEP(payload,0);
        assertEquals(g.getBytes("U"),data);
        assertFalse(g.hasKey("C"));
    }
    
    /** Tests that requests from older versions get responded to properly. */
    public void testOldRequestGetsMaxVersionNoGGEP() throws Exception {
        assertEquals(0, getUpdateHandler().getLatestId());
        
        // We should get no response, since we have no data to give.
        PEER.send(new UpdateRequestStub(1, false, false));
        PEER.flush();
        Message m = BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateResponse.class);
        assertNotNull(m);
        
        byte[] payload = payload(m);
        assertEquals("I5AVOQKFIZCE4Q2RKFATKVBWKBKVEWSOJRFU6WS2JVIUCR2QGRJESNBVIE3UESKDINIUCSSWIZKE2WCHGZKFMMS2GJAVSTKCGQ2TINSLIRFEQWCRG5KEITL4PQ6HK4DEMF2GKIDJMQ6SEMRRGQ3TIOBTGY2DOIRAORUW2ZLTORQW24B5EIYSEPQKEAQCAPDNONTSAZTSN5WT2IRXGYXDONZOG42SEIDGN5ZD2IRYGYXDQOBOHA2SEIDUN46SEOBWFY4DSLRYGURCA5LSNQ6SE2DUORYDULZPO53XOLTMNFWWK53JOJSS4Y3PNUXXK4DEMF2GKIRAON2HS3DFHURDAIRAN5ZT2ISXNFXGI33XOMRCA5LSNY6SE5LSNY5GE2LUOBZGS3TUHJIEYUCSKRIEET2BKJBE6U2BJNIECTKHKZJTEU2MGU3VGM2HIRGFCLRXIZIEGR2NG43VGSCPKFGVAUSQJU2UGNKMJ5NEKT2EG43EGRK2IQ2E2USBIVGESIRAOVRW63LNMFXGIPJHEISCKIRAF5JSOIDVNZQW2ZJ5EJGGS3LFK5UXEZKXNFXDILRRGYXDMLTFPBSSEIDTNF5GKPJCGQ2TANRSGU3CEPQKEAQCAIBAEA6GYYLOM4QGSZB5E5SW4JZ6BIQCAIBAEAQCAIB4EFNUGRCBKRAVWNBOGE3C4NRAKVJE4XK5HYFCAIBAEAQCAPBPNRQW4ZZ6BIQCAIB4F5WXGZZ6BIQCAIBAEAQDY3LTM4QGM4TPNU6SENBOHAXDCIRAMZXXEPJCGQXDCNROGYRCA5LSNQ6SE2DUORYDULZPO53XOLTMNFWWK53JOJSS4Y3PNUXXK4DEMF2GKIRAMZZGKZJ5EJ2HE5LFEIQG64Z5EJLWS3TEN53XGIRAON2HS3DFHURDIIRAOVZG4PJCOVZG4OTCNF2HA4TJNZ2DUUCMKBJFIUCCJ5AVEQSPKNAUWUCBJVDVMUZSKNGDKN2TGNDUITCRFY3UMUCDI5GTON2TJBHVCTKQKJIE2NKDGVGE6WSFJ5CDONSDIVNEINCNKJAUKTCJEIQHKY3PNVWWC3TEHUTSEJBFEIQC6UZHEB2W4YLNMU6SETDJNVSVO2LSMVLWS3RUFYYTMLRWFZSXQZJCEBZWS6TFHURDINJQGYZDKNRCHYFCAIBAEAQCAPDMMFXGOIDJMQ6SOZLOE47AUIBAEAQCAIB4EFNUGRCBKRAVWCRAEAQCAIBAHR2GCYTMMUQGC3DJM5XD2Y3FNZ2GK4RAOZQWY2LHNY6WGZLOORSXEPR4ORZD4PDUMQ7AUPDDMVXHIZLSHY6GEPSVOJTWK3TUEBGGS3LFK5UXEZJAKNSWG5LSNF2HSICVOBSGC5DFEBAXMYLJNRQWE3DFFY6GE4R6BJIGYZLBONSSAVLQMRQXIZJAJFWW2ZLENFQXIZLMPEXDYYTSHY6GE4R6HQXWEPQKJFTCA5DIMUQHK4DEMF2GKIDEN5SXGIDON52CA53POJVSYIDWNFZWS5B4MJZD4CTIOR2HAORPF53XO5ZONRUW2ZLXNFZGKLTDN5WS6ZDPO5XGY33BMQ6GE4R6EBTG64RAORUGKIDMMF2GK43UEB3GK4TTNFXW4IDPMYQEY2LNMVLWS4TFFY6C6YR6HQXWGZLOORSXEPR4F52GIPR4F52HEPR4F52GCYTMMU7AUIBAEAQCAIC5LU7AUIBAEAQCAIB4F5WGC3THHYFCAIBAEAQCAPBPNVZWOPQKEAQCAIBAEAFCAIBAEAQCAPDNONTSAZTSN5WT2IRUFY4C4MJCEBTG64R5EI2C4MJWFY3CEIDVOJWD2ITIOR2HAORPF53XO5ZONRUW2ZLXNFZGKLTDN5WS65LQMRQXIZJCEBZXI6LMMU6SENBCHYFCAIBAEAQCAPDMMFXGOIDJMQ6SOZLOE47AUIBAEAQCAIB4EFNUGRCBKRAVWCRAEAQCAIBAHR2GCYTMMUQGC3DJM5XD2Y3FNZ2GK4RAOZQWY2LHNY6WGZLOORSXEPR4ORZD4PDUMQ7AUPDDMVXHIZLSHY6GEPSVOJTWK3TUEBGGS3LFK5UXEZJAKNSWG5LSNF2HSICVOBSGC5DFEBAXMYLJNRQWE3DFFY6GE4R6BJIGYZLBONSSAVLQMRQXIZJAJFWW2ZLENFQXIZLMPEXDYYTSHY6GE4R6HQXWEPQKJFTCA5DIMUQHK4DEMF2GKIDEN5SXGIDON52CA53POJVSYIDWNFZWS5B4MJZD4CTIOR2HAORPF53XO5ZONRUW2ZLXNFZGKLTDN5WS6ZDPO5XGY33BMQ6GE4R6EBTG64RAORUGKIDMMF2GK43UEB3GK4TTNFXW4IDPMYQEY2LNMVLWS4TFFY6C6YR6HQXWGZLOORSXEPR4F52GIPR4F52HEPR4F52GCYTMMU7AUIBAEAQCAIC5LU7AUIBAEAQCAIB4F5WGC3THHYFCAIBAEAQCAPBPNVZWOPQKHQXXK4DEMF2GKPQK",
                Base32.encode(payload));
    }
    
    private  void setCurrentId(int i) throws Exception {
        PrivilegedAccessor.setValue(getUpdateHandler(), "_lastId", new Integer(i));
    }
    
    private void setCurrentBytes(byte[] b) throws Exception {
        PrivilegedAccessor.setValue(getUpdateHandler(), "_lastBytes", b);
    }
    
    private void setCurrentInfo(UpdateInformation info) throws Exception {
        PrivilegedAccessor.setValue(getUpdateHandler(), "_updateInfo", null);
    }
    
    private byte[] setCurrent(int i) throws Exception {
        setCurrentId(i);
        byte[] b = readFile(i);
        assertNotNull(b);
        setCurrentBytes(b);
        return b;
    }
    
    private void setEmpty() throws Exception {
        setCurrentId(0);
        setCurrentBytes(null);
        setCurrentInfo(null);
    }
    
    private static byte[] readFile(int i) throws Exception {
        File f = TestUtils.getResourceFile("com/limegroup/gnutella/version/test_" + i + ".xml");
        assertTrue(f.exists());
        assertTrue(f.isFile());
        return FileUtils.readFileFully(f);
    }
    
    private static void setVersion(String v) throws Exception {
        PrivilegedAccessor.setValue(LimeWireUtils.class, "testVersion", v);
    }
    
    private static byte[] payload(Message m) throws Exception {
        assertInstanceof("not a vendor message!", VendorMessage.class, m);
        return (byte[])PrivilegedAccessor.invokeMethod(m, "getPayload");
    }
    
    private static Message getCVM(int i) throws Exception {
        return CapabilitiesVMStubHelper.makeCapabilitiesWithUpdate(i);
    }
    
    private void doInitialExchange() throws Exception {
        PEER.send(getCVM(0));
        PEER.flush();
        assertNotNull(BlockingConnectionUtils.getFirstInstanceOfMessageType(PEER, UpdateRequest.class));
    }

    /* Required for PeerTestCase. */
    @Override
    protected ActivityCallback getActivityCallback() {
        return new MyActivityCallback();
    }

    /* Required for PeerTestCase. */
    private static class MyActivityCallback extends ActivityCallbackStub {
        UpdateInformation lastUpdate;
        
        @Override
        public void updateAvailable(UpdateInformation info) {
            lastUpdate = info;
        }
    }
    
    private static byte[] derivePayload(boolean hasGGEP, boolean requestsCompressed) throws Exception {
        if (!hasGGEP)
            return DataUtils.EMPTY_BYTE_ARRAY;
        
        GGEP g = new GGEP();
        if (requestsCompressed)
            g.put("C");
        else
            g.put("X"); //put something else
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        g.write(baos);
        return baos.toByteArray();
    }
    
    private static class UpdateRequestStub extends AbstractVendorMessage {

        public int version;
        public boolean hasGGEP,requestsCompressed;
        
        public UpdateRequestStub(int version, boolean hasGGEP, boolean requestsCompressed) 
        throws Exception {
            super(F_LIME_VENDOR_ID, F_UPDATE_REQ, version, derivePayload(hasGGEP, requestsCompressed));
            this.version = version;
            this.hasGGEP = hasGGEP;
            this.requestsCompressed = requestsCompressed;
        }
        
        @Override
        public int getVersion() {
            return version;
        }

        public boolean hasGGEP() {
            return hasGGEP;
        }

        public boolean requestsCompressed() {
            return requestsCompressed;
        }
        
    }
}

