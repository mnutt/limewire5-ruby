package com.limegroup.gnutella;

import java.net.Socket;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.gnutella.tests.LimeTestUtils;

import junit.framework.Test;

import com.google.inject.Injector;
import com.google.inject.Stage;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class FirewalledStatusTest extends ClientSideTestCase {

    private Injector injector;
    private NetworkManager networkManager;
    
    public FirewalledStatusTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(FirewalledStatusTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    public void setUp() throws Exception {
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        super.setUp(injector);
        networkManager = injector.getInstance(NetworkManager.class);
    }
    
    @Override
    public void setSettings() {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(true);
    }        

    ///////////////////////// Actual Tests ////////////////////////////
    
    public void testStillFirewalledAfterLocalConnect() throws Exception {

        assertFalse(networkManager.acceptedIncomingConnection());

        Socket incoming = new Socket("localhost", SERVER_PORT);
        incoming.close();

        assertFalse(networkManager.acceptedIncomingConnection());
    }


    //////////////////////////////////////////////////////////////////

    @Override
    public int getNumberOfPeers() {
        return 1;
    }
}

