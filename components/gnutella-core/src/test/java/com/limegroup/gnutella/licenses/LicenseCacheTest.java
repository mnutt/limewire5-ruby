package com.limegroup.gnutella.licenses;

import java.io.File;
import java.net.URI;

import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.util.LimeTestCase;

public class LicenseCacheTest extends LimeTestCase {

    public LicenseCacheTest(String name) {
        super(name);
    }

    public void testConstructionNoFile() throws Exception {
        LicenseCache licenseCache = new LicenseCache();
        // this will throw a an NPE if the fields have not been initialized correctly
        licenseCache.getData("random");
        licenseCache.getLicense("foo", new URI("http://foo.bar/"));
    }
    
    public void testConstructionExistingEmptyFile() throws Exception {
        File file = new File(CommonUtils.getUserSettingsDir(), "licenses.cache");
        assertTrue(file.createNewFile());
        
        LicenseCache licenseCache = new LicenseCache();
        // this will throw a an NPE if the fields have not been initialized correctly
        licenseCache.getData("random");
        licenseCache.getLicense("foo", new URI("http://foo.bar/"));
    }

}
