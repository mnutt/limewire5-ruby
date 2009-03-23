package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Random;

import junit.framework.Test;

import org.limewire.io.GGEP;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.MACCalculatorRepositoryManager;

import com.limegroup.gnutella.util.LimeTestCase;

public class MiscTests extends LimeTestCase {
    
    public MiscTests(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MiscTests.class);
    }
    
    // Makes sure QueryKeys have no problem going in and out of GGEP blocks
    public void testQueryKeysAndGGEP() throws Exception {
        MACCalculatorRepositoryManager macManager = new MACCalculatorRepositoryManager();
        Random rand = new Random();
        for (int i = 4; i < 17; i++) {
            byte[] qk = new byte[i];
            Arrays.sort(qk);
            // make sure the bytes have offensive characters....
            while ((Arrays.binarySearch(qk, (byte) 0x1c) < 0) ||
                   (Arrays.binarySearch(qk, (byte) 0x00) < 0)) {
                rand.nextBytes(qk);
                Arrays.sort(qk);
            }
            AddressSecurityToken addressSecurityToken = new AddressSecurityToken(qk,macManager);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            addressSecurityToken.write(baos);
            GGEP in = new GGEP(true);
            in.put(GGEPKeys.GGEP_HEADER_QUERY_KEY_SUPPORT,
                   baos.toByteArray());
            baos = new ByteArrayOutputStream();
            in.write(baos);
            GGEP out = new GGEP(baos.toByteArray(), 0, null);
            AddressSecurityToken queryKey2 = 
            new AddressSecurityToken(out.getBytes(GGEPKeys.GGEP_HEADER_QUERY_KEY_SUPPORT),macManager);
            assertEquals("qks not equal, i = " + i,
                       addressSecurityToken, queryKey2);
        }
    }

}
