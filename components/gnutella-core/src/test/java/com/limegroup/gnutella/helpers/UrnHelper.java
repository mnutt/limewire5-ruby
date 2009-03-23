package com.limegroup.gnutella.helpers;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.UrnCache;

public class UrnHelper {

    /**
     * Strings representing invalid URNs.
     */
    public static final String[] INVALID_URN_STRINGS = {
    	"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFBC",
    	"urn:sh1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"ur:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"rn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"urnsha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"urn:sha1PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	" urn:sHa1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"urn::sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"urn: sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"urn:sha1 :PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"urn:sha1 :PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"urn:sha1: PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWU GYQYPFB",
    	"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWU GYQYPFB ",
    	null,
    	"",
    	"test"
    };
    
    /**
     * String representing valid URNs.
     */
    public static final String[] VALID_URN_STRINGS = {
    	"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"urn:sha1:ZLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
    	"Urn:sha1:ALSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
    	"uRn:sHa1:QLRTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"urn:sha1:WLPTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"urn:Sha1:ELSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"UrN:sha1:RLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"urn:sHa1:ILSTIIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"urn:sha1:FLSTXIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPFB",
    	"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPZB",
    	"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPZC",
    	"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPZD",
    	"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPZE",
    	"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPRC",
    	"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPSD",
    	"urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPTE",
    };
    

    /**
     * Array of URNs for use by tests.
     */
    public static final URN[] URNS = new URN[VALID_URN_STRINGS.length];
    
    /**
     * Array of URNType instances for use by tests.
     */
    public static final URN.Type[] URN_TYPES = new URN.Type[VALID_URN_STRINGS.length];
    
    @SuppressWarnings("unchecked")
    public static final Set<URN>[] URN_SETS = new Set[VALID_URN_STRINGS.length];


    /**
     * A "unique" SHA1 for convenience.
     */
    public static final URN UNIQUE_SHA1;

    public static final URN SHA1;
    
    public static final URN TTROOT;
    
    static {        
        try {
            UNIQUE_SHA1 = URN.createSHA1Urn("urn:sha1:PLSTHIFQGSJZT45FJUPAKUZWUGYQYPFB");
            SHA1 = URN.createSHA1Urn(UrnHelper.VALID_URN_STRINGS[3]);
            TTROOT= URN.createTTRootUrn("urn:ttroot:PLSTHIFQGSJZT45FJUPAKUZWUGYQYPFBAAAAAAA");
        
            for(int i=0; i<UrnHelper.VALID_URN_STRINGS.length; i++) {
                URN urn = URN.createSHA1Urn(UrnHelper.VALID_URN_STRINGS[i]);
                UrnHelper.URNS[i] = urn;
                UrnHelper.URN_TYPES[i] = urn.getUrnType();
                Set<URN> urnSet = new HashSet<URN>();
                urnSet.add(urn);
                UrnHelper.URN_SETS[i] = urnSet;
            }
        } catch(IOException iox) {
            throw new RuntimeException(iox);
        }
    }

    public static Set<URN> calculateAndCacheURN(File f, UrnCache urnCache) throws Exception {
        return urnCache.calculateAndCacheUrns(f).get();
    }

}
