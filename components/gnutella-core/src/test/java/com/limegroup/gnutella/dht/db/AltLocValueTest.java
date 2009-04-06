package com.limegroup.gnutella.dht.db;

import java.io.ByteArrayOutputStream;
import java.util.Random;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GUID;
import org.limewire.mojito.routing.Version;

import com.google.inject.Injector;
import com.limegroup.gnutella.dht.DHTTestCase;
import com.limegroup.gnutella.security.MerkleTree;

public class AltLocValueTest extends DHTTestCase {
    
    private AltLocValueFactoryImpl altLocValueFactory;

    public AltLocValueTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AltLocValueTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        altLocValueFactory = (AltLocValueFactoryImpl) injector.getInstance(AltLocValueFactory.class);
    }
    
    
    public void testSerializationVersionZero() throws Exception {
        byte[] guid = GUID.makeGuid();
        int port = 1234;
        boolean firewalled = true;
        
        AltLocValue value1 = altLocValueFactory.createAltLocValue(Version.ZERO, guid, port, -1L, null, firewalled);
        
        assertEquals(guid, value1.getGUID());
        assertEquals(port, value1.getPort());
        assertEquals(firewalled, value1.isFirewalled());
        
        // Serialize it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        value1.write(baos);
        
        // Get the raw bytes
        byte[] serialized = baos.toByteArray();
        
        // De-serialize it
        AltLocValue value2 = altLocValueFactory.createFromData(Version.ZERO, serialized);
        
        // Should be the same
        assertEquals(value1.getGUID(), value2.getGUID());
        assertEquals(value1.getPort(), value2.getPort());
        assertEquals(-1L, value2.getFileSize());
        assertEquals(null, value2.getRootHash());
        assertEquals(value1.isFirewalled(), value2.isFirewalled());
    }
    
    public void testSerializationVersionOne() throws Exception {
        byte[] guid = GUID.makeGuid();
        int port = 1234;
        long fileSize = 334455;
        byte[] ttroot = new byte[MerkleTree.HASHSIZE];
        boolean firewalled = true;
        
        Random random = new Random();
        random.nextBytes(ttroot);
        
        AltLocValue value1 = altLocValueFactory.createAltLocValue(
            AbstractAltLocValue.VERSION_ONE, guid, port, fileSize, ttroot, firewalled);
        
        assertEquals(guid, value1.getGUID());
        assertEquals(port, value1.getPort());
        assertEquals(firewalled, value1.isFirewalled());
        
        // Serialize it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        value1.write(baos);
        
        // Get the raw bytes
        byte[] serialized = baos.toByteArray();
        
        // De-serialize it
        AltLocValue value2 = altLocValueFactory.createFromData(AbstractAltLocValue.VERSION_ONE, serialized);
        
        // Should be the same
        assertEquals(value1.getGUID(), value2.getGUID());
        assertEquals(value1.getPort(), value2.getPort());
        assertEquals(value1.getFileSize(), value2.getFileSize());
        assertEquals(value1.getRootHash(), value2.getRootHash());
        assertEquals(value1.isFirewalled(), value2.isFirewalled());
        
        // De-serialize it but do as if it's a Version 0 value!
        // The File size and TigerTree root hash should be missing!
        AltLocValue value3 = altLocValueFactory.createFromData(Version.ZERO, serialized);
        
        // Should be the same
        assertEquals(value1.getGUID(), value3.getGUID());
        assertEquals(value1.getPort(), value3.getPort());
        assertEquals(-1L, value3.getFileSize());
        assertEquals(null, value3.getRootHash());
        assertEquals(value1.isFirewalled(), value3.isFirewalled());
    }
}
