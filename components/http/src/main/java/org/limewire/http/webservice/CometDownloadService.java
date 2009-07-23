package org.limewire.http.webservice;

import java.io.IOException;
import java.util.HashMap;

import org.cometd.Bayeux;
import org.cometd.Client;
import org.cometd.Message;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.impl.download.CoreDownloadListManager;
import org.limewire.core.impl.download.DownloadListener;
import org.limewire.core.impl.download.DownloadListenerList;
import org.mortbay.cometd.BayeuxService;

import com.limegroup.gnutella.URN;

public class CometDownloadService extends BayeuxService {
    
    private CoreDownloadListManager downloadListManager;

    public CometDownloadService(Bayeux bayeux, CoreDownloadListManager downloadListManager, DownloadListenerList downloadListenerList) {
        super(bayeux, "download");
        System.out.println("Setting up download...");
        subscribe("/download", "sendDownload");
        DownloadListener listener = new CometDownloadListener(getBayeux().getChannel("/download", false), 
                                                              getClient(), 
                                                              downloadListManager);
        downloadListenerList.addDownloadListener(listener);
        this.downloadListManager = downloadListManager;
    }
    
    public Object sendDownload(Client client, Message message) {
        if(message.getData() != null) {
          try {
              DownloadItem item = this.downloadListManager.getDownloadItem(URN.createSHA1Urn("urn:sha1:" + message.getData().toString()));
              HashMap<String, Object> response = new HashMap<String, Object>();
              response.put("sha1", item.getUrn().toString().replaceFirst("urn:sha1:", ""));
              response.put("title", item.getTitle());
              response.put("state", item.getState().toString());
              response.put("complete", item.getCurrentSize());
              response.put("percent_complete", item.getPercentComplete());
              response.put("download_speed", item.getDownloadSpeed());
              response.put("sources", item.getSources());
              response.put("remaining_time", item.getRemainingDownloadTime());
              response.put("file_name", item.getFileName());
              response.put("total_size", item.getTotalSize());
              return response;
          } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
              return "";
          }
          
        } else {
            return "";
        }
    }
}