package org.limewire.http.webservice;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.cometd.Channel;
import org.cometd.Client;

public class CometDownloadPropertyListener implements PropertyChangeListener {


    public CometDownloadPropertyListener(Channel channel, Client client) {
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        System.out.println(event.getNewValue());
    }

}
