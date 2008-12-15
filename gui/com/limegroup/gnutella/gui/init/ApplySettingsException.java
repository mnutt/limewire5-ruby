package com.limegroup.gnutella.gui.init;

/**
 * Signifies that an exception has occurred in applying a setting during
 * setup.
 */
public class ApplySettingsException extends Exception {

    /**
     * Constructs an ApplySettingsException with <code>null</code>
     * as its error detail message.  No dialog is shown if the message is <code>null</code>.
     */
    public ApplySettingsException() {
		super();
    }

    /**
     * Constructs an ApplySettingsException with the specified detail
     * message. The error message string <code>s</code> can later be
     * retrieved by the <code>{@link java.lang.Throwable#getMessage}</code>
     * method of class <code>java.lang.Throwable</code>.
     *
     * @param   s   the detail message.
     */
    public ApplySettingsException(String s) {
		super(s);
    }
}
