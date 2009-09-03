package org.limewire.core.impl.inspections;

/**
 * Exception thrown when there is an error while
 * processing the inspection result (such as sending data to server)
 */
public class InspectionProcessingException extends Exception {

    public InspectionProcessingException() {
        super();
    }

    public InspectionProcessingException(String msg) {
        super(msg);
    }

    public InspectionProcessingException(Throwable cause) {
        super(cause);
    }

    public InspectionProcessingException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
