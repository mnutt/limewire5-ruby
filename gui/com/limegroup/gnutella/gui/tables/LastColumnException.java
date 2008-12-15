package com.limegroup.gnutella.gui.tables;

/**
 * Error because user attempted to remove the last column
 */
public final class LastColumnException extends Exception {

    /**
     * dull constructor
     */
    public LastColumnException() { super(); }

    /**
     * less dull constructor
     *
     * @param s the message
     */
    public LastColumnException(String s) { super(s); }
}
