package com.limegroup.gnutella.library;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.lifecycle.ServiceScheduler;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.TestUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.helpers.UrnHelper;


/**
 * Tests the public methods of the UrnCache class.
 */
@SuppressWarnings("unchecked")
public final class UrnCacheTest extends LimeTestCase {

    /**
     * File where urns (currently SHA1 urns) get persisted to
     */
    private static final String URN_CACHE_FILE = "fileurns.cache";
    private static final String FILE_PATH = "com/limegroup/gnutella/util";

    private static final Set EMPTY_SET = 
        Collections.unmodifiableSet(new HashSet());
    private UrnCache urnCache;
    
	/**
	 * Constructs a new UrnCacheTest with the specified name.
	 */
	public UrnCacheTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UrnCacheTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	@Override
	protected void setUp() throws Exception {
	    urnCache = new UrnCache(ExecutorsHelper.newProcessingQueue("test"), null);
	}
    
    /**
     * Test read & write of map
     */
    public void testPersistence() throws Exception {
        assertTrue("cache should not be present", !cacheExists() );
        
        Collection<FileAndUrns> faus = createLotsOfUrns();
        assertNotNull("should have some file descs", faus);
        assertGreaterThan("should have some file descs", 0, faus.size());
        assertTrue("cache should still not be present", !cacheExists() );
        urnCache.persistCache();
        assertTrue("cache should now exist", cacheExists());
        for(FileAndUrns fau : faus) {
            Set set = urnCache.getUrns(fau.file);
            assertTrue("file should be present in cache",
               !set.equals(EMPTY_SET)
            );
            assertTrue("URN set should not be empty", !set.isEmpty());
            assertEquals("URN set should be same as that in desc",
                fau.urns, set);
        }
    }
    
    public void testPersistsItself() throws Exception {
        Mockery context = new Mockery();
        final ServiceScheduler serviceScheduler = context.mock(ServiceScheduler.class);
        final AtomicReference<Runnable> runnable = new AtomicReference<Runnable>();
        context.checking(new Expectations() {{
            one(serviceScheduler).scheduleAtFixedRate(with(any(String.class)), 
                    with(any(Runnable.class)), with(equal(30L)), with(equal(30L)), 
                    with(equal(TimeUnit.SECONDS)), with(aNull(ScheduledExecutorService.class)));
            will(new AssignParameterAction<Runnable>(runnable, 1));
        }});
        
        urnCache.register(null, serviceScheduler);
        
        Collection<FileAndUrns> faus = createLotsOfUrns();
        assertNotNull("should have some file descs", faus);
        assertGreaterThan("should have some file descs", 0, faus.size());
        assertTrue("cache should still not be present", !cacheExists() );
                
        runnable.get().run();
        
        assertTrue("runnable should have written cache", cacheExists());
        
        context.assertIsSatisfied();
    }

	private Collection<FileAndUrns> createLotsOfUrns() throws Exception {
        File path = TestUtils.getResourceFile(FILE_PATH);
        File[] files = path.listFiles(new FileFilter() { 
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });
		List<FileAndUrns> faus = new ArrayList<FileAndUrns>();
		for(int i=0; i<files.length; i++) {
			Set<URN> urns = UrnHelper.calculateAndCacheURN(files[i], urnCache);
			faus.add(new FileAndUrns(files[i], urns));
		}				
		return faus;
	}
	
	private class FileAndUrns {
	    private final Set<URN> urns;
	    private final File file;
	    
	    public FileAndUrns(File file, Set<URN> urns) {
            this.urns = urns;
            this.file = file;
        }
	}

	/**
	 * Convenience method for making sure that the serialized file exists.
	 */
	private static boolean cacheExists() {
		File cacheFile = new File(_settingsDir, URN_CACHE_FILE);
		return cacheFile.exists();
	}

}

