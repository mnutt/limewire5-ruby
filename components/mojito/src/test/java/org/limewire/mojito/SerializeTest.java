package org.limewire.mojito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

import junit.framework.TestSuite;

import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.db.impl.DatabaseImpl;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.routing.impl.RouteTableImpl;

public class SerializeTest extends MojitoTestCase {
    
    public SerializeTest(String name) {
        super(name);
    }
   
    public static TestSuite suite() {
        return buildTestSuite(SerializeTest.class);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSerializeRouteTable() throws IOException, ClassNotFoundException {
        RouteTable routeTable1 = new RouteTableImpl();
        for (int i = 0; i < 100; i++) {
            Contact node = ContactFactory.createUnknownContact(
                    Vendor.UNKNOWN, 
                    Version.ZERO, 
                    KUID.createRandomID(), 
                    new InetSocketAddress("localhost", 2000 + i));
            routeTable1.add(node);
        }
        
        assertEquals(101, routeTable1.getContacts().size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(routeTable1);
        oos.close();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        RouteTable routeTable2 = (RouteTable)ois.readObject();
        ois.close();
        
        assertNotSame(routeTable1, routeTable2);
        assertEquals(101, routeTable2.getContacts().size());
        for (Contact node : routeTable1.getContacts()) {
            Contact other = routeTable2.get(node.getNodeID());
            assertNotNull(other);
            assertEquals(node, other);
            assertNotSame(node, other);
        }
    }
    
    public void testSerializeDatabase() throws IOException, ClassNotFoundException {
        Database database1 = new DatabaseImpl();
        
        for (int i = 0; i < 100; i++) {
            SocketAddress addr = new InetSocketAddress("192.168.1." + i, 2000 + i);
            Contact node = ContactFactory.createLiveContact(
                    addr, 
                    Vendor.UNKNOWN, 
                    Version.ZERO, 
                    KUID.createRandomID(), 
                    addr, 0, 
                    Contact.DEFAULT_FLAG);
            
            KUID primaryKey = KUID.createRandomID();
            
            DHTValueEntity entity = DHTValueEntity.createFromRemote(node, node, primaryKey, 
                    new DHTValueImpl(DHTValueType.TEST, Version.ZERO, "Hello World".getBytes()));
            
            database1.store(entity);
        }
        
        assertEquals(100, database1.getValueCount());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(database1);
        oos.close();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Database database2 = (Database)ois.readObject();
        ois.close();
        
        assertNotSame(database1, database2);
        assertEquals(100, database2.getValueCount());
        for (DHTValueEntity entity : database1.values()) {
            Map<KUID, DHTValueEntity> bag = database2.get(entity.getPrimaryKey());
            assertNotNull(bag);
            
            DHTValueEntity other = bag.get(entity.getSecondaryKey());
            assertNotNull(other);
            
            assertEquals(entity, other);
            assertNotSame(entity, other);
        }
    }
}
