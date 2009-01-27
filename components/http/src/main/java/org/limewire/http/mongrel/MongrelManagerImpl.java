package org.limewire.http.mongrel;

import java.io.File;
import java.io.FileNotFoundException;

import javax.script.ScriptException;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.NetworkUtils;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.UPnPManager;
import com.limegroup.scripting.RubyEvaluator;

public class MongrelManagerImpl implements MongrelManager {

    private RubyEvaluator rubyEvaluator;
    private final Provider<UPnPManager> upnpManager;
    private NetworkManager networkManager;
        
    private int _port = 4422;
    
    @Inject
    public MongrelManagerImpl(RubyEvaluator rubyEvaluator, Provider<UPnPManager> upnpManager, 
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
        System.out.println("Starting mongrel...");
        
        // Try to port forward incoming traffic to our server via UPnP
        mapPort();
        
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
                this.rubyEvaluator.eval(usablePath);
            } else {
                throw new FileNotFoundException();
            }
        } catch(FileNotFoundException exception) {
            System.out.println("couldn't find mongrel start script.");
        } catch(ScriptException exception) {
            exception.getCause().printStackTrace();
        }
    }
    
    private void mapPort() {
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
    @Override
    public void stop() {
    }
    
    @Override
    public boolean isAsyncStop() {
        return true;
    }

}
