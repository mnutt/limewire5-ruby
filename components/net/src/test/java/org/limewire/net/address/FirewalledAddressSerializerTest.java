package org.limewire.net.address;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.Test;

import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.util.BaseTestCase;
import org.limewire.util.TestUtils;

public class FirewalledAddressSerializerTest extends BaseTestCase {

    private FirewalledAddressSerializer serializer;
    private ConnectableSerializer connectableSerializer;

    public FirewalledAddressSerializerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(FirewalledAddressSerializerTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        serializer = new FirewalledAddressSerializer(new ConnectableSerializer());
        connectableSerializer = new ConnectableSerializer();
    }
    
    public void testSerialize() throws Exception {
        GUID guid = new GUID();
        Set<Connectable> proxies = new LinkedHashSet<Connectable>();
        Connectable proxy1 = new ConnectableImpl("127.0.0.1", 10, true);
        Connectable proxy2 = new ConnectableImpl("129.0.0.1", 15, false);
        proxies.add(proxy1);
        proxies.add(proxy2);
        FirewalledAddress address = new FirewalledAddress(proxy1, proxy2, guid, proxies, 1);
        byte[] serialized = serializer.serialize(address);
        GGEP ggep = new GGEP(serialized, 0);
        assertTrue(ggep.hasKey("PU"));
        assertTrue(ggep.hasKey("PR"));
        assertTrue(ggep.hasKey("PX"));
        assertTrue(ggep.hasKey("GU"));
        assertTrue(ggep.hasKey("FW"));
        
        Connectable readAddress = connectableSerializer.deserialize(ggep.getBytes("PU"));
        assertEquals("127.0.0.1", readAddress.getAddress());
        assertEquals(10, readAddress.getPort());
        assertTrue(readAddress.isTLSCapable());
        
        readAddress = connectableSerializer.deserialize(ggep.getBytes("PR"));
        assertEquals("129.0.0.1", readAddress.getAddress());
        assertEquals(15, readAddress.getPort());
        assertFalse(readAddress.isTLSCapable());
        
        Set<Connectable> readproxies = connectableSerializer.deserializeSet(ggep.getBytes("PX"));
        assertContains(readproxies, proxy1);
        assertContains(readproxies, proxy2);
        
        assertEquals(guid.bytes(), ggep.getBytes("GU"));
        assertEquals(1, ggep.getInt("FW"));
    }
    
    public void testDeserializeFromBytes() throws IOException {
        File serializedAddressFile = TestUtils.getResourceInPackage("firewalled_address.txt", FirewalledAddressSerializerTest.class);
        byte [] serialzedAddress = IOUtils.readFully(new FileInputStream(serializedAddressFile));
        Object address = serializer.deserialize(serialzedAddress);
        assertInstanceof(FirewalledAddress.class, address);
        FirewalledAddress firewalledAddress = (FirewalledAddress)address;
        
        GUID guid = new GUID("8D342E7D874A0AB26B0A4E3627C76F00");
        Set<Connectable> proxies = new LinkedHashSet<Connectable>();
        Connectable proxy1 = new ConnectableImpl("127.0.0.1", 10, true);
        Connectable proxy2 = new ConnectableImpl("129.0.0.1", 15, false);
        proxies.add(proxy1);
        proxies.add(proxy2);
        FirewalledAddress expectedAddress = new FirewalledAddress(proxy1, proxy2, guid, proxies, 1);
        assertEquals(expectedAddress, firewalledAddress);
    }
    
    public void testDeserializeFromString() {
        try {
            serializer.deserialize("192.168.1.1");
            fail();
        } catch (IOException e) {
            // expected result
        }
    }
    
    public void testDeserializeNotEnoughData() {
        try {
            serializer.deserialize(new byte[0]);
            fail("io exception excepted, because of end of stream");
        } catch (IOException ie) {
        }
        try {
            serializer.deserialize(new byte[1]);
            fail("io exception excepted, because of end of stream");
        } catch (IOException ie) {
        }
    }

}
