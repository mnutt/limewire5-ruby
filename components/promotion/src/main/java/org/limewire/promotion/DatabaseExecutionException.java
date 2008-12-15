package org.limewire.promotion;

/**
 * Thrown when the execution of a database operation fails. This is thrown from
 * various operations in class {@link SearcherDatabase}.
 */
public class DatabaseExecutionException extends Exception {

    public DatabaseExecutionException() {
        super();
    }

    public DatabaseExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseExecutionException(String message) {
        super(message);
    }

    public DatabaseExecutionException(Throwable cause) {
        super(cause);
    }
    
}
