package com.limegroup.gnutella.library;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.limewire.util.TestUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.util.LimeTestCase;


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
	    urnCache = new UrnCache();
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

