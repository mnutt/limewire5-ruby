package com.limegroup.gnutella.routing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;

import junit.framework.Test;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Unit tests for PatchTableMessage
 */
public class PatchTableMessageTest extends LimeTestCase {
        
	private MessageFactory messageFactory;

    public PatchTableMessageTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(PatchTableMessageTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	@Override
	protected void setUp() throws Exception {
	    Injector injector = LimeTestUtils.createInjector();
		messageFactory = injector.getInstance(MessageFactory.class);
	}
	
    /** Unit test */
    public void testLegacy() throws Exception {
        //From scratch.  Check encode.
        PatchTableMessage m=new PatchTableMessage(
            (short)3, (short)255, PatchTableMessage.COMPRESSOR_NONE, (byte)2,
            new byte[] {(byte)0, (byte)0xAB, (byte)0xCD, (byte)0},
            1, 3);
        assertEquals(ResetTableMessage.PATCH_VARIANT, m.getVariant());
        assertEquals((byte)1, m.getTTL());
        assertEquals(255, m.getSequenceSize());
        assertEquals(3, m.getSequenceNumber());
        assertEquals(PatchTableMessage.COMPRESSOR_NONE, m.getCompressor());
        assertEquals(2, m.getEntryBits());
        assertTrue(Arrays.equals(m.getData(),
                                  new byte[] {(byte)0xAB, (byte)0xCD }));
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        m.write(out);
        out.flush();

        assertEquals(ResetTableMessage.PATCH_VARIANT, m.getVariant());
        assertEquals((byte)1, m.getTTL());
        assertEquals(255, m.getSequenceSize());
        assertEquals(3, m.getSequenceNumber());
        assertEquals(PatchTableMessage.COMPRESSOR_NONE, m.getCompressor());
        assertEquals(2, m.getEntryBits());
        assertTrue(Arrays.equals(m.getData(),
                                  new byte[] {(byte)0xAB, (byte)0xCD }));

        //Read from bytes
        byte[] message=new byte[23+5+2];
        message[16]=Message.F_ROUTE_TABLE_UPDATE;            //function code
        message[17]=(byte)1;                                 //TTL
        message[19]=(byte)7;                                 //payload length
        message[23+0]=RouteTableMessage.PATCH_VARIANT;       //patch variant
        message[23+1]=(byte)1;                               //sequence 1...
        message[23+2]=(byte)0xFF;                            //...of 255
        message[23+3]=PatchTableMessage.COMPRESSOR_DEFLATE;                       //comrpessor
        message[23+4]=(byte)2;                               //entry bits
        message[23+5]=(byte)0xAB;                            //data
        message[23+6]=(byte)0xCD;
        m=read(message);
        assertEquals(RouteTableMessage.PATCH_VARIANT, m.getVariant());
        assertEquals((byte)1, m.getTTL());
        assertEquals(1, m.getSequenceNumber());
        assertEquals(255, m.getSequenceSize());
        assertEquals(PatchTableMessage.COMPRESSOR_DEFLATE, m.getCompressor());
        assertEquals(2, m.getEntryBits());
        assertEquals(2, m.getData().length);
        assertEquals((byte)0xAB, m.getData()[0]);
        assertEquals((byte)0xCD, m.getData()[1]);
    }

    private PatchTableMessage read(byte[] bytes) throws Exception {
        InputStream in=new ByteArrayInputStream(bytes);
        return (PatchTableMessage)messageFactory.read(in, Network.TCP);
    }
}
