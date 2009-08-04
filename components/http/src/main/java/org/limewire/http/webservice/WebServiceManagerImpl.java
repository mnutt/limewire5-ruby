package org.limewire.http.webservice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.jruby.rack.RackServletContextListener;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.NetworkUtils;
import org.limewire.core.impl.download.CoreDownloadListManager;
import org.limewire.core.impl.download.DownloadListenerList;
import org.mortbay.cometd.AbstractBayeux;
import org.mortbay.cometd.continuation.ContinuationCometdServlet;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;
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

            String limeRemotePath = findLimeRemotePath();
            if(limeRemotePath != null) {
                loadWebService(limeRemotePath);
                setStatus("started");
            } else {
                System.out.println("Could not find lime remote path");
            }
        }
    }
    
    private String findLimeRemotePath() {
        String usablePath = null;
        String[] loadPaths = {
                "../../../../..",
                "./remote"
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
    
    public void loadWebService(String limeRemotePath) {
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
        context.setResourceBase(limeRemotePath);
        System.out.println("limeRemotePath: " + limeRemotePath);
        context.addEventListener(new RackServletContextListener());
        
        ContinuationCometdServlet cometdServlet = new ContinuationCometdServlet();
        ServletHolder cometdServletHolder = setupCometdServletHolder(cometdServlet);
        context.addServlet(cometdServletHolder, "/comet/*");
        
        PartialDownloadStreamServlet partialDownloadStreamServlet = new PartialDownloadStreamServlet(this.downloadManager);
        ServletHolder streamServletHolder = new ServletHolder(partialDownloadStreamServlet);
        context.addServlet(streamServletHolder, "/stream/*");
        
        String rackup = "";
        
        try {
            String path = "./remote/app.rb";
            BufferedReader buff =  new BufferedReader(new FileReader(path));
            String s;
            while((s = buff.readLine()) != null) {
                rackup += s + "\n";
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        
        HashMap<String, String> options = new HashMap<String, String>();
        //options.put("rails.root", ".");
        options.put("rackup", rackup);
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
