package com.limegroup.gnutella.lws.server;

import java.io.IOException;
import java.util.HashMap;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.limewire.core.settings.LWSSettings;
import org.limewire.lws.server.LWSDispatcherFactoryImpl;
import org.limewire.lws.server.StringCallback;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * This checks that we handle an empty hostname setting OK.
 */
public class LWSManagerImplTest extends LimeTestCase {

    public LWSManagerImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LWSManagerImplTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testSendingMessageToServerWithEmptyHost() throws IOException {
        Injector injector = LimeTestUtils.createInjector();
        // precondition: host should NOT be empty when we're not setting remotely
        //               but set to empty now for tseting if it were empty
        LWSSettings.LWS_AUTHENTICATION_HOSTNAME.setValue("");
        LWSSettings.LWS_DOWNLOAD_PREFIX.setValue("");
        assertEquals("", LWSSettings.LWS_AUTHENTICATION_HOSTNAME.getValue());
        
        LWSManagerImpl lwsManagerImpl = new LWSManagerImpl(injector.getInstance(HttpExecutor.class), new LWSDispatcherFactoryImpl());

        try {
            lwsManagerImpl.sendMessageToServer("blah", new HashMap<String, String>(), new StringCallback() {
                public void process(String response) {
                }
            });
            fail("expected iox");
        } catch(IOException iox) {
            assertEquals("null host!", iox.getMessage());
        }
    }
    
    public void testSendingMessageToServerWithEmptyDownloadPrefix() throws IOException {
        Injector injector = LimeTestUtils.createInjector();
        // precondition: download prefix should NOT be empty when we're not setting remotely
        //               but set to empty now for tseting if it were empty
        LWSSettings.LWS_DOWNLOAD_PREFIX.setValue("");
        LWSSettings.LWS_AUTHENTICATION_HOSTNAME.setValue("");
        assertEquals("", LWSSettings.LWS_DOWNLOAD_PREFIX.getValue());
        
        LWSManagerImpl lwsManagerImpl = new LWSManagerImpl(injector.getInstance(HttpExecutor.class), new LWSDispatcherFactoryImpl());

        try {
            lwsManagerImpl.sendMessageToServer("blah", new HashMap<String, String>(), new StringCallback() {
                public void process(String response) {
                }
            });
            fail("expected iox");
        } catch(IOException iox) {
            assertEquals("null host!", iox.getMessage());
        }
    }      

}
