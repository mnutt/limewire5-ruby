package org.limewire.promotion.containers;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.GGEP;

/**
 * The most basic block of information conveyed by the promotion system, a block
 * of bytes encoded with GGEP. Each entry should have a key "T" (type) that
 * determines how to further interpret the GGEP data after parsing.
 */
public interface MessageContainer {
    String TYPE_KEY = "T";

    /**
     * @return the type code for this container
     */
    byte[] getType();

    /**
     * @return The full encoded version of this container (A raw GGEP,
     *         generally)
     */
    byte[] encode();

    /**
     * Take the given passed in GGEP-encoded bytes and parse out the data to
     * fill yourself in.
     * 
     * @throws BadGGEPBlockException If the passed-in value doesn't represent
     *         an instance of this object type, or a wrapped MessageContainer
     *         throws this exception during its own parse method.
     */
    void decode(GGEP rawGGEP) throws BadGGEPBlockException;

}
