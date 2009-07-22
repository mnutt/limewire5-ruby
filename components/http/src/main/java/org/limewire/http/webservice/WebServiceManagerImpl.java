package org.limewire.http.webservice;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.cometd.Bayeux;
import org.cometd.Channel;
import org.cometd.Client;
import org.cometd.Message;
import org.jruby.rack.rails.RailsServletContextListener;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.core.impl.search.SearchManagerImpl.Details;
import org.limewire.core.impl.search.SearchManagerImpl.SearchWithResults;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.GUID;
import org.limewire.io.NetworkUtils;
import org.limewire.listener.EventListener;
import org.mortbay.cometd.AbstractBayeux;
import org.mortbay.cometd.BayeuxService;
import org.mortbay.cometd.continuation.ContinuationCometdServlet;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.log.Log;
import org.mortbay.thread.QueuedThreadPool;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.UPnPManager;

@Singleton
public 
class WebServiceManagerImpl implements WebServiceManager {

    private final Provider<UPnPManager> upnpManager;
    private NetworkManager networkManager;
            
    private int _port = 4422;
    private String status = "stopped";
    private Injector injector;
    private SearchManager searchManager;
    
    @Inject
    public WebServiceManagerImpl(Provider<UPnPManager> upnpManager, 
            NetworkManager networkManager, Injector injector, SearchManager searchManager) {
        this.upnpManager = upnpManager;
        this.networkManager = networkManager;
        this.injector = injector;
        this.searchManager = searchManager;
    }
    
    @Override
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("WebService Manager");
    }
    
    @Override
    public void start() {
        if(!this.isServerRunning()) {
            System.out.println("Starting mongrel...");
            setStatus("starting");

            // Try to port forward incoming traffic to our server via UPnP
            mapPort();

            String railsRoot = findRailsPath();
            if(railsRoot != null) {
                loadWebService(railsRoot);
                setStatus("started");
            } else {
                System.out.println("Could not find rails root");
            }
        }
    }
    
    private String findRailsPath() {
        String usablePath = null;
        String[] loadPaths = {
                "../../../../..",
                "./rails"
        };

        // Look through the paths to find one 
        for(String path : loadPaths) {
            File file = new File(path);
            if(file.exists()) {
                usablePath = path;
            }
        };
        return usablePath;
    }
    
    public void loadWebService(String railsRoot) {
        System.out.println("Jetty starting.");

        Server server = new Server(4422);
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMinThreads(1);
        threadPool.setMaxThreads(10);
        server.setThreadPool(threadPool);
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(4422);
        
        Context context = new Context(null, "/", Context.NO_SESSIONS);
        context.addFilter("org.jruby.rack.RackFilter", "/*", Handler.DEFAULT);
        context.setResourceBase(railsRoot);
        context.addEventListener(new RailsServletContextListener());
        
        ContinuationCometdServlet cometdServlet = new ContinuationCometdServlet();
        ServletHolder cometdServletHolder = setupCometdServletHolder(cometdServlet);
        context.addServlet(cometdServletHolder, "/comet/*");
        
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("rails.root", ".");
        options.put("public.root", "public");
        options.put("environment", "development");
        options.put("org.mortbay.jetty.servlet.Default.relativeResourceBase", "/public");
        options.put("jruby.max.runtimes", "1");
        
        context.setAttribute("injector", this.injector);
        context.setInitParams(options);
        context.addServlet(new ServletHolder(new DefaultServlet()), "/");
        
        server.setHandler(context);
        try {
            server.start();
            //server.join();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        AbstractBayeux bayeux = cometdServlet.getBayeux();
        new Monitor(bayeux);
        new CometSearchService(bayeux, this.searchManager);
    }
    
    public static class Monitor extends BayeuxService
    {
        public Monitor(Bayeux bayeux)
        {
            super(bayeux,"monitor");
            subscribe("/meta/subscribe","monitorSubscribe");
            subscribe("/meta/unsubscribe","monitorUnsubscribe");
            subscribe("/meta/*","monitorMeta");
            // subscribe("/**","monitorVerbose");
        }
        
        public void monitorSubscribe(Client client, Message message)
        {
            Log.info("Subscribe from "+client+" for "+message.get(Bayeux.SUBSCRIPTION_FIELD));
        }
        
        public void monitorUnsubscribe(Client client, Message message)
        {
            Log.info("Unsubscribe from "+client+" for "+message.get(Bayeux.SUBSCRIPTION_FIELD));
        }
        
        public void monitorMeta(Client client, Message message)
        {
            if (Log.isDebugEnabled())
                Log.debug(message.toString());
        }
        
        /*
        public void monitorVerbose(Client client, Message message)
        {
            System.err.println(message);
            try 
            {
                Thread.sleep(5000);
            }
            catch(Exception e)
            {
                Log.warn(e);
            }
        }
        */
    }
    
    public static class CometSearchService extends BayeuxService {
        private SearchManager searchManager;
        
        public CometSearchService(Bayeux bayeux, SearchManager searchManager) {
            super(bayeux, "search");
            System.out.println("Setting up search...");
            subscribe("/search", "searchResults");
            this.searchManager = searchManager;
        }
        
        public Object searchResults(Client client, Object data) {
            System.out.println("ECHO from "+client+" "+data);

            try {
                GUID guid = new GUID(data.toString());
                SearchListener listener = new CometSearchListener(getBayeux().getChannel("/search", false), getClient());
                SearchWithResults search = this.searchManager.getSearchByGuid(guid);
                search.getSearch().addSearchListener(listener);
                return "getting results for " + data;
            } catch(Exception e) {
                Log.warn(e);
                return "bad results for " + data;
            }

        }
    }
    
    private static ServletHolder setupCometdServletHolder(ContinuationCometdServlet cometdServlet) {
        ServletHolder cometdHolder = new ServletHolder(cometdServlet);
        cometdHolder.setInitParameter("timeout", "120000");
        cometdHolder.setInitParameter("interval", "0");
        cometdHolder.setInitParameter("maxInterval", "10000");
        cometdHolder.setInitParameter("multiFrameInterval", "2000");
        cometdHolder.setInitParameter("difectDeliver", "true");
        cometdHolder.setInitParameter("logLevel", "10");
        return cometdHolder;
    }
    
    public void mapPort() {
        boolean natted = upnpManager.get().isNATPresent();
        boolean validPort = NetworkUtils.isValidPort(_port);
        boolean forcedIP = ConnectionSettings.FORCE_IP_ADDRESS.getValue() &&
            !ConnectionSettings.UPNP_IN_USE.getValue();
                
        if(natted && validPort && !forcedIP) {
            byte[] externalAddress = networkManager.getExternalAddress();
            System.out.println(_port);
            System.out.println(externalAddress);
            int usedPort = upnpManager.get().mapPort(_port, externalAddress);
            System.out.println(usedPort);
        } 
    }

    public void stop() {
        if(this.isServerRunning()) {
            this.setStatus("stopping");
        }
    }
    
    public void restart() {
        if(this.isServerRunning()) {
            this.stop();
        }
        this.start();
    }
    
    public Boolean isServerRunning() {
        return this.getStatus() == "started" || this.getStatus() == "starting";
    }
    
    public synchronized String getStatus() {
        return this.status;
    }
    
    public synchronized void setStatus(String status) {
        System.out.println("setting status to " + status);
        this.status = status;
    }
    
    @Override
    public boolean isAsyncStop() {
        return true;
    }
}
