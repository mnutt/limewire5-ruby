package com.limegroup.gnutella.routing;


import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.lifecycle.ServiceRegistry;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileDescStub;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.LibraryStubModule;
import com.limegroup.gnutella.stubs.QueryRequestStub;

public class QRPUpdaterTest extends LimeTestCase {

    @Inject private QRPUpdater qrpUpdater;
    @Inject private Injector injector;
    @Inject @GnutellaFiles private FileCollection gnutellaFileCollection;
    
    public QRPUpdaterTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(QRPUpdaterTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        injector = LimeTestUtils.createInjector(new LibraryStubModule(), LimeTestUtils.createModule(this));
        injector.getInstance(ServiceRegistry.class).initialize();
    }
    
    public void testGetQRT() {
        QueryRouteTable table = qrpUpdater.getQRT();

        //only defaults should be found
        assertTrue(table.contains(new QueryRequestImpl("limewire")));
        assertFalse(table.contains(new QueryRequestImpl("FoundFile.txt")));
        assertFalse(table.contains(new QueryRequestImpl("NotFound.txt")));
        
        FileDesc fd = new FileDescStub("FoundFile.txt");
        gnutellaFileCollection.add(fd);
        
        table = qrpUpdater.getQRT();
        assertTrue(table.contains(new QueryRequestImpl("limewire")));
        assertTrue(table.contains(new QueryRequestImpl("FoundFile.txt")));
        assertFalse(table.contains(new QueryRequestImpl("NotFound.txt")));
        
        gnutellaFileCollection.remove(fd);
        table = qrpUpdater.getQRT();
        assertTrue(table.contains(new QueryRequestImpl("limewire")));
        assertFalse(table.contains(new QueryRequestImpl("FoundFile.txt")));
        assertFalse(table.contains(new QueryRequestImpl("NotFound.txt")));
    }
    
    private class QueryRequestImpl extends QueryRequestStub {
        
        private String keyWords;
        
        public QueryRequestImpl(String keywords) {
            this.keyWords = keywords;
        }
        
        @Override
        public String getQuery() {
            return keyWords;
        }
    }
}
