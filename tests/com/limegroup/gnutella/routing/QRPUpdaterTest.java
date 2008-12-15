package com.limegroup.gnutella.routing;


import java.util.LinkedList;
import java.util.List;

import junit.framework.Test;

import org.limewire.lifecycle.ServiceRegistry;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileDescStub;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FileManagerStub;
import com.limegroup.gnutella.stubs.QueryRequestStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class QRPUpdaterTest extends LimeTestCase {

    private QRPUpdater qrpUpdater;
    private Injector injector;
    private FileManagerStub fileManagerStub;
    
    public QRPUpdaterTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(QRPUpdaterTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        
        List<Module> allModules = new LinkedList<Module>();
        allModules.add(new AbstractModule() {
            @Override
             protected void configure() {
                bind(FileManager.class).to(FileManagerStub.class);
             } 
         });
        injector = LimeTestUtils.createInjector(allModules.toArray(new Module[0]));
        
        qrpUpdater = injector.getInstance(QRPUpdater.class);
        fileManagerStub = (FileManagerStub)injector.getInstance(FileManager.class);
        
        injector.getInstance(ServiceRegistry.class).initialize();
        fileManagerStub.start();
    }
    
    public void testGetQRT() {
        QueryRouteTable table = qrpUpdater.getQRT();

        //only defaults should be found
        assertTrue(table.contains(new QueryRequestImpl("limewire")));
        assertFalse(table.contains(new QueryRequestImpl("FoundFile.txt")));
        assertFalse(table.contains(new QueryRequestImpl("NotFound.txt")));
        
        FileDesc fd = new FileDescStub("FoundFile.txt");
        fileManagerStub.getGnutellaFileList().add(fd);
        
        table = qrpUpdater.getQRT();
        assertTrue(table.contains(new QueryRequestImpl("limewire")));
        assertTrue(table.contains(new QueryRequestImpl("FoundFile.txt")));
        assertFalse(table.contains(new QueryRequestImpl("NotFound.txt")));
        
        fileManagerStub.getGnutellaFileList().remove(fd);
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
