package org.limewire.lws.server;

import java.io.IOException;
import java.util.Map;

/**
 * Defines an interface to communicate with a remote server. The location is
 * inedpendent of the implementation of the method
 * {@link #sendMessageToServer(String, Map)}. The reason that this method takes
 * a {@link String} and {@link Map<String,String>} instead of a single
 * {@link String} is because different remote servers take arguments
 * differently. For example a normal CGI implemention would encode arguments
 * with a <code>?</code> and series of <code>&amp;</code>s; where as <a
 * href="http://wicket.apache.org/">Wicket</a> uses just <code>/</code>s.
 * <p>
 * This is used in this component to construct a {@link LWSDispatcherImpl},
 * because after receiving a message to start communication it needs to send two
 * keys to a remote server.
 */
public interface LWSSenderOfMessagesToServer {

    /**
     * Sends the response after sending a message <code>msg</code> and
     * name/value pair arguments <code>args</code> to <code>callback</code>.
     * 
     * @param msg the command upon which to act
     * @param args name/value pair of arguments to send
     * @param callback put the result of sending this message to
     *        <code>callback</code>'s {@link StringCallback#process(String)}
     *        method, so this is non-blocking
     * @throws IOException when something happens to the connection or another
     *         IO program occurs
     */
    void sendMessageToServer(String msg, Map<String, String> args, StringCallback callback)
            throws IOException;
}
