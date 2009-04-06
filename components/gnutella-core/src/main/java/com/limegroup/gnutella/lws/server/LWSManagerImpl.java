package com.limegroup.gnutella.lws.server;


import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.core.settings.LWSSettings;
import org.limewire.lws.server.AbstractReceivesCommandsFromDispatcher;
import org.limewire.lws.server.LWSConnectionListener;
import org.limewire.lws.server.LWSDispatcher;
import org.limewire.lws.server.LWSDispatcherFactory;
import org.limewire.lws.server.LWSDispatcherFactoryImpl;
import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSDispatcherSupport.Responses;
import org.limewire.lws.server.LWSReceivesCommandsFromDispatcher;
import org.limewire.lws.server.LWSSenderOfMessagesToServer;
import org.limewire.lws.server.StringCallback;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.util.EncodingUtils;
import com.limegroup.gnutella.util.LimeWireUtils;


/**
 * Encapsulates a {@link LWSDispatcher} and {@link LWSReceivesCommandsFromDispatcher}.
 */
@Singleton
public final class LWSManagerImpl implements LWSManager, LWSSenderOfMessagesToServer {
    
    private final static Log LOG = LogFactory.getLog(LWSManagerImpl.class);
    
    /** The page for making commands to The LimeWire Store server. */
    final static String COMMAND_PAGE_WITH_LEADING_AND_TRAILING_SLASHES 
        = "/store/app/pages/client/ClientCom/command/";
        
    private final LWSDispatcher dispatcher;
    private final Map<String, LWSManagerCommandResponseHandler> commands2handlers = new HashMap<String, LWSManagerCommandResponseHandler>();
    
    /** This is provided by {@link LWSSettings}. */
    private final String hostNameAndPort;
    
    private final HttpExecutor exe;
    private boolean isConnected;
    
    @Inject
    public LWSManagerImpl(HttpExecutor exe) {
        this(exe, new LWSDispatcherFactoryImpl()); //TODO: inject
    }    
    
    //todo @Inject
    public LWSManagerImpl(HttpExecutor exe, LWSDispatcherFactory lwsDispatcherFactory) {
        this(exe, LWSSettings.LWS_AUTHENTICATION_HOSTNAME.get(), 
             LWSSettings.LWS_AUTHENTICATION_PORT.getValue(), lwsDispatcherFactory);
    }
    
    public LWSManagerImpl(HttpExecutor exe, String host, int port, LWSDispatcherFactory lwsDispatcherFactory) {
        this.exe = exe;
        this.dispatcher = lwsDispatcherFactory.createDispatcher(this, new  AbstractReceivesCommandsFromDispatcher() {
            public String receiveCommand(String cmd, Map<String, String> args) {
                return LWSManagerImpl.this.dispatch(cmd, args);
            }            
        });
        
        // Construct the hostname and port to which we connect for authentication
        // from remote settings
        StringBuilder hostNameAndPortBuffer = new StringBuilder(host);
        if (port > 0) hostNameAndPortBuffer.append(":").append(port);
        this.hostNameAndPort = hostNameAndPortBuffer.toString();
        if (LOG.isDebugEnabled()) {
            LOG.debug("hostname and port: " + hostNameAndPort);
        }
        //
        // remember when we're connected
        //
        dispatcher.addConnectionListener(new LWSConnectionListener() {

            public void connectionChanged(boolean isConnected) {
                LWSManagerImpl.this.isConnected = isConnected;
            }
            
        });
        //
        // Add handler to check if we are still connected
        // 
        registerHandler("IsConnected", new LWSManagerCommandResponseHandlerWithCallback("IsConnected") {
            @Override
            protected String handleRest(Map<String, String> args) {
                //
                // If we're connected with the correct private key this will be
                // fine
                //
                return LWSDispatcherSupport.Responses.OK;
            } 
        });
    }
 
    public final boolean addConnectionListener(LWSConnectionListener lis) {
        return dispatcher.addConnectionListener(lis);
    }

    public final boolean removeConnectionListener(LWSConnectionListener lis) {
        return dispatcher.removeConnectionListener(lis);
    }
    
    
    // -----------------------------------------------------------------
    // Implementation of LWSManager
    // -----------------------------------------------------------------

    public final NHttpRequestHandler getHandler() {
        return dispatcher;
    }
    
    public boolean isConnected() {
        return isConnected;
    }
  
    public final boolean registerHandler(String cmd, LWSManagerCommandResponseHandler lis) {
        if (lis == null) throw new NullPointerException("Handlers can't be null");
        String hash = hash(cmd);
        commands2handlers.put(hash, lis);
        return true;
    }
    
    public final boolean unregisterHandler(String cmd) {
        String hash = hash(cmd);
        return commands2handlers.remove(hash) != null;
    }
 
    public final void clearHandlers() {
        commands2handlers.clear();
    }
    
    public final void sendMessageToServer(final String msg, 
                                          final Map<String, String> args, 
                                          final StringCallback cb) throws IOException {
        String url = constructURL(msg, args);
        if (LOG.isDebugEnabled()) {
            LOG.debug("sending URL " + url);
        }
        final HttpGet get = new HttpGet(url);
        
        if(get.getURI().getHost() == null) {
            LOG.error("null host!");
            throw new IOException("null host!");
        }
        
        get.addHeader("User-Agent", LimeWireUtils.getHttpServer());
        //
        // we don't care what this response is, because we are
        // always talking to the remote web server, so process it
        // right away so we don't block
        //
        //
        cb.process(Responses.OK);
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 1000);
        HttpConnectionParams.setSoTimeout(params, 1000);
        exe.execute(get, params);
        
    }
     
    // ---------------------------------------------------------------------------------
    // Private
    // ---------------------------------------------------------------------------------    
    
    private String constructURL(final String msg, final Map<String, String> args) {
        StringBuilder url = new StringBuilder("http");
        if (LWSSettings.LWS_USE_SSL.getValue()) {
            url.append("s");
        }
        url.append("://")
           .append(hostNameAndPort)
           .append(COMMAND_PAGE_WITH_LEADING_AND_TRAILING_SLASHES);
        url.append(msg);
        for (Map.Entry<String, String> e : args.entrySet()) {
            url.append("/");
            url.append(e.getKey()).append("/").append(EncodingUtils.encode(e.getValue()));
        }
        return url.toString();
    }    
  
    private String hash(String cmd) {
        return cmd.toLowerCase(Locale.US);
    }    

    private String dispatch(String command, Map<String, String> arguments) {
        String hash = hash(command);
        String result = null;

        LWSManagerCommandResponseHandler handler = commands2handlers.get(hash);
        if (handler != null) {
            result = handler.handle(arguments);

            if (result == null) {
				// does this ever happen? could just assert.
                result = "OK";
            }            
        }
        
        return result;
    } 
}
