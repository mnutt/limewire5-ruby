package org.limewire.http.webservice;

import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceStage;

import com.google.inject.Singleton;
import com.google.inject.Inject;

@Singleton
public
class WebServiceGlue {
    private final WebServiceManager WebServiceManager;

    @Inject
    public WebServiceGlue(WebServiceManager WebServiceManager) {
        this.WebServiceManager = WebServiceManager;
    }
    
    public WebServiceManager getWebServiceManager() {
        return this.WebServiceManager;
    }
    
    @Inject
    @SuppressWarnings({"unused", "UnusedDeclaration"})
    private void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(new Service() {
            public String getServiceName() {
                return "WebService";
            }

            public void initialize() {
            };

            public void start() {
                WebServiceManager.start();
            };
        
            public void stop() {
                WebServiceManager.stop();
            };
            
            public void restart() {
                WebServiceManager.restart();
            }
        
            public boolean isAsyncStop() {
                return true;
            }
        }).in(ServiceStage.LATE);
    }
}
