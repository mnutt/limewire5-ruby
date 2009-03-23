package com.limegroup.gnutella.messages;

import java.io.IOException;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.GGEP;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.HUGEExtension.GGEPBlock;
import com.limegroup.gnutella.messages.QueryRequestTest.PositionByteArrayOutputStream;

public class HUGEExtensionTest extends BaseTestCase {

    public HUGEExtensionTest(String name) {
        super(name);
    }
    
    public void testMultipleGGEPParsing() throws IOException, BadGGEPBlockException {
        PositionByteArrayOutputStream out = new PositionByteArrayOutputStream();
        GGEP ggep = new GGEP(true);
        ggep.put("1", "1");
        ggep.write(out);
        out.write(0x1c);
        int pos2 = out.getPos();
        ggep.put("2", "2");
        ggep.write(out);
        int end2 = out.getPos();
        out.write(0x1c);
        // non ggep data inbetween:
        out.write("urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C".getBytes("UTF-8"));
        out.write(0x1c);
        int pos3 = out.getPos();
        ggep.put("3", "3");
        ggep.write(out);
        
        HUGEExtension huge = new HUGEExtension(out.toByteArray());
        assertNotNull(huge.getGGEP());
        // parsed and merged ggep should be like last one written
        assertEquals(ggep, huge.getGGEP());
        assertEquals(3, huge.getGGEPBlocks().size());
        assertEquals(ggep, huge.getGGEPBlocks().get(2).getGGEP());
        
        // check indices
        assertEquals(pos2 - 1, huge.getGGEPBlocks().get(0).getEndPos());
        assertEquals(pos2, huge.getGGEPBlocks().get(1).getStartPos());
        assertEquals(end2, huge.getGGEPBlocks().get(1).getEndPos());
        assertEquals(pos3, huge.getGGEPBlocks().get(2).getStartPos());
        
        // parse and compare
        for (GGEPBlock block : huge.getGGEPBlocks()) {
            int[] end = new int[1];
            GGEP parsed = new GGEP(out.toByteArray(), block.getStartPos(), end);
            assertEquals(parsed, block.getGGEP());
            assertEquals(end[0], block.getEndPos());
        }
        
        assertEquals(URN.createSHA1Urn("urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C"), 
                huge.getURNS().iterator().next());
    }

}
