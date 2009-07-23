package org.limewire.http.webservice;

import java.beans.PropertyChangeListener;
import java.util.HashMap;

import org.cometd.Channel;
import org.cometd.Client;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.impl.download.DownloadListener;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.downloader.DownloadStateEvent;

public class CometDownloadListener implements DownloadListener {
    
    private Channel channel;
    private Client client;
    private DownloadListManager downloadListManager;

    public CometDownloadListener(Channel channel, Client client, DownloadListManager downloadListManager) {
        this.channel = channel;
        this.client = client;
        this.downloadListManager = downloadListManager;
    }

    @Override
    public void downloadAdded(Downloader downloader) {
        HashMap<String, Object> response = new HashMap<String, Object>();
        response.put("sha1", downloader.getSha1Urn().toString().replaceFirst("urn:sha1:", ""));
        response.put("state", downloader.getState());
        channel.publish(this.client, response, downloader.getSha1Urn().toString().substring(8));
        EventListener<DownloadStateEvent> listener = new CometDownloadStateListener(this.channel, this.client, this.downloadListManager);
        downloader.addListener(listener);
        PropertyChangeListener propertyListener = new CometDownloadPropertyListener(this.channel, this.client);
        this.downloadListManager.addPropertyChangeListener(propertyListener);
    }

    @Override
    public void downloadRemoved(Downloader downloader) {
        HashMap<String, Object> response = new HashMap<String, Object>();
        response.put("sha1", downloader.getSha1Urn().toString().replaceFirst("urn:sha1:", ""));
        response.put("state", "removed");
        channel.publish(this.client, response, downloader.getSha1Urn().toString().replaceFirst("urn:sha1:", ""));
    }

    @Override
    public void downloadsCompleted() {
    }

}
