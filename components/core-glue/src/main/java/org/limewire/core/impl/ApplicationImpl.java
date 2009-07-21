package org.limewire.core.impl;

import org.limewire.core.api.Application;
import org.limewire.http.httpclient.HttpClientInstanceUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.util.LimeWireUtils;

@Singleton
class ApplicationImpl implements Application, HttpClientInstanceUtils {
    
    private final ApplicationServices applicationServices;
    private final LifecycleManager lifecycleManager;
    private volatile String flag = null;
    
    @Inject
    public ApplicationImpl(ApplicationServices applicationServices, LifecycleManager lifecycleManager) {
        this.applicationServices = applicationServices;
        this.lifecycleManager = lifecycleManager;
    }
    
    @Override
    public String addClientInfoToUrl(String baseUrl) {
        return LimeWireUtils.addLWInfoToUrl(baseUrl, applicationServices.getMyGUID());
    }
    
    @Override
    public void startCore() {
        lifecycleManager.start();
    }
    
    @Override
    public void stopCore() {
        if(flag == null)
            lifecycleManager.shutdown();
        else
            lifecycleManager.shutdown(flag);
    }
    
    @Override
    public void setShutdownFlag(String flag) {
        this.flag = flag;
    }
    
    @Override
    public boolean isTestingVersion() {
        return LimeWireUtils.isTestingVersion();
    }
    
    @Override
    public String getVersion() {
        return LimeWireUtils.getLimeWireVersion();
    }

    @Override
    public boolean isProVersion() {
       return LimeWireUtils.isPro();
    }
    

}
