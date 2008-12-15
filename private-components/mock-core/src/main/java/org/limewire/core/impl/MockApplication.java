package org.limewire.core.impl;

import org.limewire.core.api.Application;


public class MockApplication implements Application {
    
    @Override
    public String getUniqueUrl(String baseUrl) {
        return baseUrl;
    }
    
    @Override
    public void startCore() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void stopCore() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void setShutdownFlag(String flag) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public boolean isTestingVersion() {
        // TODO Auto-generated method stub
        return true;
    }    
    
    @Override
    public String getVersion() {
        return "0.0.1";
    }

    @Override
    public boolean isProVersion() {
        return false;
    }
}
