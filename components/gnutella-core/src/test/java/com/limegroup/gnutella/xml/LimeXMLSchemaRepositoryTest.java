package com.limegroup.gnutella.xml;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.Expand;
import org.limewire.util.CommonUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;


/**
 * Unit tests for LimeXMLSchemaRepository
 */
public class LimeXMLSchemaRepositoryTest extends LimeTestCase {
            
	private LimeXMLSchemaRepository limeXMLSchemaRepository;

    public LimeXMLSchemaRepositoryTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(LimeXMLSchemaRepositoryTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	@Override
    public void setUp() throws Exception {
	    Expand.expandFile(
            TestUtils.getResourceFile("com/limegroup/gnutella/xml/xml.war"), 
            CommonUtils.getUserSettingsDir()
        );

		Injector injector = LimeTestUtils.createInjector();
		limeXMLSchemaRepository = injector.getInstance(LimeXMLSchemaRepository.class);
    }
    
    /**
     * Tests that the getAvailableSchemaURIs function correctly returns
     * all availables URIs.
     *
     * Note that this test is stricter than necessary, relying on the order.
     * If any new xsd files are are added (or the URIs within the existing
     * ones change) then change the order of the checks.
     */
    public void testAvailableSchemaURIs() {
        String[] availableSchemas = limeXMLSchemaRepository.getAvailableSchemaURIs();
            
        check( availableSchemas[0], "application");
        check( availableSchemas[1], "audio");
        check( availableSchemas[2], "document");
        check( availableSchemas[3], "image");
        check( availableSchemas[4], "torrent");
        check( availableSchemas[5], "video");
    }
    
    private static void check(String actual, String name) {
        String expected = "http://www.limewire.com/schemas/" + name + ".xsd";
        assertEquals(expected, actual);
    }        
}
