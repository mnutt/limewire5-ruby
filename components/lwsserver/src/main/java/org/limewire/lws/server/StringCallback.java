/**
 * 
 */
package org.limewire.lws.server;

/**
 * Defines an interface for objects that want to receive a {@link String}
 * result. This is useful for non-blocking situations, where instead of
 * returning a {@link String} result you pass it back in a tail call to one of
 * these.
 * <p>
 * Implementations of this interface are used to make
 * {@link LWSSenderOfMessagesToServer#sendMessageToServer(String, java.util.Map, StringCallback)}
 * nonblocking by passing the result to the last argument.
 */
public interface StringCallback {

    /**
     * A method thats to be non-blockingwould take a {@link StringCallback} as
     * an argument and pass the result into this method instead of returning.
     * 
     * @param response the result of some computation
     */
    void process(String response);
}