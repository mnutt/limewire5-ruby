package com.limegroup.gnutella.util;

import junit.framework.AssertionFailedError;

/**
 * Note:
 *  This can be easily changed to become failures instead of errors
 *  by making it extend AssertionFailedError instead of Error, if that
 *  is desired,
 *  or vice versa by extending Error instead of AssertionFailedError
 *  ( That is the reason initCause(e) is used instead of using super(e) ).
 */
public class UnexpectedExceptionError extends AssertionFailedError {

	public UnexpectedExceptionError () {
	    super();
	}
	
	public UnexpectedExceptionError (String message) {
		super(message);
	}
	
	public UnexpectedExceptionError(Throwable e) {
	    super();
	    initCause(e);
	}
	
	public UnexpectedExceptionError(String message, Throwable e) {
	    super(message);
	    initCause(e);
	}
	    
}