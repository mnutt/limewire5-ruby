package org.limewire.http.mongrel;

import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceStage;

import com.google.inject.Singleton;
import com.google.inject.Inject;

@Singleton
public
class MongrelGlue {
    private final MongrelManager mongrelManager;

    @Inject
    public MongrelGlue(MongrelManager mongrelManager) {
        this.mongrelManager = mongrelManager;
    }
    
    public MongrelManager getMongrelManager() {
        return this.mongrelManager;
    }
    
    @Inject
    @SuppressWarnings({"unused", "UnusedDeclaration"})
    private void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(new Service() {
            public String getServiceName() {
                return "Mongrel";
            }

            public void initialize() {
            };

            public void start() {
                mongrelManager.start();
            };
        
            public void stop() {
                mongrelManager.stop();
            };
            
            public void restart() {
                mongrelManager.restart();
            }
        
            public boolean isAsyncStop() {
                return true;
            }
        }).in(ServiceStage.LATE);
    }
}
