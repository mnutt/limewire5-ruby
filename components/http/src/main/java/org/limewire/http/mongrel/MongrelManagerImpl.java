package org.limewire.http.mongrel;

import java.io.File;
import java.io.FileNotFoundException;

import javax.script.ScriptException;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.NetworkUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.UPnPManager;
import com.limegroup.scripting.RubyEvaluator;

public class MongrelManagerImpl implements MongrelManager {

    private RubyEvaluator rubyEvaluator;
    private final Provider<UPnPManager> upnpManager;
    private NetworkManager networkManager;
    
    private MongrelThread serverThread = null;
        
    private int _port = 4422;
    private String status = "stopped";
    
    @Inject
    public MongrelManagerImpl(final RubyEvaluator rubyEvaluator, Provider<UPnPManager> upnpManager, 
            NetworkManager networkManager) {
        this.rubyEvaluator = rubyEvaluator;
        this.upnpManager = upnpManager;
        this.networkManager = networkManager;
        
        this.serverThread = new MongrelThread(this);
    }
    
    @Override
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Mongrel Manager");
    }
    
    @Override
    public void start() {
        if(!this.isServerRunning()) {
            this.serverThread.start();
            setStatus("started");
        }
    }
    
    public void loadMongrel() {
        try {
            String usablePath = null;
            String[] loadPaths = {
                    "../../../../../script/start_rails",
                    "rails/script/start_rails"
            };

            // Look through the paths to find one 
            for(String path : loadPaths) {
                File file = new File(path);
                if(file.exists()) {
                    usablePath = path;
                }
            };
            if(usablePath != null) {
                rubyEvaluator.eval(usablePath);
            } else {
                throw new FileNotFoundException();
            }
        } catch(FileNotFoundException exception) {
            System.out.println("couldn't find mongrel start script.");
        } catch(ScriptException exception) {
            exception.getCause().printStackTrace();
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
        System.out.println(this.isServerRunning());
        System.out.println(this.getStatus());
        if(this.isServerRunning()) {
            System.out.println("interrupted");
            this.setStatus("stopped");
            this.serverThread.shutdown();
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

    
    public class MongrelThread implements Runnable {
        private Boolean finished = false;
        private Thread t;
        private MongrelManager mongrelManager;
        
        public MongrelThread(MongrelManager mongrelManager) {
            this.mongrelManager = mongrelManager;
            t = new Thread(this);
        }
        
        public void start() {
            t.start();
        }
        
        @Override
        public void run() {
            System.out.println("Starting mongrel...");
            this.mongrelManager.setStatus("starting");

            // Try to port forward incoming traffic to our server via UPnP
            mongrelManager.mapPort();

            mongrelManager.loadMongrel();
            
            while(this.finished != true) {
                try {
                Thread.currentThread().sleep(1000);
                System.out.println("looping..." + mongrelManager.getStatus());
                } catch(InterruptedException exception) {
                }
            }
        }
        
        public void shutdown() {
            this.finished = true;
        }
    }
}
