package com.limegroup.gnutella.library;

import static com.limegroup.gnutella.library.FileManagerTestUtils.*;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.lifecycle.ServiceRegistry;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.util.LimeTestCase;

public class SharedFileKeywordsIndexImplIntegrationTest extends LimeTestCase {

    private QueryRequestFactory queryRequestFactory;
    private ManagedFileList managedList;
    private GnutellaFileList fileList;
    private SharedFilesKeywordIndex keywordIndex;
    private Response[] responses;
    private Injector injector;

    private File f1, f2, f3;

    public SharedFileKeywordsIndexImplIntegrationTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SharedFileKeywordsIndexImplIntegrationTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        fileList = injector.getInstance(FileManager.class).getGnutellaFileList();
        managedList = injector.getInstance(FileManager.class).getManagedFileList();
        keywordIndex = injector.getInstance(SharedFilesKeywordIndex.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        injector.getInstance(ServiceRegistry.class).initialize();
        assertLoads(managedList); // Ensure it starts up & schemas load & all.
    }
    
    public void testOneSharedFile() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);

        assertAdds(fileList, f1);

        // it is important to check the query at all bounds,
        // // including tests for case.
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        responses = keywordIndex.query(queryRequestFactory.createQuery("unit", (byte) 3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses = keywordIndex.query(queryRequestFactory.createQuery("FileManager", (byte) 3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses = keywordIndex.query(queryRequestFactory.createQuery("test", (byte) 3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses = keywordIndex.query(queryRequestFactory.createQuery("file", (byte) 3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses = keywordIndex.query(queryRequestFactory.createQuery("FileManager UNIT tEsT",
                (byte) 3));
        assertEquals("Unexpected number of responses", 1, responses.length);
    }
    
    public void testAddAnotherSharedFile() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        
        assertAdds(fileList, f1, f2);
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("unit", (byte) 3));
        assertNotEquals("responses gave same index", responses[0].getIndex(), responses[1].getIndex());
        for (int i = 0; i < responses.length; i++) {
            assertTrue("responses should be expected indexes",
                    responses[i].getIndex() == 0 || responses[i].getIndex() == 1);
        }
    }
    
    public void testRemovingOneSharedFile() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);
        
        assertAdds(fileList, f1, f2);

        // Remove file that's shared. Back to 1 file.
        fileList.remove(f2);
        responses = keywordIndex.query(queryRequestFactory.createQuery("unit", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);
    }

    public void testAddAnotherSharedFileDifferentIndex() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);
        
        assertAdds(fileList, f1, f2);
        fileList.remove(f2);
        assertAdds(fileList, f3);
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("unit", (byte) 3));
        assertEquals("unexpected response length", 2, responses.length);
        assertNotEquals("unexpected response[0] index", 1, responses[0].getIndex());
        assertNotEquals("unexpected response[1] index", 1, responses[1].getIndex());
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("*unit*", (byte) 3));
        assertEquals("unexpected responses length", 2, responses.length);
    }
    
    public void testRenameFiles() throws Exception {
        f1 = createNewNamedTestFile(1, "1111", _scratchDir);
        f2 = createNewNamedTestFile(3, "2222", _scratchDir);
        f3 = createNewNamedTestFile(11, "3333", _scratchDir);
        
        assertAdds(fileList, f1, f3);
        assertEquals(2, fileList.size());       
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("1111", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("2222", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);

        responses = keywordIndex.query(queryRequestFactory.createQuery("3333", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);
        
        try {
            managedList.fileRenamed(f1, new File(_scratchDir, "!<invalid file>")).get(1, TimeUnit.SECONDS);
            fail("should have failed");
        } catch(ExecutionException expected) {}
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("1111", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("2222", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);

        responses = keywordIndex.query(queryRequestFactory.createQuery("3333", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);
        
        assertFileRenames(managedList, f3, f2);
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("1111", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("2222", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);

        responses = keywordIndex.query(queryRequestFactory.createQuery("3333", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);
        
        try {
            managedList.fileRenamed(f1, f3).get(1, TimeUnit.SECONDS);
            fail("should have failed");
        } catch(ExecutionException expected) {}
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("1111", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("2222", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);

        responses = keywordIndex.query(queryRequestFactory.createQuery("3333", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);
    }

    public void testChangeSharedFile() throws Exception {
        f1 = createNewNamedTestFile(100, "name", _scratchDir);
        f2 = createNewTestFile(10, _scratchDir);
        
        assertAdds(fileList, f1);
        responses = keywordIndex.query(queryRequestFactory.createQuery("name", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);        
        
        change(f1);
        assertFileChanges(managedList, f1);
        responses = keywordIndex.query(queryRequestFactory.createQuery("name", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);
        
        f1.delete();
        assertFileChangedFails("NOT_MANAGEABLE", managedList, f1);
        responses = keywordIndex.query(queryRequestFactory.createQuery("name", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);
        
        assertFileChangedFails("OLD_WASNT_MANAGED", managedList, f2);
        responses = keywordIndex.query(queryRequestFactory.createQuery("name", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);
    }
    
}
