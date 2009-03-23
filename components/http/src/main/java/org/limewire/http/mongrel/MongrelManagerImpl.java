package org.limewire.http.mongrel;

import java.io.File;
import java.io.FileNotFoundException;

import javax.script.ScriptException;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.NetworkUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.UPnPManager;
import org.limewire.scripting.RubyEvaluator;

@Singleton
public 
class MongrelManagerImpl implements MongrelManager {

    private RubyEvaluator rubyEvaluator;
    private final Provider<UPnPManager> upnpManager;
    private NetworkManager networkManager;
            
    private int _port = 4422;
    private String status = "stopped";
    
    @Inject
    public MongrelManagerImpl(final RubyEvaluator rubyEvaluator, Provider<UPnPManager> upnpManager, 
            NetworkManager networkManager) {
        this.rubyEvaluator = rubyEvaluator;
        this.upnpManager = upnpManager;
        this.networkManager = networkManager;
        
        
    }
    
    @Override
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Mongrel Manager");
    }
    
    @Override
    public void start() {
        if(!this.isServerRunning()) {
            System.out.println("Starting mongrel...");
            setStatus("starting");

            // Try to port forward incoming traffic to our server via UPnP
            mapPort();

            loadMongrel();
            setStatus("started");
        }
    }
    
    public void loadMongrel() {
        System.out.println("Release the hounds!");
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
