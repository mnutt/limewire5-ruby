package org.limewire.http.webservice;

import java.io.File;
import java.util.HashMap;

import org.jruby.rack.rails.RailsServletContextListener;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.NetworkUtils;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
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
    
    @Inject
    public WebServiceManagerImpl(Provider<UPnPManager> upnpManager, 
            NetworkManager networkManager, Injector injector) {
        this.upnpManager = upnpManager;
        this.networkManager = networkManager;
        this.injector = injector;
        
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
