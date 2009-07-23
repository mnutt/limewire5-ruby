package org.limewire.http.webservice;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.cometd.Channel;
import org.cometd.Client;
import org.limewire.core.api.download.DownloadListManager;

public class CometDownloadPropertyListener implements PropertyChangeListener {

    private Channel channel;
    private Client client;

    public CometDownloadPropertyListener(Channel channel, Client client) {
        this.channel = channel;
        this.client = client;
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        System.out.println(event.getNewValue());
    }

}
