package org.limewire.http.webservice;

import java.util.HashMap;

import org.cometd.Channel;
import org.cometd.Client;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.downloader.DownloadStateEvent;

public class CometDownloadStateListener implements EventListener<DownloadStateEvent> {

    private Channel channel;
    private Client client;
    private DownloadListManager downloadListManager;

    public CometDownloadStateListener(Channel channel, Client client, DownloadListManager downloadListManager) {
        this.channel = channel;
        this.client = client;
        this.downloadListManager = downloadListManager;
    }

    @Override
    public void handleEvent(DownloadStateEvent event) {
        DownloadItem item = downloadListManager.getDownloadItem(event.getSource().getSha1Urn());
        HashMap<String, Object> response = new HashMap<String, Object>();
        response.put("sha1", item.getUrn().toString().replaceFirst("urn:sha1:", ""));
        response.put("title", item.getTitle());
        response.put("state", event.getType().toString());
        response.put("complete", item.getCurrentSize());
        response.put("percent_complete", item.getPercentComplete());
        response.put("download_speed", item.getDownloadSpeed());
        response.put("sources", item.getSources());
        response.put("remaining_time", item.getRemainingDownloadTime());
        response.put("file_name", item.getFileName());
        response.put("total_size", item.getTotalSize());
        System.out.println("Updating " + item.getFileName() + "(" + item.getUrn().toString().replaceFirst("urn:sha1:", "") + ")");
        channel.publish(this.client, response, item.getUrn().toString().replaceFirst("urn:sha1:", ""));
    }

}
