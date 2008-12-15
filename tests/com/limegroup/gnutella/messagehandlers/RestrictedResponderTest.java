package com.limegroup.gnutella.messagehandlers;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.Executor;

import junit.framework.Test;

import org.limewire.io.GGEP;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.security.Verifier;
import org.limewire.setting.LongSetting;
import org.limewire.setting.SettingsFactory;
import org.limewire.setting.StringArraySetting;
import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPReplyHandlerCache;
import com.limegroup.gnutella.UDPReplyHandlerFactory;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.vendor.RoutableGGEPMessage;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.stubs.ReplyHandlerStub;

@SuppressWarnings("all")
public class RestrictedResponderTest extends BaseTestCase {

    private PingRequestFactory pingRequestFactory;
    private NetworkManager networkManager;
    private SimppManager simppManager;
    private UDPReplyHandlerFactory udpReplyHandlerFactory;
    private UDPReplyHandlerCache udpReplyHandlerCache;

    public RestrictedResponderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RestrictedResponderTest.class);
    }    
    
    static InetSocketAddress addr;
    static ReplyHandler h;
    static StringArraySetting ipSetting;
    static LongSetting versionSetting;
    
    public void setUp() throws Exception {
        SettingsFactory f = new SettingsFactory(new File("set"));
        ipSetting = f.createStringArraySetting("ips", new String[]{"1.2.3.4"});
        versionSetting = f.createLongSetting("version", 0);
        addr = new InetSocketAddress(InetAddress.getLocalHost(),1000);
        h = new ReplyHandlerStub() {
            public String getAddress() {
                return "1.2.3.4";
            }
        };
		Injector injector = LimeTestUtils.createInjector();

		pingRequestFactory = injector.getInstance(PingRequestFactory.class);
		networkManager = injector.getInstance(NetworkManager.class);
		simppManager = injector.getInstance(SimppManager.class);
		udpReplyHandlerFactory = injector.getInstance(UDPReplyHandlerFactory.class);
		udpReplyHandlerCache = injector.getInstance(UDPReplyHandlerCache.class);
    }
    
    public void testRestrictions() throws Exception {
        // ban everyone
        ipSetting.setValue(new String[0]);
        TestResponder responder = new TestResponder(null);
        Message m = pingRequestFactory.createMulticastPing();
        responder.handleMessage(m, addr, h);
        assertNull(responder.msg);
        assertNull(responder.addr);
        assertNull(responder.handler);
        
        // allow this specific hosts
        ipSetting.setValue(new String[]{"1.2.3.4"});
        triggerSimppUpdate();
        responder.handleMessage(m, addr, h);
        assertSame(m, responder.msg);
        assertSame(addr, responder.addr);
        assertSame(h, responder.handler);
        
        // but someone else won't get in
        responder.reset();
        responder.handleMessage(m, addr, new ReplyHandlerStub() {
            public String getAddress() {
                return "1.2.3.5";
            }
        });
        
        assertNull(responder.msg);
        assertNull(responder.addr);
        assertNull(responder.handler);
    }

    /**
     * tests that messages are verified.
     */
    public void testSecure() throws Exception {
        TestVerifier verifier = new TestVerifier();
        
        TestResponder responder = new TestResponder(verifier);
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLocalHost(),1000);
        ReplyHandler h = new ReplyHandlerStub() {
            public String getAddress() {
                return "1.2.3.4";
            }
        };
        TestGGEPMessage m = new TestGGEPMessage();
        
        // the message should be sent for verification 
        m.returnAddr = new IpPortImpl("1.2.3.4",5);
        responder.handleMessage(m, addr, h);
        assertSame(verifier.sm, m);
        
        // if it doesn't verify, it wont' get processed
        verifier.smc.handleSecureMessage(verifier.sm, false);
        assertNull(responder.msg);
        
        // but a message that verifies will be processed
        verifier.smc.handleSecureMessage(verifier.sm, true);
        assertSame(m,responder.msg);
    }
    
    /** tests that the return address of a message is respected */
    public void testReturnAddress() throws Exception {
        TestResponder responder = new TestResponder(null);
        final IpPort allowed = new IpPortImpl("1.2.3.4",100);
        final IpPort notAllowed = new IpPortImpl("1.2.3.5",100);
        
        // try a message that comes from an allowed host but wants to go
        // to a non-allowed return address.  It should not be handled.
        TestGGEPMessage m = new TestGGEPMessage();
        m.returnAddr = notAllowed;
        responder.handleMessage(m, addr, h);
        assertNull(responder.msg);
        
        // try a message that comes from a non-allowed host but wants to
        // go to an allowed return address
        m.returnAddr = allowed;
        ReplyHandler notAllowedHandler = new ReplyHandlerStub() {
            public String getAddress() {
                return "1.2.3.6";
            }
        };
        responder.handleMessage(m,addr,notAllowedHandler);
        assertSame(m,responder.msg);
        assertEquals(allowed.getAddress(),responder.handler.getAddress());
        
        // if both the return address and source address are allowed, the
        // return address takes precedence.
        responder = new TestResponder(null);
        m.version++;
        responder.handleMessage(m,addr,h);
        assertSame(m,responder.msg);
        assertEquals(allowed.getAddress(),responder.handler.getAddress());
    }
    
    /**
     * tests that only newer messages are handled.
     */
    public void testVersioning() throws Exception {
        TestResponder responder = new TestResponder(null);
        TestGGEPMessage m = new TestGGEPMessage();
        responder.handleMessage(m, addr, h);
        assertSame(m,responder.msg);
        
        // now try the same message again, it should not be handled.
        responder.msg = null;
        responder.handleMessage(m, addr, h);
        assertNull(responder.msg);
        
        // but if the version number is higher, it gets handled.
        m.version = 2;
        responder.handleMessage(m, addr, h);
        assertSame(m,responder.msg);
        
        // a message w/o a version will not go through.
        m.version = -1;
        responder.msg = null;
        responder.handleMessage(m, addr, h);
        assertNull(responder.msg);
        
        // but if it has a version it will have to be higher than the current.
        responder.msg = null;
        m.version = 1;
        responder.handleMessage(m, addr, h);
        assertNull(responder.msg);
        
        // create a new responder - it should look up the last routed version
        // from the persisted setting.
        responder = new TestResponder(null);
        responder.handleMessage(m, addr, h);
        assertNull(responder.msg);
        m.version = 3;
        responder.handleMessage(m, addr, h);
        assertSame(m,responder.msg);
        assertEquals(3, versionSetting.getValue());
    }
    
    public void testDestinationAddress() throws Exception {
        TestResponder responder = new TestResponder(null);
        TestGGEPMessage m = new TestGGEPMessage();
        
        // a message w/o a version will go through if it has the proper
        // destination address
        m.version = -1;
        m.destAddr = new IpPortImpl(InetAddress.getByAddress(networkManager.getExternalAddress()),1234);
        responder.handleMessage(m, addr, h);
        assertSame(m, responder.msg);
        
        // if the destination address is wrong, it is dropped.
        m.destAddr = new IpPortImpl("1.2.3.4",100);
        responder.msg = null;
        responder.handleMessage(m, addr, h);
        assertNull(responder.msg);
    }
    
    private static int simppVersion;
    private void triggerSimppUpdate() throws Exception {
        List<SimppListener> l = (List<SimppListener>) 
            PrivilegedAccessor.getValue(simppManager, "listeners");
        for (SimppListener s : l)
            s.simppUpdated(simppVersion++);
    }
    
    private class TestResponder extends RestrictedResponder {

        Message msg;
        InetSocketAddress addr;
        ReplyHandler handler;
        public TestResponder(SecureMessageVerifier verifier) {
            super(ipSetting, verifier, versionSetting, networkManager, simppManager, udpReplyHandlerFactory, udpReplyHandlerCache, 
                    new ImmediateExecutor(), new SimpleNetworkInstanceUtils());
        }

        @Override
        protected void processAllowedMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            this.msg = msg;
            this.addr = addr;
            this.handler = handler;
        }
        
        void reset() {
            this.msg = null;
            this.addr = null;
            this.handler = null;
        }
    }
    
    private static class TestVerifier implements SecureMessageVerifier {
        SecureMessage sm;
        SecureMessageCallback smc;
        TestVerifier() {
        }
        public void verify(Verifier verifier) {
            sm = verifier.getSecureMessage();
            smc = verifier.getSecureMessageCallback();
        }
        
        public void verify(SecureMessage sm, SecureMessageCallback smc) {
            this.sm = sm;
            this.smc = smc;
        }
        
        public void verify(PublicKey pubKey, String algorithm, 
                SecureMessage sm, SecureMessageCallback smc) {
            this.sm = sm;
            this.smc = smc;
        }
    }
    
    private static class TestGGEPMessage extends RoutableGGEPMessage {
        int version = 1;
        IpPort returnAddr, destAddr;
        protected TestGGEPMessage() throws BadPacketException {
            super(new byte[4], 1, 1, new RoutableGGEPMessage.GGEPSigner() {
                public GGEP getSecureGGEP(GGEP original) {
                    return original;
                }
            },new GGEP(true));
        }
        
        @Override
        public long getRoutableVersion() {
            return version;
        }
        
        @Override
        public IpPort getReturnAddress() {
            return returnAddr;
        }
        @Override
        public IpPort getDestinationAddress() {
            return destAddr;
        }
    }
    
    private static class ImmediateExecutor implements Executor {

        public void execute(Runnable command) {
            command.run();
        }
    }
}
