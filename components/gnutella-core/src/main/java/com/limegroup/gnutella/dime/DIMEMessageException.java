package com.limegroup.gnutella.dime;

import java.io.IOException;

/**
 * @author Gregorio Roper
 * 
 * Custom exception for DIMEMessage & DIMERecord
 */
public class DIMEMessageException extends IOException {

    /**
     * Constructs standard DIMEMessageException
     */
    public DIMEMessageException() {
        super("Could not create DIMEMessage");
    }

    /**
     * Constructs DIMEMessageException
     * 
     * @param arg0
     *            the <tt>String</tt> to pass on to super
     */
    public DIMEMessageException(String arg0) {
        super(arg0);
    }

}
