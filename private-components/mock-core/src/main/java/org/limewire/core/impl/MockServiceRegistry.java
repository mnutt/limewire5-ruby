package org.limewire.core.impl;

import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.lifecycle.ServiceRegistryListener;
import org.limewire.lifecycle.ServiceStage;
import org.limewire.lifecycle.StagedRegisterBuilder;


public class MockServiceRegistry implements ServiceRegistry {

    @Override
    public void addListener(ServiceRegistryListener serviceRegistryListener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void initialize() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public StagedRegisterBuilder register(Service service) {
        return new StagedRegisterBuilder() {
            @Override
            public void in(Object customStage) {
                // TODO Auto-generated method stub
                
            }
            @Override
            public void in(ServiceStage stage) {
                // TODO Auto-generated method stub
                
            }
        };
    }

    @Override
    public void start() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void start(Object stage) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub
        
    }
    
}
