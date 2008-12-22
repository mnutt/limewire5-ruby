package org.limewire.http.mongrel;

import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceStage;
import org.limewire.ui.swing.util.BackgroundExecutorService;

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
    
    @Inject
    @SuppressWarnings({"unused", "UnusedDeclaration"})
    private void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(new Service() {
            public String getServiceName() {
                return "Mongrel Manager";
            }

            public void initialize() {
            };

            public void start() {
                BackgroundExecutorService.execute(new Runnable(){
                    @Override
                    public void run() {
                        mongrelManager.start();
                    }
                });
            };
        
            public void stop() {
            };
        
            public boolean isAsyncStop() {
                return true;
            }
        }).in(ServiceStage.LATE);
    }
}
