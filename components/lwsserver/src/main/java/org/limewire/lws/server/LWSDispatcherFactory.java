package org.limewire.lws.server;

/**
 * Creates a {@link LWSDispatcher} and is the main entry point to this component.
 */
public interface LWSDispatcherFactory {

    /**
     * Returns a new {@link LWSDispatcher}. A {@link LWSDispatcher} has to know how
     * to send a message to a remote server -- i.e. a
     * {@link LWSSenderOfMessagesToServer} -- and to whom commands should go after
     * authentication -- i.e. a {@link LWSReceivesCommandsFromDispatcher}.
     *
     * @return a new {@link LWSDispatcher}
     */ 
    public LWSDispatcher createDispatcher(LWSSenderOfMessagesToServer sender, LWSReceivesCommandsFromDispatcher recipient);

}
