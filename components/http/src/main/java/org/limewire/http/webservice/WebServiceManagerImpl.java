package org.limewire.http.webservice;

import java.io.File;
import java.util.HashMap;

import org.jruby.rack.rails.RailsServletContextListener;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.NetworkUtils;
import org.limewire.core.impl.download.CoreDownloadListManager;
import org.limewire.core.impl.download.DownloadListenerList;
import org.mortbay.cometd.AbstractBayeux;
import org.mortbay.cometd.continuation.ContinuationCometdServlet;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.servlet.ProxyServlet;
import org.mortbay.thread.QueuedThreadPool;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.UPnPManager;

@EagerSingleton
public 
class WebServiceManagerImpl implements WebServiceManager {

    private final Provider<UPnPManager> upnpManager;
    private NetworkManager networkManager;
            
    private int _port = 4422;
    private String status = "stopped";
    private Injector injector;
    private SearchManager searchManager;
    private CoreDownloadListManager downloadManager;
    private DownloadListenerList downloadListenerList;
    private LibraryManager libraryManager;
    
    @Inject
    public WebServiceManagerImpl(Provider<UPnPManager> upnpManager, 
            NetworkManager networkManager, Injector injector, SearchManager searchManager, 
            CoreDownloadListManager downloadManager, DownloadListenerList downloadListenerList,
            LibraryManager libraryManager) {
        this.upnpManager = upnpManager;
        this.networkManager = networkManager;
        this.injector = injector;
        this.searchManager = searchManager;
        this.downloadManager = downloadManager;
        this.downloadListenerList = downloadListenerList;
        this.libraryManager = libraryManager;
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
        
        ProxyServlet.Transparent proxyServlet = new ProxyServlet.Transparent("", "ayce-test.ath.cx", 80);
        ServletHolder proxyServletHolder = setupProxyServletHolder(proxyServlet);
        context.addServlet(proxyServletHolder, "/ayce/*");
        
        ContinuationCometdServlet cometdServlet = new ContinuationCometdServlet();
        ServletHolder cometdServletHolder = setupCometdServletHolder(cometdServlet);
        context.addServlet(cometdServletHolder, "/comet/*");
        
        PartialDownloadStreamServlet partialDownloadStreamServlet = new PartialDownloadStreamServlet(this.downloadManager, this.searchManager, this.libraryManager);
        ServletHolder streamServletHolder = new ServletHolder(partialDownloadStreamServlet);
        context.addServlet(streamServletHolder, "/stream/*");
        
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
        new CometMonitorService(bayeux);
        new CometSearchService(bayeux, this.searchManager, this.libraryManager);
        new CometDownloadService(bayeux, this.downloadManager, this.downloadListenerList);
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
    
    private static ServletHolder setupProxyServletHolder(ProxyServlet proxyServlet) {
        ServletHolder proxyHolder = new ServletHolder(proxyServlet);
        proxyHolder.setInitParameter("timeout", "120000");
        proxyHolder.setInitParameter("interval", "0");
        proxyHolder.setInitParameter("maxInterval", "10000");
        proxyHolder.setInitParameter("multiFrameInterval", "2000");
        proxyHolder.setInitParameter("difectDeliver", "true");
        proxyHolder.setInitParameter("logLevel", "10");
        return proxyHolder;
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
