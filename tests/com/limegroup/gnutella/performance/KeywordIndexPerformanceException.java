package com.limegroup.gnutella.performance;


/**
 * Exception used in this perf test
 */
public class KeywordIndexPerformanceException extends Exception {

    public KeywordIndexPerformanceException() {
        super();
    }

    public KeywordIndexPerformanceException(String msg) {
        super(msg);
    }

    public KeywordIndexPerformanceException(Throwable cause) {
        super(cause);
    }

    public KeywordIndexPerformanceException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
