package com.limegroup.gnutella.messages.vendor;

/**
 * Factory for inspection responses
 */
public interface InspectionResponseFactory {
    /**
     * @param request the message to respond to
     * @return one or more messages to send back.
     */
    public InspectionResponse[] createResponses(InspectionRequest request);
}
