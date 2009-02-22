package org.limewire.io;

import org.limewire.util.BaseTestCase;

public class ConnectableImplTest extends BaseTestCase {

    public ConnectableImplTest(String name) {
        super(name);
    }
    
    public void testEqualsWithDifferentConstructors() throws Exception {
        ConnectableImpl address1 = new ConnectableImpl("localhost:4545", true);
        ConnectableImpl address2 = new ConnectableImpl("127.0.0.1:4545", true);
        assertEquals(address1, address2);
    }

}
