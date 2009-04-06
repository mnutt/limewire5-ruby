package com.limegroup.gnutella.lws.server;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.http.httpclient.SimpleLimeHttpClient;
import org.limewire.lws.server.LWSDispatcher;
import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LocalServerImpl;
import org.limewire.lws.server.LocalServerDelegate;

/**
 * Send commands via the the {@link LimeHttpClient}.
 */
final class CommandSender {
        
    private static final Log LOG = LogFactory.getLog(CommandSender.class);
    
    private int port = -1;
    private final String HOST = "localhost";
        
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.lws.server.CommandSender#sendMessage(java.lang.String, java.util.Map)
     */
    public final String sendMessage(String command, Map<String, String> args) {
        String url = "http://" + HOST + ":" + getPort()  + "/"
                   + LWSDispatcher.PREFIX
                   + LocalServerDelegate.NormalStyleURLConstructor.INSTANCE.constructURL(command, args);
        HttpResponse response = null;
        LimeHttpClient client = new SimpleLimeHttpClient();
        try {
            HttpGet get = new HttpGet(url);
            //
            // This is going to block, but that should be OK. This code will never
            // run in the client and really should be running outside, because it
            // represents making a call in Javascript from a web page
            //
            response = client.execute(get);
            LOG.debug(response.getStatusLine());
            LOG.debug(Arrays.asList(response.getAllHeaders()));
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
                LOG.debug(result);
            } else {
                result = null;
            }
            return result;
        } catch (IOException e) {
            LOG.warn("exception when sending " + url, e);
        } finally {
            client.releaseConnection(response);
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.lws.server.CommandSender#detach()
     */
    public final void detach(String privateKey, String sharedKey) {
        Map<String,String> args = new HashMap<String,String>();
        args.put(LWSDispatcherSupport.Parameters.CALLBACK, "_dummy");
        args.put(LWSDispatcherSupport.Parameters.PRIVATE, privateKey);
        args.put(LWSDispatcherSupport.Parameters.SHARED, sharedKey);
        sendMessage(LWSDispatcherSupport.Commands.DETATCH, args);
    }
    
    private int getPort() {
        if (port != -1) {
            return port;
        }
        int tmpPort = LocalServerImpl.PORT;
        for (tmpPort = LocalServerImpl.PORT; tmpPort < LocalServerImpl.PORT + 10; tmpPort++) {
            try {
                Socket s = new Socket(HOST, tmpPort);
                s.getInputStream(); 
                s.getOutputStream();
                s.close();
            } catch (IOException e) {
                // ignore
            }
            break;

        }
        port = tmpPort;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Connected on port " + port);
        }
        return port;
    }        
}
