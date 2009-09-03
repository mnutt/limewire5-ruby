package org.limewire.core.impl.inspections;


import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.limewire.gnutella.tests.LimeTestCase;

/**
 * Test {@link InspectionsSpec} encoding/decoding, wire format to/from object.
 */
public class InspectionsSpecTest extends LimeTestCase {
    

    public InspectionsSpecTest(String name) {
        super(name);
    }
    
    public void testInvalidWireFormatToInspectionsSpecFailure() throws Exception {
        byte[] garbage = "sdjklfhsdkfsdkjfsdgsdjgldfjgkldfjgldfjgdfkgdfkgldjfgdfjgkdfjg".getBytes();

        InspectionsParser parser = new InspectionsParser();
        
        try {
            parser.parseInspectionSpecs(garbage);
            fail("Expected IOException");
        } catch (IOException e) {
            assertEquals("Not in GZIP format", e.getMessage());       
        }
    }
    
    public void testEmptyWireFormatToInspectionsSpecFailure() throws Exception {
        byte[] garbage = new byte[0];
        InspectionsParser parser = new InspectionsParser();
        
        try {
            parser.parseInspectionSpecs(garbage);
            fail("Expected IOException");
        } catch (IOException e) {
            assertInstanceof(EOFException.class, e);       
        }
    }

    public void testWireFormatToInspectionsSpecSuccess() throws Exception {
        InspectionsSpec spec = new InspectionsSpec(Arrays.asList("one", "two", "three"), 5, 3);
        InspectionsSpec spec2 = new InspectionsSpec(Arrays.asList("four", "five", "six"), 6, 4);
        byte[] encoded = InspectionsTestUtils.getGzippedAndBencoded(Arrays.asList(spec, spec2));
        
        InspectionsParser parser = new InspectionsParser();
        List<InspectionsSpec> parsedSpecs = parser.parseInspectionSpecs(encoded);
            
        assertEquals(spec.getInitialDelay(), parsedSpecs.get(0).getInitialDelay());
        assertEquals(spec2.getInitialDelay(), parsedSpecs.get(1).getInitialDelay());
        assertEquals(spec.getInterval(), parsedSpecs.get(0).getInterval());
        assertEquals(spec2.getInterval(), parsedSpecs.get(1).getInterval());
        assertEquals(spec.getInspectionPoints(), parsedSpecs.get(0).getInspectionPoints());
        assertEquals(spec2.getInspectionPoints(), parsedSpecs.get(1).getInspectionPoints());
    }
}
