package org.limewire.lws.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;



/**
 * Creates a {@link LWSDispatcher} and is the main entry point to this component.
 */
@Singleton
public class LWSDispatcherFactoryImpl implements LWSDispatcherFactory {
      
    @Inject
    public LWSDispatcherFactoryImpl() {

    }

    public LWSDispatcher createDispatcher(LWSSenderOfMessagesToServer sender, LWSReceivesCommandsFromDispatcher recipient) {
        LWSDispatcherImpl s = new LWSDispatcherImpl(sender);
        s.setCommandReceiver(recipient);
        return s;
    }

}
