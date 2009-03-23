package com.limegroup.gnutella.util;

import java.util.Collections;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;

import com.limegroup.gnutella.messages.GGEPKeys;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.Message.Network;

/**
 * Utility class for creating common message types for tests.
 */
public class MessageTestUtils {

    /**
     * Should never be instantiated.
     */
    private MessageTestUtils() {}
    
    /**
     * Creates a new <tt>PingReply</tt> instance with the GGEP extension 
     * advertising free ultrapeer and leaf slots.  The generated pong will
     * have a random "unique" IP address that is statistically unlikely to 
     * collide with other addresses returned by this method.
     * 
     * @return a new <tt>PingReply</tt> for testing with the GGEP extension 
     *  advertising free ultrapeer and leaf slots
     */
    public static PingReply createPongWithFreeLeafSlots(PingReplyFactory pingReplyFactory) {
        GGEP ggep = newGGEP(20, true, true, true, false);
        
        byte a = (byte)(40 + (Math.random()*80));
        byte b = (byte)(40 + (Math.random()*80));
        byte c = (byte)(40 + (Math.random()*80));
        byte d = (byte)(40 + (Math.random()*80));
        
        PingReply pr = pingReplyFactory.create(GUID.makeGuid(), (byte)1, 6346, 
            new byte[]{a,b,c,d}, 10, 10, true, ggep);
        return pr;
    }
    
    public static Expectations createDefaultMessageExpectations(final Message msg, final Class<? extends Message> handlerClass) {
        return new Expectations() {{
            allowing(msg).getGUID();
            will(returnValue(new GUID().bytes()));
            allowing(msg).getHops();
            will(returnValue((byte)6));
            allowing(msg).getTTL();
            will(returnValue((byte)2));
            allowing(msg).getTotalLength();
            will(returnValue(32));
            allowing(msg).getLength();
            will(returnValue(9));
            allowing(msg).getHandlerClass();
            will(returnValue(handlerClass));
            allowing(msg).getNetwork();
            will(returnValue(Network.TCP));
            allowing(msg).hop();
        }};
    }
    
    public static Expectations createDefaultQueryExpectations(final QueryRequest qr) {
        return new Expectations(){{
            allowing(qr).desiresAll();
            will(returnValue(true));
            allowing(qr).getQueryUrns();
            will(returnValue(Collections.emptySet()));
            ignoring(qr).getRichQuery();
            ignoring(qr).hasQueryUrns();
            ignoring(qr).isWhatIsNewRequest();
            ignoring(qr).desiresXMLResponses();
            ignoring(qr).desiresOutOfBandReplies();
            ignoring(qr).shouldIncludeXMLInResponse();
        }};
    }
    
    /**
     * Creates a new <tt>PingReply</tt> instance with the GGEP extension 
     * advertising free ultrapeer and leaf slots.  The generated pong will
     * have a random "unique" IP address that is statistically unlikely to 
     * collide with other addresses returned by this method.
     * 
     * @return a new <tt>PingReply</tt> for testing with the GGEP extension 
     *  advertising free ultrapeer and leaf slots
     */
    
    public static void mockPongWithFreeLeafSlots(Mockery context, final PingReply pingReply) {
        final byte a = (byte)(40 + (Math.random()*80));
        final byte b = (byte)(40 + (Math.random()*80));
        final byte c = (byte)(40 + (Math.random()*80));
        final byte d = (byte)(40 + (Math.random()*80));
        
        final GUID guid = new GUID();
        
        context.checking(new Expectations() {{
            allowing(pingReply).getGUID();
            will(returnValue(guid.bytes()));
            allowing(pingReply).getTTL();
            will(returnValue((byte)1));
            allowing(pingReply).getHops();
            will(returnValue((byte)0));
            allowing(pingReply).getPort();
            will(returnValue(6346));
            allowing(pingReply).getIPBytes();
            will(returnValue(new byte[] { a, b, c, d }));
            allowing(pingReply).getFiles();
            will(returnValue(10));
            allowing(pingReply).getKbytes();
            will(returnValue(10));
            allowing(pingReply).isUltrapeer();
            will(returnValue(true));
            allowing(pingReply).getDailyUptime();
            will(returnValue(20));
            allowing(pingReply).supportsUnicast();
            will(returnValue(true));
            allowing(pingReply).getNumFreeLocaleSlots();
            will(returnValue(10));
            allowing(pingReply).getNumUltrapeerSlots();
            will(returnValue(0));
            allowing(pingReply).getClientLocale();
            will(returnValue(ApplicationSettings.DEFAULT_LOCALE.getValue()));
            // this could also be moved to a generic message mocking method
            allowing(pingReply).getCreationTime();
            will(returnValue(System.currentTimeMillis()));
        }});
    }
    
    /**
     * Creates a new <tt>PingReply</tt> instance with the GGEP extension 
     * advertising free ultrapeer and leaf slots.  The generated pong will
     * have a random "unique" IP address that is statistically unlikely to 
     * collide with other addresses returned by this method.
     * @param pingReplyFactory 
     * 
     * @return a new <tt>PingReply</tt> for testing with the GGEP extension 
     *  advertising free ultrapeer and leaf slots
     */
    public static PingReply createPongWithUltrapeerSlots(PingReplyFactory pingReplyFactory) {
        GGEP ggep = newGGEP(20, true, true, false, true);
        
        byte a = (byte)(40 + (Math.random()*80));
        byte b = (byte)(40 + (Math.random()*80));
        byte c = (byte)(40 + (Math.random()*80));
        byte d = (byte)(40 + (Math.random()*80));
        
        PingReply pr = pingReplyFactory.create(GUID.makeGuid(), (byte)1, 6346, 
            new byte[]{a,b,c,d}, 10, 10, true, ggep);
        return pr;
    }

    /** 
     * Returns the GGEP payload bytes to encode the given uptime. 
     */
    private static GGEP newGGEP(int dailyUptime, boolean isUltrapeer,
                                boolean isGUESSCapable, boolean freeLeaf, 
                                boolean freeUP) {
        GGEP ggep = new GGEP();
        
        if (dailyUptime >= 0)
            ggep.put(GGEPKeys.GGEP_HEADER_DAILY_AVERAGE_UPTIME, dailyUptime);
        
        if (isGUESSCapable && isUltrapeer) {
            ggep.put(GGEPKeys.GGEP_HEADER_UNICAST_SUPPORT);
        }
        
        if (isUltrapeer) { 
            // indicate UP support
            addUltrapeerExtension(ggep, freeLeaf, freeUP);
        }

        return ggep;
    }


    /**
     * Adds the ultrapeer GGEP extension to the pong.  This has the version of
     * the Ultrapeer protocol that we support as well as the number of free
     * leaf and Ultrapeer slots available.
     * 
     * @param ggep the <tt>GGEP</tt> instance to add the extension to
     */
    private static void addUltrapeerExtension(GGEP ggep, boolean freeLeaf, 
                                              boolean freeUp) {
        byte[] payload = new byte[3];
        payload[0] = 0;
        payload[1] = freeLeaf ? (byte)10 : (byte)0;
        payload[2] = freeUp ? (byte)10 : (byte)0;
        ggep.put(GGEPKeys.GGEP_HEADER_UP_SUPPORT, payload);
    }
}
