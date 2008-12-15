package com.limegroup.gnutella.helpers;

import java.io.IOException;

import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;

public final class AlternateLocationHelper {

    public final String[] SOME_IPS = new String[] { "1.2.3.4", "1.2.3.5", "1.2.3.6", "1.2.3.7",
            "1.2.3.8", "1.2.3.9", "1.2.3.10", "1.2.3.11", "1.2.3.12", "1.2.3.13", "1.2.3.14",
            "1.2.3.15", };

    /**
     * Array of unequal alternate locations for testing convenience.
     */
    public final AlternateLocation[] UNEQUAL_SHA1_LOCATIONS = new AlternateLocation[SOME_IPS.length];

    /**
     * Array of alternate locations with equal hashes but unequal host names 
     * for testing convenience.
     */
    public final AlternateLocation[] EQUAL_SHA1_LOCATIONS = new AlternateLocation[SOME_IPS.length];

    public AlternateLocationHelper(AlternateLocationFactory alternateLocationFactory) {
        try {
            for (int i = 0; i < UNEQUAL_SHA1_LOCATIONS.length; i++) {
                UNEQUAL_SHA1_LOCATIONS[i] = alternateLocationFactory.create(SOME_IPS[i],
                        UrnHelper.URNS[i]);
            }

            for (int i = 0; i < EQUAL_SHA1_LOCATIONS.length; i++) {
                EQUAL_SHA1_LOCATIONS[i] = alternateLocationFactory.create(SOME_IPS[i],
                        UrnHelper.URNS[0]);
            }
        } catch (IOException iox) {
            throw new RuntimeException(iox);
        }
    }
}
